package com.ruoyi.system.mapper;

import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.CUserBalance;

public interface CUserBalanceMapper
{
    CUserBalance selectByUserId(Long userId);
    CUserBalance selectByUserIdForUpdate(Long userId);
    int updateBalance(@Param("userId") Long userId, @Param("delta") int delta, @Param("version") int version);
}
