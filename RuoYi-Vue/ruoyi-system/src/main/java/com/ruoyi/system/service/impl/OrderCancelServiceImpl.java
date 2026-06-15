package com.ruoyi.system.service.impl;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.enums.BalanceChangeTypeEnum;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.BalanceLedger;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.BizOrderItem;
import com.ruoyi.system.domain.CUserBalance;
import com.ruoyi.system.domain.RefundLedger;
import com.ruoyi.system.mapper.BalanceLedgerMapper;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.CUserBalanceMapper;
import com.ruoyi.system.mapper.RefundLedgerMapper;
import com.ruoyi.system.service.IOrderCancelService;
import com.ruoyi.system.service.IProductCenterService;

@Service
public class OrderCancelServiceImpl implements IOrderCancelService
{
    private static final Logger log = LoggerFactory.getLogger(OrderCancelServiceImpl.class);

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;
    @Autowired private IProductCenterService productCenterService;
    @Autowired private CUserBalanceMapper cUserBalanceMapper;
    @Autowired private BalanceLedgerMapper balanceLedgerMapper;
    @Autowired private RefundLedgerMapper refundLedgerMapper;

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cancelUnpaidOrder(Long userId, Long orderId)
    {
        BizOrder order = bizOrderMapper.selectById(orderId);
        if (order == null || !Objects.equals(order.getUserId(), userId))
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        if (!Integer.valueOf(OrderStatusEnum.PENDING_PAY.getCode()).equals(order.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_PENDING.getCode()).equals(order.getPayStatus()))
        {
            throw new ServiceException("订单状态不允许取消", HttpStatus.CONFLICT);
        }
        int rows = bizOrderMapper.cancelOrder(orderId, OrderStatusEnum.CANCELLED.getCode(),
                PayStatusEnum.PAY_PENDING.getCode(), LocalDateTime.now(), "用户取消");
        if (rows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        restoreOrderStock(orderId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cancelTimeoutOrder(Long orderId)
    {
        int rows = bizOrderMapper.cancelOrder(orderId, OrderStatusEnum.CANCELLED.getCode(),
                PayStatusEnum.PAY_TIMEOUT.getCode(), LocalDateTime.now(), "支付超时自动取消");
        if (rows > 0)
        {
            restoreOrderStock(orderId);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cancelPaidOrderWithRefund(Long orderId, String cancelReason, Long operatorId)
    {
        BizOrder locked = bizOrderMapper.selectByIdForUpdate(orderId);
        if (locked == null)
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        if (Integer.valueOf(RefundStatusEnum.REFUND_SUCCESS.getCode()).equals(locked.getRefundStatus()))
        {
            log.info("退款幂等返回，orderId={} 已退款", orderId);
            return;
        }
        if (!Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(locked.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(locked.getPayStatus())
                || !Integer.valueOf(RefundStatusEnum.REFUND_NONE.getCode()).equals(locked.getRefundStatus()))
        {
            throw new ServiceException("订单状态不允许退款", HttpStatus.CONFLICT);
        }

        CUserBalance balance = cUserBalanceMapper.selectByUserIdForUpdate(locked.getUserId());
        if (balance == null)
        {
            throw new ServiceException("余额账户不存在");
        }
        int afterBalance = 0;
        for (int retry = 0; retry < 3; retry++)
        {
            int rows = cUserBalanceMapper.updateBalance(locked.getUserId(), locked.getTotalAmount(), balance.getVersion());
            if (rows > 0)
            {
                afterBalance = balance.getBalance() + locked.getTotalAmount();
                break;
            }
            if (retry == 2)
            {
                throw new ServiceException("退款失败，请重试");
            }
            balance = cUserBalanceMapper.selectByUserIdForUpdate(locked.getUserId());
        }

        BalanceLedger balanceLedger = new BalanceLedger();
        balanceLedger.setUserId(locked.getUserId());
        balanceLedger.setType(BalanceChangeTypeEnum.REFUND.getCode());
        balanceLedger.setAmount(locked.getTotalAmount());
        balanceLedger.setBeforeBalance(balance.getBalance());
        balanceLedger.setAfterBalance(afterBalance);
        balanceLedger.setOrderId(orderId);
        balanceLedger.setOperatorId(operatorId);
        balanceLedger.setIdempotentKey(orderId + ":balance:REFUND");
        balanceLedger.setRemark(String.valueOf(cancelReason == null ? "" : cancelReason));
        balanceLedgerMapper.insert(balanceLedger);

        RefundLedger refund = new RefundLedger();
        refund.setOrderId(orderId);
        refund.setUserId(locked.getUserId());
        refund.setAmount(locked.getTotalAmount());
        refund.setIdempotentKey(orderId + ":refund");
        refund.setStatus(1);
        refundLedgerMapper.insert(refund);

        int orderRows = bizOrderMapper.updateOrderForRefund(orderId, OrderStatusEnum.CANCELLED.getCode(),
                PayStatusEnum.PAY_REFUNDED.getCode(), RefundStatusEnum.REFUND_SUCCESS.getCode(),
                LocalDateTime.now(), cancelReason);
        if (orderRows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        restoreOrderStock(orderId);
    }

    private void restoreOrderStock(Long orderId)
    {
        List<BizOrderItem> items = bizOrderItemMapper.selectByOrderId(orderId);
        Map<Long, Integer> restores = new LinkedHashMap<>();
        for (BizOrderItem item : items)
        {
            restores.merge(item.getShopProductId(), item.getQuantity(), Integer::sum);
        }
        List<Long> sortedIds = new ArrayList<>(restores.keySet());
        Collections.sort(sortedIds);
        for (Long shopProductId : sortedIds)
        {
            productCenterService.restoreStock(orderId, shopProductId, restores.get(shopProductId));
        }
    }
}
