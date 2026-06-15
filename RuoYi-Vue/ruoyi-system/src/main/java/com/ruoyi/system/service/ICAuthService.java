package com.ruoyi.system.service;

import java.util.Map;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CUserUpdateRequest;

public interface ICAuthService
{
    Map<String, Object> sendSms(String phone, String ip);

    Map<String, Object> loginBySms(String phone, String code);

    Map<String, Object> loginByWechat(String code);

    Map<String, Object> bindWechatPhone(String bindTicket, String phoneCode);

    CUser getUserInfo(Long userId);

    Map<String, Object> getUserBalance(Long userId);

    CUser updateUser(Long userId, CUserUpdateRequest request);
}
