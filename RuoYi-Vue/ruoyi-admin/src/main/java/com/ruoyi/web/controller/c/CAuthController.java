package com.ruoyi.web.controller.c;

import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.utils.ip.IpUtils;
import com.ruoyi.system.domain.dto.CSendSmsRequest;
import com.ruoyi.system.domain.dto.CSmsLoginRequest;
import com.ruoyi.system.domain.dto.CWechatBindPhoneRequest;
import com.ruoyi.system.domain.dto.CWechatLoginRequest;
import com.ruoyi.system.service.ICAuthService;

/**
 * C 端认证接口（{@code /api/c/auth/**}，公开路径，无需登录）。
 *
 * <p>提供三种登录入口：短信验证码登录、微信小程序登录、微信绑定手机号。
 * 关键接口加 IP 级 {@link RateLimiter} 防刷（业务层另有手机号/IP 的日频控与验证码防爆破）。
 */
@RestController
@RequestMapping("/api/c/auth")
public class CAuthController
{
    @Autowired
    private ICAuthService authService;

    /**
     * 发送短信验证码。IP 限流：60 秒 20 次（业务层另有手机号 60s 间隔 + 日 10 条约束）。
     *
     * @param request 含手机号
     */
    @PostMapping("/send-sms")
    @RateLimiter(key = "c:auth:send-sms:", time = 60, count = 20, limitType = LimitType.IP)
    public AjaxResult sendSms(@Validated @RequestBody CSendSmsRequest request)
    {
        return AjaxResult.success(authService.sendSms(request.getPhone(), IpUtils.getIpAddr()));
    }

    /**
     * 短信验证码登录。IP 限流：60 秒 30 次（配合验证码 5 次失败锁定，防暴力枚举）。
     *
     * @param request 含手机号 + 验证码
     */
    @PostMapping("/login-by-sms")
    @RateLimiter(key = "c:auth:login-by-sms:", time = 60, count = 30, limitType = LimitType.IP)
    public AjaxResult loginBySms(@Validated @RequestBody CSmsLoginRequest request)
    {
        return AjaxResult.success(authService.loginBySms(request.getPhone(), request.getCode()));
    }

    /**
     * 微信小程序登录（第一步：login code 换 openid）。
     * 已绑定 → 返回 token；未绑定 → 返回 bindTicket 供 {@link #bindPhone} 使用。
     */
    @PostMapping("/login-by-wechat")
    public AjaxResult loginByWechat(@Validated @RequestBody CWechatLoginRequest request)
    {
        return AjaxResult.success(authService.loginByWechat(request.getCode()));
    }

    /**
     * 微信绑定手机号（第二步）：用 bindTicket + phone code 完成绑定并签发 token。
     */
    @PostMapping("/bind-phone")
    public AjaxResult bindPhone(@Validated @RequestBody CWechatBindPhoneRequest request)
    {
        Map<String, Object> result = authService.bindWechatPhone(request.getBindTicket(), request.getPhoneCode());
        return AjaxResult.success(result);
    }
}
