package com.ruoyi.system.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class BizOrder
{
    private Long id;
    private String orderNo;
    private String submitKey;
    private String pickupToken;
    private String pickupDisplay;
    private LocalDate pickupDate;
    private Long userId;
    private Long shopId;
    private String orderType;
    private LocalDateTime scheduledPickupTime;
    private Integer orderStatus;
    private Integer payStatus;
    private Integer refundStatus;
    private Integer productAmount;
    private Integer packFee;
    private Integer totalAmount;
    private String remark;
    private LocalDateTime payTime;
    private LocalDateTime acceptTime;
    private LocalDateTime makeStartTime;
    private LocalDateTime completeMakeTime;
    private LocalDateTime verifyTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getSubmitKey() { return submitKey; }
    public void setSubmitKey(String submitKey) { this.submitKey = submitKey; }
    public String getPickupToken() { return pickupToken; }
    public void setPickupToken(String pickupToken) { this.pickupToken = pickupToken; }
    public String getPickupDisplay() { return pickupDisplay; }
    public void setPickupDisplay(String pickupDisplay) { this.pickupDisplay = pickupDisplay; }
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }
    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public Integer getPayStatus() { return payStatus; }
    public void setPayStatus(Integer payStatus) { this.payStatus = payStatus; }
    public Integer getRefundStatus() { return refundStatus; }
    public void setRefundStatus(Integer refundStatus) { this.refundStatus = refundStatus; }
    public Integer getProductAmount() { return productAmount; }
    public void setProductAmount(Integer productAmount) { this.productAmount = productAmount; }
    public Integer getPackFee() { return packFee; }
    public void setPackFee(Integer packFee) { this.packFee = packFee; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
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
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
