package com.ruoyi.system.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;

/**
 * 批量移除门店商品请求
 */
public class ShopProductBatchRemoveRequest
{
    @NotEmpty(message = "请选择要移除的门店商品")
    private List<Long> ids;

    public List<Long> getIds() { return ids; }
    public void setIds(List<Long> ids) { this.ids = ids; }
}
