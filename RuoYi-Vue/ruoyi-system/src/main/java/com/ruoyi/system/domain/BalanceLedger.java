package com.ruoyi.system.domain;

import java.time.LocalDateTime;

/**
 * 余额流水实体（对应 {@code balance_ledger}，余额每次变动的审计记录）。
 *
 * <h3>type 取值（见 {@link com.ruoyi.common.enums.BalanceChangeTypeEnum}）</h3>
 * PAY=支付扣款 / REFUND=退款入账 / RECHARGE=后台充值 等。amount 带符号：扣款为负、入账为正。
 * 金额单位：分。
 *
 * <h3>幂等键</h3>
 * 支付：{@code {orderId}:balance:PAY}；退款：{@code {orderId}:balance:REFUND}（唯一，防重复记账）。
 */
public class BalanceLedger
{
    /** 主键。 */
    private Long id;
    /** 用户 ID。 */
    private Long userId;
    /** 变动类型：PAY/REFUND/RECHARGE 等。 */
    private String type;
    /** 变动金额（分，带符号：扣款负、入账正）。 */
    private Integer amount;
    /** 变动前余额。 */
    private Integer beforeBalance;
    /** 变动后余额。 */
    private Integer afterBalance;
    /** 关联订单 ID（充值等无订单时为空）。 */
    private Long orderId;
    /** 操作人 ID（后台操作记录管理员，自助操作为空）。 */
    private Long operatorId;
    /** 幂等键（唯一，防重复记账）。 */
    private String idempotentKey;
    /** 备注（退款时填取消原因等）。 */
    private String remark;
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getBeforeBalance() { return beforeBalance; }
    public void setBeforeBalance(Integer beforeBalance) { this.beforeBalance = beforeBalance; }
    public Integer getAfterBalance() { return afterBalance; }
    public void setAfterBalance(Integer afterBalance) { this.afterBalance = afterBalance; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getOperatorId() { return operatorId; }
    public void setOperatorId(Long operatorId) { this.operatorId = operatorId; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
