package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * 微信小程序登录请求（{@code /api/c/auth/login-by-wechat}）。
 */
public class CWechatLoginRequest
{
    /** 微信 login code（一次性，用于换 openid）。 */
    @NotBlank(message = "微信登录code不能为空")
    private String code;

    public String getCode() { return code; }
    public void setCode(String code) { this.code = code; }
}
