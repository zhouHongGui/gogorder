package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 门店商品修改请求
 */
public class ShopProductUpdateRequest
{
    @Min(value = 0, message = "门店售价不能小于0")
    private Integer price;

    private Boolean useBasePrice;

    @Min(value = 0, message = "商品状态不正确")
    @Max(value = 1, message = "商品状态不正确")
    private Integer status;

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
