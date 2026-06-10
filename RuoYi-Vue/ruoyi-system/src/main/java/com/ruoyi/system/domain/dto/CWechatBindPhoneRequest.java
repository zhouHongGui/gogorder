package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;

public class CWechatBindPhoneRequest
{
    @NotBlank(message = "绑定凭证不能为空")
    private String bindTicket;

    @NotBlank(message = "微信手机号code不能为空")
    private String phoneCode;

    public String getBindTicket() { return bindTicket; }
    public void setBindTicket(String bindTicket) { this.bindTicket = bindTicket; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
}
