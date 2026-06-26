package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;

/** 门店员工端订单卡片视图，供制作看板列表展示。 */
public class BOrderCardView
{
    private Long orderId;
    private String orderNo;
    private String orderType;
    private String orderTypeDesc;
    private LocalDateTime scheduledPickupTime;
    private Integer orderStatus;
    private String orderStatusDesc;
    private Integer payStatus;
    private Integer refundStatus;
    private Integer totalAmount;
    private Integer itemCount;
    private String pickupDisplay;
    private String remark;
    private LocalDateTime createTime;
    private LocalDateTime acceptTime;
    private LocalDateTime makeStartTime;
    private LocalDateTime completeMakeTime;
    private LocalDateTime estimatedReadyTime;
    private Boolean makeTimeout;
    private List<OrderItemView> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
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
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
    public String getPickupDisplay() { return pickupDisplay; }
    public void setPickupDisplay(String pickupDisplay) { this.pickupDisplay = pickupDisplay; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getAcceptTime() { return acceptTime; }
    public void setAcceptTime(LocalDateTime acceptTime) { this.acceptTime = acceptTime; }
    public LocalDateTime getMakeStartTime() { return makeStartTime; }
    public void setMakeStartTime(LocalDateTime makeStartTime) { this.makeStartTime = makeStartTime; }
    public LocalDateTime getCompleteMakeTime() { return completeMakeTime; }
    public void setCompleteMakeTime(LocalDateTime completeMakeTime) { this.completeMakeTime = completeMakeTime; }
    public LocalDateTime getEstimatedReadyTime() { return estimatedReadyTime; }
    public void setEstimatedReadyTime(LocalDateTime estimatedReadyTime) { this.estimatedReadyTime = estimatedReadyTime; }
    public Boolean getMakeTimeout() { return makeTimeout; }
    public void setMakeTimeout(Boolean makeTimeout) { this.makeTimeout = makeTimeout; }
    public List<OrderItemView> getItems() { return items; }
    public void setItems(List<OrderItemView> items) { this.items = items; }
}
