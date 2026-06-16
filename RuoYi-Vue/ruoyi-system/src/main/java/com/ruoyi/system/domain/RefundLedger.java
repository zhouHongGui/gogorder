package com.ruoyi.system.domain;

import java.time.LocalDateTime;

/**
 * 退款流水实体（对应 {@code refund_ledger}，每笔退款的记录）。
 *
 * <p>V1.0 退款规则：每单严格一次整单全额退款，不支持部分退款。
 * 幂等键 {@code {orderId}:refund} 唯一 + {@code orderId} 唯一，保证不重复退款。
 * 金额单位：分。
 */
public class RefundLedger
{
    /** 主键。 */
    private Long id;
    /** 订单 ID（唯一）。 */
    private Long orderId;
    /** 退款用户 ID。 */
    private Long userId;
    /** 退款金额（分，= 订单全额）。 */
    private Integer amount;
    /** 幂等键：orderId:refund（唯一）。 */
    private String idempotentKey;
    /** 退款状态：1=成功。 */
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
