package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Size;

/** 门店员工端出餐请求：扫码传 pickupToken，手动输入传订单号/取餐号 code。 */
public class BOrderScanOutRequest
{
    @Size(max = 64, message = "出餐码格式不正确")
    private String code;

    @Size(max = 64, message = "出餐码格式不正确")
    private String pickupToken;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }

    public String getPickupToken() { return pickupToken; }
    public void setPickupToken(String pickupToken) { this.pickupToken = pickupToken; }

    public String getEffectiveCode()
    {
        if (code != null && !code.trim().isEmpty())
        {
            return code;
        }
        return pickupToken;
    }
}
