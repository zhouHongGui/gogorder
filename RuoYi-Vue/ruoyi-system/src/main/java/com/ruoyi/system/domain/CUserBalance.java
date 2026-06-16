package com.ruoyi.system.domain;

import java.time.LocalDateTime;

/**
 * 用户余额账户实体（对应 {@code c_user_balance}）。
 *
 * <p>每个用户一个余额账户（userId 唯一）。V1.0 仅管理后台手动充值，C 端无充值入口。
 * 余额单位：分。
 *
 * <h3>并发控制</h3>
 * 支付/退款扣减余额采用「悲观锁（selectByUserIdForUpdate）+ updateLockedBalance」，
 * {@code version} 字段保留兼容但当前主流程不再依赖乐观锁重试。每次变动写一条
 * {@code balance_ledger} 流水（幂等键）。
 */
public class CUserBalance
{
    /** 主键。 */
    private Long id;
    /** 用户 ID（唯一，一对一）。 */
    private Long userId;
    /** 余额（分）。 */
    private Integer balance;
    /** 乐观锁版本号（当前主流程用悲观锁，此字段保留兼容）。 */
    private Integer version;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
