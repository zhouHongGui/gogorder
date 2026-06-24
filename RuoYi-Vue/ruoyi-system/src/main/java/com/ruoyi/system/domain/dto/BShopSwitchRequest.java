package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/** 门店员工端切换当前门店请求。 */
public class BShopSwitchRequest
{
    @NotNull(message = "门店不能为空")
    @Positive(message = "门店ID不正确")
    private Long shopId;

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }
}
