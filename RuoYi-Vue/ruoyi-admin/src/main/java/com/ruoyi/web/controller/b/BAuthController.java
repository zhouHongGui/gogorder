package com.ruoyi.web.controller.b;

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
import com.ruoyi.framework.web.service.StaffAuthService;
import com.ruoyi.system.domain.dto.BPasswordLoginRequest;
import com.ruoyi.system.domain.dto.BSendSmsRequest;
import com.ruoyi.system.domain.dto.BSmsLoginRequest;

/** 门店员工端公开认证接口。 */
@RestController
@RequestMapping("/api/b/auth")
public class BAuthController
{
    @Autowired
    private StaffAuthService staffAuthService;

    @PostMapping("/send-sms")
    @RateLimiter(key = "staff:auth:send-sms:", time = 60, count = 20, limitType = LimitType.IP)
    public AjaxResult sendSms(@Validated @RequestBody BSendSmsRequest request)
    {
        return AjaxResult.success(staffAuthService.sendSms(request.getPhone(), IpUtils.getIpAddr()));
    }

    @PostMapping("/login/sms")
    @RateLimiter(key = "staff:auth:login:sms:", time = 60, count = 30, limitType = LimitType.IP)
    public AjaxResult loginBySms(@Validated @RequestBody BSmsLoginRequest request)
    {
        return AjaxResult.success(staffAuthService.loginBySms(request.getPhone(), request.getCode(), IpUtils.getIpAddr()));
    }

    @PostMapping("/login/password")
    @RateLimiter(key = "staff:auth:login:password:", time = 60, count = 20, limitType = LimitType.IP)
    public AjaxResult loginByPassword(@Validated @RequestBody BPasswordLoginRequest request)
    {
        return AjaxResult.success(staffAuthService.loginByPassword(
                request.getAccount(), request.getPassword(), IpUtils.getIpAddr()));
    }
}
