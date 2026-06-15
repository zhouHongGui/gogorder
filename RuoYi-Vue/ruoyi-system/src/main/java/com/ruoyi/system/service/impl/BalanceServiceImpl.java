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

@Service
public class BalanceServiceImpl implements IBalanceService
{
    private static final Logger log = LoggerFactory.getLogger(BalanceServiceImpl.class);
    private static final String TOKEN_CHARS = "ABCDEFGHJKLMNPQRSTUVWXYZ23456789";
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    @Autowired private PaymentTransactionService paymentTransactionService;

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

    private String generatePickupToken()
    {
        StringBuilder value = new StringBuilder(12);
        for (int i = 0; i < 12; i++)
        {
            value.append(TOKEN_CHARS.charAt(SECURE_RANDOM.nextInt(TOKEN_CHARS.length())));
        }
        return value.toString();
    }

    private boolean containsConstraint(DuplicateKeyException e, String constraint)
    {
        return String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase()
                .contains(constraint.toLowerCase());
    }
}
