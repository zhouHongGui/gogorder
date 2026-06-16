package com.ruoyi.system.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

/**
 * 门店商品分配请求（批量把商品库商品分配到门店，建立 shop_product 关联）。
 */
public class ShopProductAssignRequest
{
    /** 目标门店 ID。 */
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    /** 待分配的商品 ID 列表。 */
    @NotEmpty(message = "请选择商品")
    private List<Long> productIds;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public List<Long> getProductIds() { return productIds; }
    public void setProductIds(List<Long> productIds) { this.productIds = productIds; }
}
