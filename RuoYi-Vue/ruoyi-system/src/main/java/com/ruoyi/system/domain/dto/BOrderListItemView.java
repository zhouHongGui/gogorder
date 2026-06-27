package com.ruoyi.system.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 门店员工端订单列表行。 */
public class BOrderListItemView
{
    private Long orderId;
    private String orderNo;
    @JsonIgnore
    private String userPhone;
    private String userPhoneMasked;
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
    private LocalDate pickupDate;
    private String remark;
    private LocalDateTime estimatedReadyTime;
    private LocalDateTime payTime;
    private LocalDateTime acceptTime;
    private LocalDateTime makeStartTime;
    private LocalDateTime completeMakeTime;
    private LocalDateTime verifyTime;
    private LocalDateTime cancelTime;
    private LocalDateTime createTime;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserPhoneMasked() { return userPhoneMasked; }
    public void setUserPhoneMasked(String userPhoneMasked) { this.userPhoneMasked = userPhoneMasked; }
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
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getEstimatedReadyTime() { return estimatedReadyTime; }
    public void setEstimatedReadyTime(LocalDateTime estimatedReadyTime) { this.estimatedReadyTime = estimatedReadyTime; }
    public LocalDateTime getPayTime() { return payTime; }
    public void setPayTime(LocalDateTime payTime) { this.payTime = payTime; }
    public LocalDateTime getAcceptTime() { return acceptTime; }
    public void setAcceptTime(LocalDateTime acceptTime) { this.acceptTime = acceptTime; }
    public LocalDateTime getMakeStartTime() { return makeStartTime; }
    public void setMakeStartTime(LocalDateTime makeStartTime) { this.makeStartTime = makeStartTime; }
    public LocalDateTime getCompleteMakeTime() { return completeMakeTime; }
    public void setCompleteMakeTime(LocalDateTime completeMakeTime) { this.completeMakeTime = completeMakeTime; }
    public LocalDateTime getVerifyTime() { return verifyTime; }
    public void setVerifyTime(LocalDateTime verifyTime) { this.verifyTime = verifyTime; }
    public LocalDateTime getCancelTime() { return cancelTime; }
    public void setCancelTime(LocalDateTime cancelTime) { this.cancelTime = cancelTime; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
