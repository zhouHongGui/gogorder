package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.PayResponse;

public interface IBalanceService
{
    PayResponse pay(Long userId, Long orderId);
}
