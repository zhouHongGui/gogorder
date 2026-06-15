package com.ruoyi.system.service.impl;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.config.GogorderOrderProperties;
import com.ruoyi.common.enums.BalanceChangeTypeEnum;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BalanceLedger;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.CUserBalance;
import com.ruoyi.system.domain.PaymentLedger;
import com.ruoyi.system.domain.dto.PayResponse;
import com.ruoyi.system.mapper.BalanceLedgerMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.CUserBalanceMapper;
import com.ruoyi.system.mapper.PaymentLedgerMapper;

@Service
public class PaymentTransactionService
{
    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionService.class);
    private static final int PICKUP_DISPLAY_GROUP_SIZE = 999;
    private static final int PICKUP_DISPLAY_MAX_SEQUENCE = 26 * PICKUP_DISPLAY_GROUP_SIZE;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private CUserBalanceMapper cUserBalanceMapper;
    @Autowired private BalanceLedgerMapper balanceLedgerMapper;
    @Autowired private PaymentLedgerMapper paymentLedgerMapper;
    @Autowired private GogorderOrderProperties orderProperties;

    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public PayResponse payAttempt(Long userId, Long orderId, String pickupToken)
    {
        BizOrder locked = bizOrderMapper.selectByIdForUpdate(orderId);
        if (locked == null || !Objects.equals(locked.getUserId(), userId))
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(locked.getPayStatus()))
        {
            PaymentLedger ledger = paymentLedgerMapper.selectByIdempotentKey(orderId + ":pay");
            if (ledger == null)
            {
                log.error("支付数据不一致: orderId={} pay_status=SUCCESS 但 payment_ledger 缺失", orderId);
                throw new ServiceException("订单支付数据异常，请联系管理员");
            }
            CUserBalance current = cUserBalanceMapper.selectByUserId(userId);
            return new PayResponse(locked.getOrderNo(), locked.getPickupDisplay(), current == null ? 0 : current.getBalance());
        }
        if (!Integer.valueOf(OrderStatusEnum.PENDING_PAY.getCode()).equals(locked.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_PENDING.getCode()).equals(locked.getPayStatus()))
        {
            throw new ServiceException("订单状态不允许支付", HttpStatus.CONFLICT);
        }
        if (locked.getCreateTime() != null && locked.getCreateTime().isBefore(
                LocalDateTime.now().minusMinutes(orderProperties.getPayTimeoutMinutes())))
        {
            throw new ServiceException("订单已超时，请重新下单", HttpStatus.BAD_REQUEST);
        }

        LocalDate pickupDate = LocalDate.now();
        if (OrderTypeEnum.PREORDER.getCode().equals(locked.getOrderType()))
        {
            if (locked.getScheduledPickupTime() == null)
            {
                log.error("预订单数据不一致: orderId={} scheduled_pickup_time 为空", orderId);
                throw new ServiceException("预订单取餐时间异常，请重新下单", HttpStatus.CONFLICT);
            }
            pickupDate = locked.getScheduledPickupTime().toLocalDate();
        }
        CUserBalance balance = cUserBalanceMapper.selectByUserIdForUpdate(userId);
        if (balance == null)
        {
            throw new ServiceException("余额账户不存在");
        }
        if (balance.getBalance() < locked.getTotalAmount())
        {
            throw new ServiceException("余额不足", HttpStatus.BAD_REQUEST).setData(
                    Map.of("balance", balance.getBalance(), "required", locked.getTotalAmount()));
        }

        int rows = cUserBalanceMapper.updateLockedBalance(userId, -locked.getTotalAmount());
        if (rows == 0)
        {
            throw new ServiceException("余额扣减失败，请重试");
        }
        int afterBalance = balance.getBalance() - locked.getTotalAmount();

        bizOrderMapper.allocatePickupDisplay(locked.getShopId(), pickupDate);
        int sequence = bizOrderMapper.selectPickupSeq(locked.getShopId(), pickupDate);
        String pickupDisplay = formatPickupDisplay(sequence);

        BalanceLedger balanceLedger = new BalanceLedger();
        balanceLedger.setUserId(userId);
        balanceLedger.setType(BalanceChangeTypeEnum.PAY.getCode());
        balanceLedger.setAmount(-locked.getTotalAmount());
        balanceLedger.setBeforeBalance(balance.getBalance());
        balanceLedger.setAfterBalance(afterBalance);
        balanceLedger.setOrderId(orderId);
        balanceLedger.setIdempotentKey(orderId + ":balance:PAY");
        balanceLedger.setRemark("");
        balanceLedgerMapper.insert(balanceLedger);

        PaymentLedger paymentLedger = new PaymentLedger();
        paymentLedger.setOrderId(orderId);
        paymentLedger.setUserId(userId);
        paymentLedger.setAmount(locked.getTotalAmount());
        paymentLedger.setIdempotentKey(orderId + ":pay");
        paymentLedger.setStatus(1);
        paymentLedgerMapper.insert(paymentLedger);

        LocalDateTime now = LocalDateTime.now();
        rows = bizOrderMapper.updateOrderStatus(orderId, OrderStatusEnum.ACCEPTED.getCode(),
                PayStatusEnum.PAY_SUCCESS.getCode(), RefundStatusEnum.REFUND_NONE.getCode(),
                now, now, pickupToken, pickupDisplay, pickupDate);
        if (rows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        return new PayResponse(locked.getOrderNo(), pickupDisplay, afterBalance);
    }

    private String formatPickupDisplay(int sequence)
    {
        if (sequence <= 0 || sequence > PICKUP_DISPLAY_MAX_SEQUENCE)
        {
            throw new ServiceException("当日取餐号已满，请联系门店");
        }
        int zeroBased = sequence - 1;
        char prefix = (char) ('A' + zeroBased / PICKUP_DISPLAY_GROUP_SIZE);
        int number = zeroBased % PICKUP_DISPLAY_GROUP_SIZE + 1;
        return String.format("%c%03d", prefix, number);
    }
}
