package com.ruoyi.system.domain;

import java.time.LocalDateTime;

/**
 * 支付流水实体（对应 {@code payment_ledger}，每笔余额支付的记录）。
 *
 * <p>幂等键 {@code {orderId}:pay} 唯一，且 {@code orderId} 唯一——保证每单仅一次支付记账。
 * V1.0 仅余额支付，故无第三方交易号字段。金额单位：分。
 *
 * <p>已支付订单重复发起支付时，{@code PaymentTransactionService} 用此表做幂等返回。
 */
public class PaymentLedger
{
    /** 主键。 */
    private Long id;
    /** 订单 ID（唯一）。 */
    private Long orderId;
    /** 支付用户 ID。 */
    private Long userId;
    /** 支付金额（分）。 */
    private Integer amount;
    /** 幂等键：orderId:pay（唯一）。 */
    private String idempotentKey;
    /** 支付状态：1=成功。 */
    private Integer status;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
