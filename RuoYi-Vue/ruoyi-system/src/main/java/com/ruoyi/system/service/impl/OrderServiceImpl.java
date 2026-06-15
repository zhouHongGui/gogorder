package com.ruoyi.system.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.config.GogorderOrderProperties;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.BizOrderItem;
import com.ruoyi.system.domain.CUserBalance;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderItemRequest;
import com.ruoyi.system.domain.dto.OrderItemView;
import com.ruoyi.system.domain.dto.OrderListItemView;
import com.ruoyi.system.domain.dto.OrderPageView;
import com.ruoyi.system.domain.dto.OrderSubmitRequest;
import com.ruoyi.system.domain.dto.OrderSubmitResponse;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.domain.dto.SpecValidationResult;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.CUserBalanceMapper;
import com.ruoyi.system.mapper.CUserMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.IOrderCancelService;
import com.ruoyi.system.service.IOrderService;
import com.ruoyi.system.service.IProductCenterService;
import com.ruoyi.system.service.ISpecValidationService;

@Service
public class OrderServiceImpl implements IOrderService
{
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private CUserBalanceMapper cUserBalanceMapper;
    @Autowired private CUserMapper cUserMapper;
    @Autowired private ISpecValidationService specValidationService;
    @Autowired private IProductCenterService productCenterService;
    @Autowired private IOrderCancelService orderCancelService;
    @Autowired private GogorderOrderProperties orderProperties;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitResponse submitOrder(Long userId, OrderSubmitRequest request)
    {
        BizOrder existing = bizOrderMapper.selectBySubmitKey(userId, request.getSubmitToken());
        if (existing != null)
        {
            return buildSubmitResponse(existing);
        }
        CUser user = cUserMapper.selectByIdForUpdate(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()))
        {
            throw new ServiceException("账号不可用");
        }
        existing = bizOrderMapper.selectBySubmitKey(userId, request.getSubmitToken());
        if (existing != null)
        {
            return buildSubmitResponse(existing);
        }
        if (bizOrderMapper.countPendingOrdersByUserId(userId) >= orderProperties.getMaxPendingOrdersPerUser())
        {
            throw new ServiceException("待支付订单过多，请先支付或取消已有订单");
        }
        return doSubmitOrder(userId, request);
    }

    private OrderSubmitResponse doSubmitOrder(Long userId, OrderSubmitRequest request)
    {
        Shop shop = shopMapper.selectShopById(request.getShopId());
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        validateOrderType(shop, request);

        List<BizOrderItem> orderItems = new ArrayList<>();
        Map<Long, Integer> stockDeductions = new LinkedHashMap<>();
        int productAmount = 0;
        int totalQuantity = 0;
        try
        {
            for (OrderItemRequest itemRequest : request.getItems())
            {
                SpecValidationResult validation = specValidationService.validateAndPrice(
                        request.getShopId(), itemRequest.getProductId(), itemRequest.getSpecs());
                if (validation.getStock() != -1 && validation.getStock() < itemRequest.getQuantity())
                {
                    throw new ServiceException("商品「" + validation.getProductName() + "」库存不足");
                }
                int subtotal = Math.multiplyExact(validation.getUnitPrice(), itemRequest.getQuantity());
                productAmount = Math.addExact(productAmount, subtotal);
                totalQuantity = Math.addExact(totalQuantity, itemRequest.getQuantity());
                stockDeductions.merge(validation.getShopProductId(), itemRequest.getQuantity(), Math::addExact);

                BizOrderItem item = new BizOrderItem();
                item.setShopProductId(validation.getShopProductId());
                item.setProductId(itemRequest.getProductId());
                item.setProductName(validation.getProductName());
                item.setProductImage(validation.getProductImage());
                item.setSpecs(JSON.toJSONString(validation.getSelectedOptions()));
                item.setUnitPrice(validation.getUnitPrice());
                item.setQuantity(itemRequest.getQuantity());
                item.setSubtotal(subtotal);
                orderItems.add(item);
            }
        }
        catch (ArithmeticException e)
        {
            throw new ServiceException("订单金额或数量超出系统限制");
        }

        int packFee;
        int totalAmount;
        try
        {
            packFee = Math.multiplyExact(StringUtils.nvl(shop.getPackFee(), 0), totalQuantity);
            totalAmount = Math.addExact(productAmount, packFee);
        }
        catch (ArithmeticException e)
        {
            throw new ServiceException("订单金额超出系统限制");
        }

        BizOrder order = new BizOrder();
        order.setSubmitKey(request.getSubmitToken());
        order.setUserId(userId);
        order.setShopId(request.getShopId());
        order.setOrderType(request.getOrderType());
        order.setScheduledPickupTime(request.getScheduledPickupTime());
        order.setProductAmount(productAmount);
        order.setPackFee(packFee);
        order.setTotalAmount(totalAmount);
        order.setRemark(StringUtils.trim(request.getRemark()));
        order.setCreateTime(LocalDateTime.now());
        BizOrder concurrentExisting = insertOrderWithRetry(order);
        if (concurrentExisting != null)
        {
            return buildSubmitResponse(concurrentExisting);
        }

        for (BizOrderItem item : orderItems)
        {
            item.setOrderId(order.getId());
        }
        bizOrderItemMapper.batchInsert(orderItems);

        List<Long> sortedIds = new ArrayList<>(stockDeductions.keySet());
        Collections.sort(sortedIds);
        for (Long shopProductId : sortedIds)
        {
            productCenterService.deductStock(order.getId(), shopProductId, stockDeductions.get(shopProductId));
        }
        return buildSubmitResponse(order);
    }

    private BizOrder insertOrderWithRetry(BizOrder order)
    {
        for (int retry = 0; retry < 3; retry++)
        {
            order.setOrderNo(generateOrderNo());
            try
            {
                bizOrderMapper.insertOrder(order);
                return null;
            }
            catch (DuplicateKeyException e)
            {
                if (containsConstraint(e, "uk_user_submit_key"))
                {
                    BizOrder existing = bizOrderMapper.selectBySubmitKeyForUpdate(order.getUserId(), order.getSubmitKey());
                    if (existing != null)
                    {
                        return existing;
                    }
                }
                if (!containsConstraint(e, "uk_order_no") || retry == 2)
                {
                    throw e;
                }
            }
        }
        throw new ServiceException("订单创建失败，请重试");
    }

    private void validateOrderType(Shop shop, OrderSubmitRequest request)
    {
        LocalDateTime now = LocalDateTime.now();
        if (OrderTypeEnum.NORMAL.getCode().equals(request.getOrderType()))
        {
            if (!Integer.valueOf(1).equals(shop.getStatus()) || !shop.isOpenAt(now.toLocalTime()))
            {
                throw new ServiceException("当前门店暂不支持即时单，请选择预约单");
            }
            request.setScheduledPickupTime(null);
            return;
        }
        if (!OrderTypeEnum.PREORDER.getCode().equals(request.getOrderType()))
        {
            throw new ServiceException("无效的订单类型");
        }
        LocalDateTime pickupTime = request.getScheduledPickupTime();
        if (pickupTime == null)
        {
            throw new ServiceException("预订单必须选择取餐时间");
        }
        LocalDateTime earliest = now.plusMinutes(StringUtils.nvl(shop.getPreorderMinMinutes(), 30));
        LocalDateTime latest = now.plusDays(StringUtils.nvl(shop.getPreorderMaxDays(), 7));
        if (pickupTime.isBefore(earliest) || pickupTime.isAfter(latest) || !shop.isOpenAt(pickupTime.toLocalTime()))
        {
            throw new ServiceException("预约取餐时间不可用，请重新选择");
        }
    }

    @Override
    public void cancelOrder(Long userId, Long orderId)
    {
        orderCancelService.cancelUnpaidOrder(userId, orderId);
    }

    @Override
    public OrderPageView getOrderList(Long userId, String dateScope, Integer pageNum, Integer pageSize)
    {
        String scope = StringUtils.isEmpty(dateScope) ? "TODAY" : dateScope.trim().toUpperCase();
        if (!"TODAY".equals(scope) && !"HISTORY".equals(scope))
        {
            throw new ServiceException("无效的订单日期范围");
        }
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startTime = "TODAY".equals(scope) ? todayStart : null;
        LocalDateTime endTime = "TODAY".equals(scope) ? todayStart.plusDays(1) : todayStart;
        int currentPage = Math.max(StringUtils.nvl(pageNum, 1), 1);
        int currentPageSize = Math.min(Math.max(StringUtils.nvl(pageSize, 10), 1), 20);
        int offset = (currentPage - 1) * currentPageSize;
        long total = bizOrderMapper.countUserOrders(userId, startTime, endTime);
        if (total == 0 || offset >= total)
        {
            return new OrderPageView(Collections.emptyList(), total, currentPage, currentPageSize);
        }
        List<BizOrder> orders = bizOrderMapper.selectUserOrderList(
                userId, startTime, endTime, offset, currentPageSize);
        Set<Long> shopIds = orders.stream().map(BizOrder::getShopId).collect(Collectors.toSet());
        Map<Long, Shop> shops = shopMapper.selectShopByIds(new ArrayList<>(shopIds)).stream()
                .collect(Collectors.toMap(Shop::getId, shop -> shop));
        List<Long> orderIds = orders.stream().map(BizOrder::getId).toList();
        Map<Long, List<BizOrderItem>> itemsByOrder = bizOrderItemMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(BizOrderItem::getOrderId));
        List<OrderListItemView> rows = orders.stream()
                .map(order -> buildListItemView(order, shops.get(order.getShopId()),
                        itemsByOrder.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();
        return new OrderPageView(rows, total, currentPage, currentPageSize);
    }

    @Override
    public OrderDetailView getOrderDetail(Long userId, Long orderId)
    {
        BizOrder order = bizOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId))
        {
            throw new ServiceException("订单不存在");
        }
        Shop shop = shopMapper.selectShopById(order.getShopId());
        CUser user = cUserMapper.selectById(userId);
        OrderDetailView view = new OrderDetailView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shop == null ? "" : shop.getName());
        view.setShopPhone(shop == null ? "" : shop.getPhone());
        view.setReservedPhone(user == null ? "" : user.getPhone());
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
        view.setItems(bizOrderItemMapper.selectByOrderId(orderId).stream().map(this::buildItemView).toList());
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

    private OrderListItemView buildListItemView(BizOrder order, Shop shop, List<BizOrderItem> orderItems)
    {
        List<OrderItemView> items = orderItems.stream().map(this::buildItemView).toList();
        OrderListItemView view = new OrderListItemView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shop == null ? "历史门店" : shop.getName());
        view.setOrderType(order.getOrderType());
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), order.getOrderType()));
        view.setScheduledPickupTime(order.getScheduledPickupTime());
        view.setOrderStatus(order.getOrderStatus());
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), order.getOrderStatus()));
        view.setPayStatus(order.getPayStatus());
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), order.getPayStatus()));
        view.setRefundStatus(order.getRefundStatus());
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), order.getRefundStatus()));
        view.setTotalAmount(order.getTotalAmount());
        view.setItemCount(items.stream().mapToInt(item -> StringUtils.nvl(item.getQuantity(), 0)).sum());
        view.setPickupDisplay(order.getPickupDisplay());
        view.setCreateTime(order.getCreateTime());
        view.setItems(items);
        return view;
    }

    private OrderSubmitResponse buildSubmitResponse(BizOrder order)
    {
        CUserBalance balance = cUserBalanceMapper.selectByUserId(order.getUserId());
        return new OrderSubmitResponse(order.getId(), order.getOrderNo(), order.getTotalAmount(),
                balance == null ? 0 : balance.getBalance(), order.getCreateTime());
    }

    private String generateOrderNo()
    {
        return LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    private boolean containsConstraint(DuplicateKeyException e, String constraint)
    {
        return String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase()
                .contains(constraint.toLowerCase());
    }

    private String enumDesc(OrderTypeEnum[] values, String code)
    {
        for (OrderTypeEnum value : values) if (value.getCode().equals(code)) return value.getDesc();
        return "";
    }

    private String enumDesc(OrderStatusEnum[] values, Integer code)
    {
        for (OrderStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(PayStatusEnum[] values, Integer code)
    {
        for (PayStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(RefundStatusEnum[] values, Integer code)
    {
        for (RefundStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }
}
