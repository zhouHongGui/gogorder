package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 门店状态修改请求。status：0=休息 1=营业 2=暂停即时（仅可预订单）。
 */
public class ShopStatusRequest
{
    /** 目标状态：0休息 1营业 2暂停即时。 */
    @NotNull(message = "门店状态不能为空")
    @Min(value = 0, message = "门店状态不正确")
    @Max(value = 2, message = "门店状态不正确")
    private Integer status;

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }
}
