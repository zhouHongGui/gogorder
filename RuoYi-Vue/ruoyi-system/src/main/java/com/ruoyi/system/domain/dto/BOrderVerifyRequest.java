package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 门店员工端核销请求。 */
public class BOrderVerifyRequest
{
    @NotBlank(message = "取餐码不能为空")
    @Size(max = 64, message = "取餐码格式不正确")
    private String pickupToken;

    public String getPickupToken() { return pickupToken; }
    public void setPickupToken(String pickupToken) { this.pickupToken = pickupToken; }
}
