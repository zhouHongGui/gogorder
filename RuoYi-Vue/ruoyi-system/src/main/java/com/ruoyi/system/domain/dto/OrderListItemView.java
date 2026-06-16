package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单列表项展示对象（订单列表每行）。比详情精简，门店删除时名称兜底「历史门店」。
 */
public class OrderListItemView
{
    private Long orderId;                  // 订单 ID
    private String orderNo;                // 订单号
    private Long shopId;                   // 门店 ID
    private String shopName;               // 门店名（删除时兜底「历史门店」）
    private String orderType;              // 订单类型 NORMAL/PREORDER
    private String orderTypeDesc;          // 订单类型中文
    private LocalDateTime scheduledPickupTime;  // 预约取餐时间
    private Integer orderStatus;           // 订单状态码
    private String orderStatusDesc;        // 订单状态中文
    private Integer payStatus;             // 支付状态码
    private String payStatusDesc;          // 支付状态中文
    private Integer refundStatus;          // 退款状态码
    private String refundStatusDesc;       // 退款状态中文
    private Integer totalAmount;           // 应付合计（分）
    private Integer itemCount;             // 总杯数
    private String pickupDisplay;          // 取餐展示号
    private LocalDateTime createTime;      // 下单时间
    private List<OrderItemView> items;     // 商品明细列表

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public String getOrderTypeDesc() { return orderTypeDesc; }
    public void setOrderTypeDesc(String orderTypeDesc) { this.orderTypeDesc = orderTypeDesc; }
    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderStatusDesc() { return orderStatusDesc; }
    public void setOrderStatusDesc(String orderStatusDesc) { this.orderStatusDesc = orderStatusDesc; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public String getPayStatusDesc() { return payStatusDesc; }
    public void setPayStatusDesc(String payStatusDesc) { this.payStatusDesc = payStatusDesc; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public String getRefundStatusDesc() { return refundStatusDesc; }
    public void setRefundStatusDesc(String refundStatusDesc) { this.refundStatusDesc = refundStatusDesc; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getPickupDisplay() { return pickupDisplay; }
    public void setPickupDisplay(String pickupDisplay) { this.pickupDisplay = pickupDisplay; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<OrderItemView> getItems() { return items; }
    public void setItems(List<OrderItemView> items) { this.items = items; }
}
