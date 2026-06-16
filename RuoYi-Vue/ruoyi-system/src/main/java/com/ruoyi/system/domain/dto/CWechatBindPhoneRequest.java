package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信绑定手机号请求（{@code /api/c/auth/bind-phone}，loginByWechat 未绑定时调用）。
 */
public class CWechatBindPhoneRequest
{
    /** 绑定凭证（loginByWechat 返回，5 分钟有效，内含 openid/unionid）。 */
    @NotBlank(message = "绑定凭证不能为空")
    private String bindTicket;

    /** 微信获取手机号的 phone code（新版动态码，与 login code 分开）。 */
    @NotBlank(message = "微信手机号code不能为空")
    private String phoneCode;

    public String getBindTicket() { return bindTicket; }
    public void setBindTicket(String bindTicket) { this.bindTicket = bindTicket; }
    public String getPhoneCode() { return phoneCode; }
    public void setPhoneCode(String phoneCode) { this.phoneCode = phoneCode; }
}
