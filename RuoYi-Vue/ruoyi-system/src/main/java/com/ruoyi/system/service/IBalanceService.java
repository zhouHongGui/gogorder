package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.PayResponse;

/**
 * 余额支付服务契约（外层重试编排）。实现见 {@link com.ruoyi.system.service.impl.BalanceServiceImpl}，
 * 真正扣款事务在 {@code PaymentTransactionService}。
 */
public interface IBalanceService
{
    /** 余额支付订单（处理取餐码碰撞重试，幂等防重复扣款）。 */
    PayResponse pay(Long userId, Long orderId);
}
