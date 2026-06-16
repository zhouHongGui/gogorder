package com.ruoyi.system.domain.dto;

import java.util.Collections;
import java.util.List;

/**
 * 购物车视图（查询/增删改后返回前端）。金额单位：分。
 */
public class CCartView
{
    /** 门店 ID。 */
    private Long shopId;
    /** 门店名。 */
    private String shopName;
    /** 商品条目列表（按 cartItemId 排序）。 */
    private List<CCartItem> items = Collections.emptyList();
    /** 合计金额（分）。 */
    private Integer totalAmount = 0;
    /** 合计数量（总杯数，用于包装费计算）。 */
    private Integer totalCount = 0;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
    public List<CCartItem> getItems() { return items; }
    public void setItems(List<CCartItem> items) { this.items = items; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public Integer getTotalCount() { return totalCount; }
    public void setTotalCount(Integer totalCount) { this.totalCount = totalCount; }
}
