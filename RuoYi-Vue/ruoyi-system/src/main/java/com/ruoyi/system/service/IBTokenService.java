package com.ruoyi.system.service;

import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.BAuthPrincipal;

/** 门店员工端 JWT 服务。 */
public interface IBTokenService
{
    String createToken(ShopStaff staff);
    BAuthPrincipal parseToken(String token);
}
