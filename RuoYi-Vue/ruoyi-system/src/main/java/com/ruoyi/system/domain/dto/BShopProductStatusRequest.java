package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/** 门店员工端商品销售状态请求（销售中/已停售）。 */
public class BShopProductStatusRequest
{
    @NotNull(message = "商品状态不能为空")
    @Min(value = 0, message = "商品状态不正确")
    @Max(value = 1, message = "商品状态不正确")
    private Integer status;

    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
