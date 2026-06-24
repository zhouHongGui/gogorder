package com.ruoyi.system.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.BizOrderItem;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.dto.BOrderBoardView;
import com.ruoyi.system.domain.dto.BOrderCardView;
import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderItemView;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.IBOrderService;

/** 门店员工端订单看板与制作核销服务。 */
@Service
public class BOrderServiceImpl implements IBOrderService
{
    private static final int BOARD_LIMIT = 300;
    private static final long MAKE_TIMEOUT_MINUTES = 15;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;
    @Autowired private ShopMapper shopMapper;

    @Override
    public BOrderBoardView getBoard(Long shopId)
    {
        Shop shop = requireShop(shopId);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime makeWindowTime = now.plusMinutes(StringUtils.nvl(shop.getMakeLeadMinutes(), 30));
        List<BizOrder> orders = bizOrderMapper.selectBBoardOrders(shopId, BOARD_LIMIT);
        Map<Long, List<BizOrderItem>> itemsByOrder = loadItemsByOrder(orders);

        List<BOrderCardView> preorders = new ArrayList<>();
        List<BOrderCardView> pending = new ArrayList<>();
        List<BOrderCardView> making = new ArrayList<>();
        List<BOrderCardView> waiting = new ArrayList<>();
        for (BizOrder order : orders)
        {
            BOrderCardView card = buildCardView(order, itemsByOrder.getOrDefault(order.getId(), Collections.emptyList()), now);
            if (Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(order.getOrderStatus()))
            {
                if (isFuturePreorder(order, makeWindowTime))
                {
                    preorders.add(card);
                }
                else
                {
                    pending.add(card);
                }
                continue;
            }
            if (Integer.valueOf(OrderStatusEnum.MAKING.getCode()).equals(order.getOrderStatus()))
            {
                making.add(card);
                continue;
            }
            if (Integer.valueOf(OrderStatusEnum.READY.getCode()).equals(order.getOrderStatus()))
            {
                waiting.add(card);
            }
        }

        BOrderBoardView board = new BOrderBoardView();
        board.setPreorders(preorders);
        board.setPending(pending);
        board.setMaking(making);
        board.setWaiting(waiting);
        board.setPreordersCount(preorders.size());
        board.setPendingCount(pending.size());
        board.setMakingCount(making.size());
        board.setWaitingCount(waiting.size());
        board.setTotalActiveCount(preorders.size() + pending.size() + making.size() + waiting.size());
        return board;
    }

    @Override
    public OrderDetailView getDetail(Long shopId, Long orderId)
    {
        BizOrder order = requireOrderInShop(shopId, orderId);
        Shop shop = requireShop(shopId);
        OrderDetailView view = buildDetailView(order, shop, bizOrderItemMapper.selectByOrderId(orderId));
        // B 端不下发 pickupToken（敏感核销凭证，仅顾客端可见，防冒领）
        view.setPickupToken(null);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMake(Long shopId, Long orderId)
    {
        startMakeInternal(shopId, orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int batchStartMake(Long shopId, List<Long> orderIds)
    {
        int count = 0;
        for (Long orderId : orderIds)
        {
            startMakeInternal(shopId, orderId);
            count++;
        }
        return count;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeMake(Long shopId, Long orderId)
    {
        int rows = bizOrderMapper.updateCompleteMake(orderId, shopId, LocalDateTime.now());
        if (rows == 0)
        {
            ensureTransitionTarget(shopId, orderId, OrderStatusEnum.MAKING, "订单状态不允许制作完成");
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailView verify(Long shopId, String pickupToken)
    {
        String normalizedToken = pickupToken == null ? "" : pickupToken.trim();
        if (normalizedToken.isEmpty())
        {
            throw new ServiceException("取餐码不能为空", HttpStatus.BAD_REQUEST);
        }
        BizOrder order = bizOrderMapper.selectByPickupTokenForUpdate(normalizedToken);
        // 防探测：token 不存在 / 错门店 / 状态不符 统一模糊提示，避免枚举有效码或判断归属店
        if (order == null || !Objects.equals(order.getShopId(), shopId))
        {
            throw new ServiceException("核销码无效或当前不可用", HttpStatus.BAD_REQUEST);
        }
        int rows = bizOrderMapper.updateVerify(order.getId(), shopId, LocalDateTime.now());
        if (rows == 0)
        {
            throw new ServiceException("核销码无效或当前不可用", HttpStatus.BAD_REQUEST);
        }
        return getDetail(shopId, order.getId());
    }

    private void startMakeInternal(Long shopId, Long orderId)
    {
        BizOrder order = requireOrderInShop(shopId, orderId);
        Shop shop = requireShop(shopId);
        LocalDateTime makeWindowTime = LocalDateTime.now().plusMinutes(StringUtils.nvl(shop.getMakeLeadMinutes(), 30));
        if (isFuturePreorder(order, makeWindowTime))
        {
            throw new ServiceException("预订单尚未到制作时间", HttpStatus.CONFLICT);
        }
        int rows = bizOrderMapper.updateStartMake(orderId, shopId, LocalDateTime.now());
        if (rows == 0)
        {
            ensureTransitionTarget(shopId, orderId, OrderStatusEnum.ACCEPTED, "订单状态不允许开始制作");
        }
    }

    private void ensureTransitionTarget(Long shopId, Long orderId, OrderStatusEnum expectedStatus, String message)
    {
        BizOrder order = requireOrderInShop(shopId, orderId);
        if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())
                || !Integer.valueOf(expectedStatus.getCode()).equals(order.getOrderStatus()))
        {
            throw new ServiceException(message, HttpStatus.CONFLICT);
        }
        throw new ServiceException("订单状态已变更，请刷新后重试", HttpStatus.CONFLICT);
    }

    private BizOrder requireOrderInShop(Long shopId, Long orderId)
    {
        if (shopId == null || orderId == null)
        {
            throw new ServiceException("订单参数不能为空", HttpStatus.BAD_REQUEST);
        }
        BizOrder order = bizOrderMapper.selectById(orderId);
        if (order == null)
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        if (!Objects.equals(order.getShopId(), shopId))
        {
            throw new ServiceException("订单不属于当前门店", HttpStatus.FORBIDDEN);
        }
        return order;
    }

    private Shop requireShop(Long shopId)
    {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除", HttpStatus.NOT_FOUND);
        }
        return shop;
    }

    private boolean isFuturePreorder(BizOrder order, LocalDateTime makeWindowTime)
    {
        return OrderTypeEnum.PREORDER.getCode().equals(order.getOrderType())
                && order.getScheduledPickupTime() != null
                && order.getScheduledPickupTime().isAfter(makeWindowTime);
    }

    private Map<Long, List<BizOrderItem>> loadItemsByOrder(List<BizOrder> orders)
    {
        if (orders == null || orders.isEmpty())
        {
            return Collections.emptyMap();
        }
        List<Long> orderIds = orders.stream().map(BizOrder::getId).toList();
        return bizOrderItemMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(BizOrderItem::getOrderId));
    }

    private BOrderCardView buildCardView(BizOrder order, List<BizOrderItem> items, LocalDateTime now)
    {
        List<OrderItemView> itemViews = items.stream().map(this::buildItemView).toList();
        BOrderCardView view = new BOrderCardView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setOrderType(order.getOrderType());
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), order.getOrderType()));
        view.setScheduledPickupTime(order.getScheduledPickupTime());
        view.setOrderStatus(order.getOrderStatus());
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), order.getOrderStatus()));
        view.setPayStatus(order.getPayStatus());
        view.setRefundStatus(order.getRefundStatus());
        view.setTotalAmount(order.getTotalAmount());
        view.setItemCount(itemViews.stream().mapToInt(item -> StringUtils.nvl(item.getQuantity(), 0)).sum());
        view.setPickupDisplay(order.getPickupDisplay());
        view.setRemark(order.getRemark());
        view.setCreateTime(order.getCreateTime());
        view.setAcceptTime(order.getAcceptTime());
        view.setMakeStartTime(order.getMakeStartTime());
        view.setCompleteMakeTime(order.getCompleteMakeTime());
        view.setMakeTimeout(isMakeTimeout(order, now));
        view.setItems(itemViews);
        return view;
    }

    private boolean isMakeTimeout(BizOrder order, LocalDateTime now)
    {
        return Integer.valueOf(OrderStatusEnum.MAKING.getCode()).equals(order.getOrderStatus())
                && order.getMakeStartTime() != null
                && Duration.between(order.getMakeStartTime(), now).toMinutes() >= MAKE_TIMEOUT_MINUTES;
    }

    private OrderDetailView buildDetailView(BizOrder order, Shop shop, List<BizOrderItem> items)
    {
        OrderDetailView view = new OrderDetailView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shop == null ? "" : shop.getName());
        view.setShopPhone(shop == null ? "" : shop.getPhone());
        view.setOrderType(order.getOrderType());
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), order.getOrderType()));
        view.setScheduledPickupTime(order.getScheduledPickupTime());
        view.setOrderStatus(order.getOrderStatus());
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), order.getOrderStatus()));
        view.setPayStatus(order.getPayStatus());
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), order.getPayStatus()));
        view.setRefundStatus(order.getRefundStatus());
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), order.getRefundStatus()));
        view.setProductAmount(order.getProductAmount());
        view.setPackFee(order.getPackFee());
        view.setTotalAmount(order.getTotalAmount());
        view.setRemark(order.getRemark());
        view.setPickupDisplay(order.getPickupDisplay());
        view.setPickupToken(order.getPickupToken());
        view.setPickupDate(order.getPickupDate());
        view.setPayTime(order.getPayTime());
        view.setAcceptTime(order.getAcceptTime());
        view.setMakeStartTime(order.getMakeStartTime());
        view.setCompleteMakeTime(order.getCompleteMakeTime());
        view.setVerifyTime(order.getVerifyTime());
        view.setCancelTime(order.getCancelTime());
        view.setCancelReason(order.getCancelReason());
        view.setCreateTime(order.getCreateTime());
        view.setItems(items.stream().map(this::buildItemView).toList());
        return view;
    }

    private OrderItemView buildItemView(BizOrderItem item)
    {
        List<SpecSnapshot> specs = StringUtils.isEmpty(item.getSpecs())
                ? Collections.emptyList() : JSON.parseArray(item.getSpecs(), SpecSnapshot.class);
        OrderItemView view = new OrderItemView();
        view.setProductId(item.getProductId());
        view.setProductName(item.getProductName());
        view.setProductImage(item.getProductImage());
        view.setSpecs(specs);
        view.setSpecText(specs.isEmpty() ? "默认规格"
                : specs.stream().map(SpecSnapshot::getLabel).collect(Collectors.joining("、")));
        view.setUnitPrice(item.getUnitPrice());
        view.setQuantity(item.getQuantity());
        view.setSubtotal(item.getSubtotal());
        return view;
    }

    private String enumDesc(OrderTypeEnum[] values, String code)
    {
        for (OrderTypeEnum value : values) if (value.getCode().equals(code)) return value.getDesc();
        return "";
    }

    private String enumDesc(OrderStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (OrderStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(PayStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (PayStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(RefundStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (RefundStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }
}
