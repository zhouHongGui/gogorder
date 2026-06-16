package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存流水实体（对应 {@code stock_ledger}，库存每次变更的审计记录）。
 *
 * <h3>幂等键（接手必读，防重复扣减/恢复）</h3>
 * <ul>
 *   <li>订单库存：{@code {orderId}:{shopProductId}:{changeType}}（changeType=DEDUCT/RESTORE），唯一。</li>
 *   <li>手工调整：{@code adjust:{requestId}}，唯一。</li>
 * </ul>
 *
 * <h3>changeType 取值</h3>
 * {@code DEDUCT}=下单扣减 / {@code RESTORE}=取消/退款恢复 / {@code ADJUST}=后台手工调整。
 *
 * <p>无限库存(-1)参与变更时 {@code changeAmount} 记 0（-1 与数值混算无意义）。
 */
public class StockLedger extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    private Long id;
    /** 门店商品 ID。 */
    private Long shopProductId;
    /** 变更类型：DEDUCT/RESTORE/ADJUST。 */
    private String changeType;
    /** 变更数量（DEDUCT 为负，RESTORE/ADJUST 视情况；无限库存记 0）。 */
    private Integer changeAmount;
    /** 变更前库存。 */
    private Integer beforeStock;
    /** 变更后库存。 */
    private Integer afterStock;
    /** 关联订单 ID（ADJUST 为空）。 */
    private Long orderId;
    /** 幂等键（唯一，防重复变更）。 */
    private String idempotentKey;
    /** 变更原因（手工调整填写）。 */
    private String reason;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public String getChangeType() { return changeType; }
    public void setChangeType(String changeType) { this.changeType = changeType; }
    public Integer getChangeAmount() { return changeAmount; }
    public void setChangeAmount(Integer changeAmount) { this.changeAmount = changeAmount; }
    public Integer getBeforeStock() { return beforeStock; }
    public void setBeforeStock(Integer beforeStock) { this.beforeStock = beforeStock; }
    public Integer getAfterStock() { return afterStock; }
    public void setAfterStock(Integer afterStock) { this.afterStock = afterStock; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getIdempotentKey() { return idempotentKey; }
    public void setIdempotentKey(String idempotentKey) { this.idempotentKey = idempotentKey; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
