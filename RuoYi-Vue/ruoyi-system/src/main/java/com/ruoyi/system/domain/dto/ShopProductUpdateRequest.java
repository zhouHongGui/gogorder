package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 门店商品修改请求（售价/状态/排序）。
 * <p>互斥规则：useBasePrice=true（使用基础价）时不可同时设 price；两者都不传则保持原值。
 */
public class ShopProductUpdateRequest
{
    /** 门店售价（分），与 useBasePrice 互斥。 */
    @Min(value = 0, message = "门店售价不能小于0")
    private Integer price;

    /** 是否使用商品基础价（true 时清空 price，回落 basePrice）。 */
    private Boolean useBasePrice;

    /** 上下架状态：0=下架 1=上架。 */
    @Min(value = 0, message = "商品状态不正确")
    @Max(value = 1, message = "商品状态不正确")
    private Integer status;

    /** 排序号。 */
    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    public Integer getPrice()
    {
        return price;
    }

    public void setPrice(Integer price)
    {
        this.price = price;
    }

    public Boolean getUseBasePrice()
    {
        return useBasePrice;
    }

    public void setUseBasePrice(Boolean useBasePrice)
    {
        this.useBasePrice = useBasePrice;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }
}
