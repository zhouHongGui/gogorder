package com.ruoyi.system.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * Order detail returned to the management backend. pickup_token is intentionally absent.
 */
public class AdminOrderDetailView
{
    private Long orderId;
    private String orderNo;
    private Long userId;
    @JsonIgnore
    private String userPhone;
    private String userPhoneMasked;
    private Long shopId;
    private String shopName;
    private String shopPhone;
    private String orderType;
    private String orderTypeDesc;
    private LocalDateTime scheduledPickupTime;
    private Integer orderStatus;
    private String orderStatusDesc;
    private Integer payStatus;
    private String payStatusDesc;
    private Integer refundStatus;
    private String refundStatusDesc;
    private Integer productAmount;
    private Integer packFee;
    private Integer totalAmount;
    private String remark;
    private String pickupDisplay;
    private LocalDate pickupDate;
    private Integer itemCount;
    private LocalDateTime estimatedReadyTime;
    private LocalDateTime payTime;
    private LocalDateTime acceptTime;
    private LocalDateTime makeStartTime;
    private LocalDateTime completeMakeTime;
    private LocalDateTime verifyTime;
    private LocalDateTime cancelTime;
    private String cancelReason;
    private LocalDateTime createTime;
    private List<OrderItemView> items;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserPhone() { return userPhone; }
    public void setUserPhone(String userPhone) { this.userPhone = userPhone; }
    public String getUserPhoneMasked() { return userPhoneMasked; }
    public void setUserPhoneMasked(String userPhoneMasked) { this.userPhoneMasked = userPhoneMasked; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }
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
    public Integer getProductAmount() { return productAmount; }
    public void setProductAmount(Integer productAmount) { this.productAmount = productAmount; }
    public Integer getPackFee() { return packFee; }
    public void setPackFee(Integer packFee) { this.packFee = packFee; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public String getPickupDisplay() { return pickupDisplay; }
    public void setPickupDisplay(String pickupDisplay) { this.pickupDisplay = pickupDisplay; }
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public Integer getItemCount() { return itemCount; }
    public void setItemCount(Integer itemCount) { this.itemCount = itemCount; }
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
    public String getCancelReason() { return cancelReason; }
    public void setCancelReason(String cancelReason) { this.cancelReason = cancelReason; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public List<OrderItemView> getItems() { return items; }
    public void setItems(List<OrderItemView> items) { this.items = items; }
}
