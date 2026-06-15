package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

public class OrderListItemView
{
    private Long orderId;
    private String orderNo;
    private Long shopId;
    private String shopName;
    private String orderType;
    private String orderTypeDesc;
    private LocalDateTime scheduledPickupTime;
    private Integer orderStatus;
    private String orderStatusDesc;
    private Integer payStatus;
    private String payStatusDesc;
    private Integer refundStatus;
    private String refundStatusDesc;
    private Integer totalAmount;
    private Integer itemCount;
    private String pickupDisplay;
    private LocalDateTime createTime;
    private List<OrderItemView> items;

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
