package com.ruoyi.system.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

/**
 * 批量下架门店商品请求。存在进行中订单引用的商品会被拒绝（先处理订单再下架）。
 */
public class ShopProductBatchRemoveRequest
{
    /** 待下架的 shop_product.id 列表。 */
    @NotEmpty(message = "请选择要移除的门店商品")
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
