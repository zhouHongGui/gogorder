package com.ruoyi.system.service.impl;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
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
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.dto.BOrderBoardView;
import com.ruoyi.system.domain.dto.BOrderCardView;
import com.ruoyi.system.domain.dto.BOrderListItemView;
import com.ruoyi.system.domain.dto.BOrderPageView;
import com.ruoyi.system.domain.dto.BOrderQuery;
import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderItemView;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.CUserMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.IBOrderService;

/** 门店员工端订单看板与制作流转服务。 */
@Service
public class BOrderServiceImpl implements IBOrderService
{
    private static final Logger log = LoggerFactory.getLogger(BOrderServiceImpl.class);
    private static final int BOARD_LIMIT = 300;
    private static final long MAKE_TIMEOUT_MINUTES = 15;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;
    @Autowired private CUserMapper cUserMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private ProductionScheduleService productionScheduleService;

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
    public BOrderPageView listOrders(Long shopId, BOrderQuery query)
    {
        requireShop(shopId);
        BOrderQuery effectiveQuery = query == null ? new BOrderQuery() : query;
        effectiveQuery.setShopId(shopId);
        effectiveQuery.normalize();
        PageHelper.startPage(effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
        List<BOrderListItemView> rows = bizOrderMapper.selectBOrderList(effectiveQuery);
        PageInfo<BOrderListItemView> pageInfo = new PageInfo<>(rows);
        rows.forEach(this::fillListDesc);
        return new BOrderPageView(rows, pageInfo.getTotal(), effectiveQuery.getPageNum(), effectiveQuery.getPageSize());
    }

    @Override
    public OrderDetailView getDetail(Long shopId, Long orderId)
    {
        BizOrder order = requireOrderInShop(shopId, orderId);
        Shop shop = requireShop(shopId);
        OrderDetailView view = buildDetailView(order, shop, bizOrderItemMapper.selectByOrderId(orderId));
        CUser user = order.getUserId() == null ? null : cUserMapper.selectById(order.getUserId());
        view.setReservedPhone(maskPhone(user == null ? null : user.getPhone()));
        // B 端详情不下发 pickupToken；该令牌只允许由出餐兜底扫码请求携带，避免被列表/详情泄露。
        view.setPickupToken(null);
        return view;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void startMake(Long shopId, Long orderId, Long operatorId)
    {
        boolean changed = startMakeInternal(shopId, orderId);
        BizOrder order = requireOrderInShop(shopId, orderId);
        if (changed)
        {
            log.info("员工端开始制作成功 orderId={} orderNo={} shopId={} operatorId={}",
                    orderId, order.getOrderNo(), shopId, operatorId);
        }
        else
        {
            log.debug("员工端开始制作幂等返回 orderId={} shopId={} operatorId={} status={}",
                    orderId, shopId, operatorId, order.getOrderStatus());
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void notifyPickup(Long shopId, Long orderId, Long operatorId)
    {
        int rows = bizOrderMapper.updateScanOut(orderId, shopId, LocalDateTime.now());
        if (rows > 0)
        {
            BizOrder order = requireOrderInShop(shopId, orderId);
            log.info("员工端通知取餐成功 orderId={} orderNo={} shopId={} operatorId={} pickupDisplay={}",
                    orderId, order.getOrderNo(), shopId, operatorId, order.getPickupDisplay());
            registerAfterCommit(() -> productionScheduleService.onOrderScannedOut(shopId));
            return;
        }

        BizOrder order = requireOrderInShop(shopId, orderId);
        if (Integer.valueOf(OrderStatusEnum.READY.getCode()).equals(order.getOrderStatus())
                || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus()))
        {
            log.debug("员工端通知取餐幂等返回 orderId={} shopId={} operatorId={} status={}",
                    orderId, shopId, operatorId, order.getOrderStatus());
            return;
        }
        if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())
                || !Integer.valueOf(OrderStatusEnum.MAKING.getCode()).equals(order.getOrderStatus()))
        {
            throw new ServiceException("订单状态不允许通知取餐", HttpStatus.CONFLICT);
        }
        throw new ServiceException("订单状态已变更，请刷新后重试", HttpStatus.CONFLICT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderDetailView scanOut(Long shopId, String code, Long operatorId)
    {
        String normalizedCode = code == null ? "" : code.trim();
        if (normalizedCode.isEmpty())
        {
            throw new ServiceException("订单号或取餐号不能为空", HttpStatus.BAD_REQUEST);
        }
        BizOrder order = bizOrderMapper.selectByScanOutCodeForUpdate(shopId, normalizedCode);
        // 防探测：订单号/取餐号/token 不存在、错门店或状态不符时统一模糊提示。
        if (order == null)
        {
            throw new ServiceException("订单号或取餐号无效或当前不可用", HttpStatus.BAD_REQUEST);
        }
        Integer status = order.getOrderStatus();
        if (Integer.valueOf(OrderStatusEnum.READY.getCode()).equals(status)
                || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(status))
        {
            log.debug("员工端出餐兜底幂等返回 orderId={} shopId={} operatorId={} status={}",
                    order.getId(), shopId, operatorId, status);
            return getDetail(shopId, order.getId());
        }
        if (!Integer.valueOf(OrderStatusEnum.MAKING.getCode()).equals(status))
        {
            throw new ServiceException("订单号或取餐号无效或当前不可用", HttpStatus.BAD_REQUEST);
        }
        int rows = bizOrderMapper.updateScanOut(order.getId(), shopId, LocalDateTime.now());
        if (rows == 0)
        {
            throw new ServiceException("订单号或取餐号无效或当前不可用", HttpStatus.BAD_REQUEST);
        }
        // 通知取餐后释放串行制作槽位，推进队列下一单进入制作。
        log.info("员工端出餐兜底成功 orderId={} orderNo={} shopId={} operatorId={} pickupDisplay={}",
                order.getId(), order.getOrderNo(), shopId, operatorId, order.getPickupDisplay());
        registerAfterCommit(() -> productionScheduleService.onOrderScannedOut(shopId));
        return getDetail(shopId, order.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void completeOrder(Long shopId, Long orderId, Long operatorId)
    {
        int rows = bizOrderMapper.updateCompleteOrder(orderId, shopId, LocalDateTime.now());
        if (rows > 0)
        {
            BizOrder order = requireOrderInShop(shopId, orderId);
            log.info("员工端完成订单成功 orderId={} orderNo={} shopId={} operatorId={}",
                    orderId, order.getOrderNo(), shopId, operatorId);
            return;
        }
        BizOrder order = requireOrderInShop(shopId, orderId);
        if (Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(order.getOrderStatus()))
        {
            log.debug("员工端完成订单幂等返回 orderId={} shopId={} operatorId={}",
                    orderId, shopId, operatorId);
            return;
        }
        if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())
                || !Integer.valueOf(OrderStatusEnum.READY.getCode()).equals(order.getOrderStatus()))
        {
            throw new ServiceException("订单状态不允许完成", HttpStatus.CONFLICT);
        }
        throw new ServiceException("订单状态已变更，请刷新后重试", HttpStatus.CONFLICT);
    }

    /** 事务提交后执行（无事务上下文则立即执行）。用于触发调度，避免读到未提交状态。
     *  调度异常隔离：不影响已成功提交的通知取餐（仅吞异常，兜底定时任务会补推进）。 */
    private void registerAfterCommit(Runnable action)
    {
        Runnable safeAction = () -> {
            try
            {
                action.run();
            }
            catch (Exception e)
            {
                log.warn("通知取餐后制作调度异常，已忽略（兜底任务会补）", e);
            }
        };
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            safeAction.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization()
        {
            @Override
            public void afterCommit()
            {
                safeAction.run();
            }
        });
    }

    private boolean startMakeInternal(Long shopId, Long orderId)
    {
        BizOrder order = requireOrderInShop(shopId, orderId);
        if (Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())
                && isAtOrAfterMaking(order.getOrderStatus()))
        {
            return false;
        }
        if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())
                || !Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(order.getOrderStatus()))
        {
            throw new ServiceException("订单状态不允许开始制作", HttpStatus.CONFLICT);
        }
        Shop shop = requireShop(shopId);
        LocalDateTime makeWindowTime = LocalDateTime.now().plusMinutes(StringUtils.nvl(shop.getMakeLeadMinutes(), 30));
        if (isFuturePreorder(order, makeWindowTime))
        {
            throw new ServiceException("预订单尚未到制作时间", HttpStatus.CONFLICT);
        }
        shopMapper.selectShopForUpdate(shopId);
        if (bizOrderMapper.countMakingOrders(shopId) > 0)
        {
            throw new ServiceException("当前已有制作中订单，请先出餐后再开始下一单", HttpStatus.CONFLICT);
        }
        int rows = bizOrderMapper.updateStartMake(orderId, shopId, LocalDateTime.now());
        if (rows == 0)
        {
            BizOrder current = requireOrderInShop(shopId, orderId);
            if (Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(current.getPayStatus())
                    && isAtOrAfterMaking(current.getOrderStatus()))
            {
                return false;
            }
            if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(current.getPayStatus())
                    || !Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(current.getOrderStatus()))
            {
                throw new ServiceException("订单状态不允许开始制作", HttpStatus.CONFLICT);
            }
            throw new ServiceException("订单状态已变更，请刷新后重试", HttpStatus.CONFLICT);
        }
        return true;
    }

    private boolean isAtOrAfterMaking(Integer status)
    {
        return Integer.valueOf(OrderStatusEnum.MAKING.getCode()).equals(status)
                || Integer.valueOf(OrderStatusEnum.READY.getCode()).equals(status)
                || Integer.valueOf(OrderStatusEnum.COMPLETED.getCode()).equals(status);
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
        view.setOrderStatusDesc(staffOrderStatusDesc(order.getOrderStatus()));
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
        view.setEstimatedReadyTime(order.getEstimatedReadyTime());
        view.setMakeTimeout(isMakeTimeout(order, now));
        view.setItems(itemViews);
        return view;
    }

    private void fillListDesc(BOrderListItemView view)
    {
        view.setUserPhoneMasked(maskPhone(view.getUserPhone()));
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), view.getOrderType()));
        view.setOrderStatusDesc(staffOrderStatusDesc(view.getOrderStatus()));
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), view.getPayStatus()));
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), view.getRefundStatus()));
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
        view.setOrderStatusDesc(staffOrderStatusDesc(order.getOrderStatus()));
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
        view.setEstimatedReadyTime(order.getEstimatedReadyTime());
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

    private String maskPhone(String phone)
    {
        if (StringUtils.isEmpty(phone) || phone.length() < 7)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
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

    private String staffOrderStatusDesc(Integer code)
    {
        if (Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(code))
        {
            return "待制作";
        }
        return enumDesc(OrderStatusEnum.values(), code);
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
