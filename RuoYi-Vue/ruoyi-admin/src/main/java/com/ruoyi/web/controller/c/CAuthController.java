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

@RestController
@RequestMapping("/api/c/auth")
public class CAuthController
{
    @Autowired
    private ICAuthService authService;

    @PostMapping("/send-sms")
    @RateLimiter(key = "c:auth:send-sms:", time = 60, count = 20, limitType = LimitType.IP)
    public AjaxResult sendSms(@Validated @RequestBody CSendSmsRequest request)
    {
        return AjaxResult.success(authService.sendSms(request.getPhone(), IpUtils.getIpAddr()));
    }

    @PostMapping("/login-by-sms")
    @RateLimiter(key = "c:auth:login-by-sms:", time = 60, count = 30, limitType = LimitType.IP)
    public AjaxResult loginBySms(@Validated @RequestBody CSmsLoginRequest request)
    {
        return AjaxResult.success(authService.loginBySms(request.getPhone(), request.getCode()));
    }

    @PostMapping("/login-by-wechat")
    public AjaxResult loginByWechat(@Validated @RequestBody CWechatLoginRequest request)
    {
        return AjaxResult.success(authService.loginByWechat(request.getCode()));
    }

    @PostMapping("/bind-phone")
    public AjaxResult bindPhone(@Validated @RequestBody CWechatBindPhoneRequest request)
    {
        Map<String, Object> result = authService.bindWechatPhone(request.getBindTicket(), request.getPhoneCode());
        return AjaxResult.success(result);
    }
}
