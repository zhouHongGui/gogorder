package com.ruoyi.system.domain.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单详情展示对象（{@code /api/c/order/{orderId}} 返回）。
 * 含完整时间线与金额，金额单位均为分。各状态码带 Desc 中文描述。
 */
public class OrderDetailView
{
    private Long orderId;                  // 订单 ID
    private String orderNo;                // 订单号
    private Long shopId;                   // 门店 ID
    private String shopName;               // 门店名（门店删除时兜底空串）
    private String shopPhone;              // 门店电话
    private String reservedPhone;          // 下单用户手机号
    private String orderType;              // 订单类型 NORMAL/PREORDER
    private String orderTypeDesc;          // 订单类型中文
    private LocalDateTime scheduledPickupTime;  // 预约取餐时间（仅预订单）
    private Integer orderStatus;           // 订单状态码
    private String orderStatusDesc;        // 订单状态中文
    private Integer payStatus;             // 支付状态码
    private String payStatusDesc;          // 支付状态中文
    private Integer refundStatus;          // 退款状态码
    private String refundStatusDesc;       // 退款状态中文
    private Integer productAmount;         // 商品总金额（分）
    private Integer packFee;               // 包装费（分）
    private Integer totalAmount;           // 应付合计（分）
    private String remark;                 // 用户备注
    private String pickupDisplay;          // 取餐展示号
    private String pickupToken;            // 取餐核销令牌
    private LocalDate pickupDate;          // 取餐归属日期
    private Integer queueAheadOrders;      // 当前订单前方制作订单数
    private Integer queueAheadCups;        // 当前订单前方制作杯数
    private LocalDateTime payTime;         // 支付时间
    private LocalDateTime acceptTime;      // 接单时间
    private LocalDateTime makeStartTime;   // 开始制作时间
    private LocalDateTime completeMakeTime;// 制作完成时间
    private LocalDateTime verifyTime;      // 核销时间
    private LocalDateTime cancelTime;      // 取消时间
    private String cancelReason;           // 取消原因
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
    public String getShopPhone() { return shopPhone; }
    public void setShopPhone(String shopPhone) { this.shopPhone = shopPhone; }
    public String getReservedPhone() { return reservedPhone; }
    public void setReservedPhone(String reservedPhone) { this.reservedPhone = reservedPhone; }
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
    public String getPickupToken() { return pickupToken; }
    public void setPickupToken(String pickupToken) { this.pickupToken = pickupToken; }
    public LocalDate getPickupDate() { return pickupDate; }
    public void setPickupDate(LocalDate pickupDate) { this.pickupDate = pickupDate; }
    public Integer getQueueAheadOrders() { return queueAheadOrders; }
    public void setQueueAheadOrders(Integer queueAheadOrders) { this.queueAheadOrders = queueAheadOrders; }
    public Integer getQueueAheadCups() { return queueAheadCups; }
    public void setQueueAheadCups(Integer queueAheadCups) { this.queueAheadCups = queueAheadCups; }
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
