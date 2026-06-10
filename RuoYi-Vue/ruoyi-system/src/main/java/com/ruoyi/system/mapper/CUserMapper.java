package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.CUserWechat;

public interface CUserMapper
{
    CUser selectById(Long id);

    CUser selectByPhone(String phone);

    int insertUserIfAbsent(String phone);

    int insertBalanceIfAbsent(Long userId);

    int updateUser(CUser user);

    CUserWechat selectWechatByOpenid(@Param("platform") String platform, @Param("openid") String openid);

    int insertWechatIfAbsent(CUserWechat wechat);
}
