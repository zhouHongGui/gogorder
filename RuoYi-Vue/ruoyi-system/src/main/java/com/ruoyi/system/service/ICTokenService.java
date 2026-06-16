package com.ruoyi.system.service;

import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CAuthPrincipal;

/**
 * C 端 JWT Token 服务契约。实现见 {@link com.ruoyi.system.service.impl.CTokenServiceImpl}（含密钥强度校验）。
 */
public interface ICTokenService
{
    /** 签发 JWT（claim 含 userId/phone，默认 7 天有效）。 */
    String createToken(CUser user);

    /** 解析 JWT（非法/过期抛 ServiceException 401）。 */
    CAuthPrincipal parseToken(String token);
}
