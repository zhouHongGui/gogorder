package com.ruoyi.system.service;

import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CAuthPrincipal;

public interface ICTokenService
{
    String createToken(CUser user);

    CAuthPrincipal parseToken(String token);
}
