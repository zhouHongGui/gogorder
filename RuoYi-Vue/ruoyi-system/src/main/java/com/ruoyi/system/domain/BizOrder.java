package com.ruoyi.system.domain;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 订单主表实体（对应 {@code biz_order}，订单是交易核心，接手必读）。
 *
 * <h3>状态三字段拆分（互不耦合）</h3>
 * <ul>
 *   <li>{@code orderStatus}：订单生命周期。0待支付 1已接单 2制作中 3待取餐 4已完成 5已取消。</li>
 *   <li>{@code payStatus}：支付态。0待支付 1已支付 2已退款。</li>
 *   <li>{@code refundStatus}：退款态。0无退款 1退款成功。</li>
 * </ul>
 *
 * <h3>关键设计</h3>
 * <ul>
 *   <li>下单幂等：{@code (userId, submitKey)} 唯一，防重复下单。</li>
 *   <li>取餐码双字段：{@code pickupToken}(全局唯一核销码) + {@code pickupDisplay}(门店日内展示号)。</li>
 *   <li>金额单位：分（int）。{@code totalAmount = productAmount + packFee}。</li>
 *   <li>表名用 {@code biz_order} 而非 {@code order}（MySQL 保留字）。</li>
 * </ul>
 */
public class BizOrder
{
    /** 主键。 */
    private Long id;
    /** 订单号（对外展示），格式 yyyyMMddHHmmss+6位随机，唯一。 */
    private String orderNo;
    /** 下单幂等键（客户端 submitToken），配合 userId 唯一，防重复下单。 */
    private String submitKey;
    /** 取餐核销令牌（12位随机，全局唯一），支付成功后生成，门店扫码核销用。 */
    private String pickupToken;
    /** 取餐展示号（字母+3位数字，门店日内递增），用户叫号展示用。 */
    private String pickupDisplay;
    /** 取餐归属日期：即时单=支付日，预订单=预约日，防跨天取餐号碰撞。 */
    private LocalDate pickupDate;
    /** 下单用户 ID。 */
    private Long userId;
    /** 门店 ID。 */
    private Long shopId;
    /** 订单类型：NORMAL=即时单 / PREORDER=预订单。 */
    private String orderType;
    /** 预约取餐时间（仅预订单有）。 */
    private LocalDateTime scheduledPickupTime;
    /** 订单状态：0待支付 1已接单 2制作中 3待取餐 4已完成 5已取消。 */
    private Integer orderStatus;
    /** 支付状态：0待支付 1已支付 2已退款。 */
    private Integer payStatus;
    /** 退款状态：0无退款 1退款成功。 */
    private Integer refundStatus;
    /** 商品总金额（分）。 */
    private Integer productAmount;
    /** 包装费（分，= 每杯包装费 × 杯数）。 */
    private Integer packFee;
    /** 应付合计（分，= productAmount + packFee）。 */
    private Integer totalAmount;
    /** 用户备注。 */
    private String remark;
    /** 支付时间。 */
    private LocalDateTime payTime;
    /** 接单时间（支付成功即自动接单）。 */
    private LocalDateTime acceptTime;
    /** 开始制作时间。 */
    private LocalDateTime makeStartTime;
    /** 制作完成时间。 */
    private LocalDateTime completeMakeTime;
    /** 核销时间（用户取餐扫码核销）。 */
    private LocalDateTime verifyTime;
    /** 取消时间。 */
    private LocalDateTime cancelTime;
    /** 取消原因。 */
    private String cancelReason;
    /** 创建（下单）时间，支付超时判断以此为基准。 */
    private LocalDateTime createTime;
    /** 更新时间。 */
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
