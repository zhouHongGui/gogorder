package com.ruoyi.system.domain;

import java.time.LocalDateTime;

public class BalanceLedger
{
    private Long id;
    private Long userId;
    private String type;
    private Integer amount;
    private Integer beforeBalance;
    private Integer afterBalance;
    private Long orderId;
    private Long operatorId;
    private String idempotentKey;
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
