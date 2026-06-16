package com.ruoyi.system.service.impl;

import java.security.SecureRandom;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.dto.PayResponse;
import com.ruoyi.system.service.IBalanceService;

/**
 * 余额支付编排服务（外层重试）。
 *
 * <p>真正的扣款逻辑在 {@link PaymentTransactionService#payAttempt}（独立事务）。
 * 本类负责生成「取餐核销令牌」{@code pickup_token} 并处理其唯一约束碰撞重试：
 * pickup_token 是 12 位随机串，靠 {@code uk_pickup_token} 唯一索引保证全局唯一，
 * 极小概率碰撞时换一个重试（最多 3 次）。
 *
 * <p>设计要点：{@code payAttempt} 用 {@code REQUIRES_NEW} 传播，token 碰撞抛
 * {@link DuplicateKeyException} 时本方法捕获并重试，每次重试开全新事务。
 *
 * @see PaymentTransactionService 实际支付事务
 */
@Service
public class BalanceServiceImpl implements IBalanceService
{
    private static final Logger log = LoggerFactory.getLogger(BalanceServiceImpl.class);
    /** 取餐令牌字符集：去除易混淆字符（0/O、1/I/L），避免口头/手抄混淆。 */
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired private PaymentTransactionService paymentTransactionService;

    /**
     * 余额支付订单。
     *
     * <p>生成 pickup_token 后调用 {@link PaymentTransactionService#payAttempt}；
     * 若 token 碰撞唯一约束则换号重试，最多 3 次。
     *
     * @param userId  当前用户
     * @param orderId 待支付订单
     * @return 支付结果
     * @throws ServiceException 重试用尽、或业务异常（余额不足/订单状态等，由 payAttempt 抛出）
     */
    @Override
    public PayResponse pay(Long userId, Long orderId)
    {
        for (int retry = 0; retry < 3; retry++)
        {
            String pickupToken = generatePickupToken();
            try
            {
                return paymentTransactionService.payAttempt(userId, orderId, pickupToken);
            }
            catch (DuplicateKeyException e)
            {
                // 仅当是 pickup_token 唯一约束碰撞且仍有重试机会时换号重试；其他唯一约束冲突直接抛出。
                if (containsConstraint(e, "uk_pickup_token") && retry < 2)
                {
                    log.warn("取餐令牌碰撞，orderId={} retry={}", orderId, retry + 1);
                    continue;
                }
                throw e;
            }
        }
        throw new ServiceException("支付失败，请重试");
    }

    /**
     * 生成 12 位取餐核销令牌（从去混淆字符集中随机取）。
     */
    private String generatePickupToken()
    {
        StringBuilder value = new StringBuilder(12);
        for (int i = 0; i < 12; i++)
        {
            value.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return value.toString();
    }

    /**
     * 判断唯一约束异常是否由指定约束触发（匹配异常 cause message，大小写不敏感）。
     */
    private boolean containsConstraint(DuplicateKeyException e, String constraint)
    {
        return String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase()
                .contains(constraint.toLowerCase());
    }
}
