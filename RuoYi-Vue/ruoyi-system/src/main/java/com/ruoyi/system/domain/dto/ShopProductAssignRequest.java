package com.ruoyi.system.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 门店商品分配请求
 */
public class ShopProductAssignRequest
{
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    @NotEmpty(message = "请选择商品")
    private List<Long> productIds;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
}
