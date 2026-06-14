package com.ruoyi.system.domain.dto;

import java.util.Collections;
import java.util.List;

public class CCartView
{
    private Long shopId;
    private String shopName;
    private List<CCartItem> items = Collections.emptyList();
    private Integer totalAmount = 0;
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
