package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 库存流水 stock_ledger
 */
public class StockLedger extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long shopProductId;
    private String changeType;
    private Integer changeAmount;
    private Integer beforeStock;
    private Integer afterStock;
    private Long orderId;
    private String idempotentKey;
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
