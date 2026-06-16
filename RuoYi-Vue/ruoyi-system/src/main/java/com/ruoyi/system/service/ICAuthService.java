package com.ruoyi.system.service;

import java.util.Map;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CUserUpdateRequest;

/**
 * C 端认证与用户服务契约。实现见 {@link com.ruoyi.system.service.impl.CAuthServiceImpl}（含频控/防爆破/微信绑定逻辑）。
 */
public interface ICAuthService
{
    /** 发送短信验证码（含频控；mock 模式返回 mockCode）。 */
    Map<String, Object> sendSms(String phone, String ip);

    /** 短信验证码登录（防爆破，自动注册）。 */
    Map<String, Object> loginBySms(String phone, String code);

    /** 微信小程序登录（已绑定返 token，未绑定返 bindTicket）。 */
    Map<String, Object> loginByWechat(String code);

    /** 微信绑定手机号（bindTicket + phone code 完成绑定并签发 token）。 */
    Map<String, Object> bindWechatPhone(String bindTicket, String phoneCode);

    /** 获取用户信息（校验账号可用）。 */
    CUser getUserInfo(Long userId);

    /** 获取用户余额。 */
    Map<String, Object> getUserBalance(Long userId);

    /** 修改用户资料（昵称/头像）。 */
    CUser updateUser(Long userId, CUserUpdateRequest request);
}
