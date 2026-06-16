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

/**
 * 订单取消与退款服务。
 *
 * <h3>三种取消场景</h3>
 * <ul>
 *   <li>{@link #cancelUnpaidOrder}：用户主动取消「未支付」订单（仅还库存，不涉及退款）。</li>
 *   <li>{@link #cancelTimeoutOrder}：定时任务对超时未支付订单的自动取消（仅还库存）。</li>
 *   <li>{@link #cancelPaidOrderWithRefund}：管理端/门店取消「已接单未制作」订单，整单全额退款 + 还库存。</li>
 * </ul>
 *
 * <h3>关键规则（接手必读）</h3>
 * <ul>
 *   <li><b>用户只能取消未支付订单</b>；支付后只能由管理端走退款。</li>
 *   <li><b>开始制作后不允许退款</b>：{@code cancelPaidOrderWithRefund} 仅允许 order_status=1(已接单)，
 *       制作中(2)及以后状态会被拒绝。</li>
 *   <li><b>每单严格一次整单全额退款</b>，不支持部分退款；由 {@code refund_ledger.idempotent_key=orderId:refund} 保证。</li>
 *   <li><b>库存归还幂等</b>：{@code restoreStock} 内部用 {@code stock_ledger} 幂等键
 *       {@code orderId:shopProductId:RESTORE} 保证重复取消不会重复还库存。</li>
 *   <li><b>事务</b>：三个方法都用 {@link Propagation#REQUIRES_NEW}，每个取消/退款独立事务，
 *       失败仅回滚本单，不影响调用方（如定时任务批量取消时单笔失败不影响其他）。</li>
 * </ul>
 *
 * @see PaymentTransactionService 支付（正向扣款）
 */
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

    /**
     * 用户主动取消「未支付」订单。
     *
     * <p>仅允许 order_status=0(待支付) 且 pay_status=0 的订单。取消后归还库存，不涉及资金。
     *
     * @param userId  当前用户
     * @param orderId 订单 ID
     * @throws ServiceException 订单不存在/越权、订单已不是待支付态
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cancelUnpaidOrder(Long userId, Long orderId)
    {
        BizOrder order = bizOrderMapper.selectById(orderId);
        // 越权或不存在统一报「订单不存在」，不泄露订单存在性。
        if (order == null || !Objects.equals(order.getUserId(), userId))
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        // 只允许取消「待支付」订单。
        if (!Integer.valueOf(OrderStatusEnum.PENDING_PAY.getCode()).equals(order.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_PENDING.getCode()).equals(order.getPayStatus()))
        {
            throw new ServiceException("订单状态不允许取消", HttpStatus.CONFLICT);
        }
        // 条件 UPDATE 取消订单（要求 order_status=0 且 pay_status=0）。
        // rows=0 说明并发期间状态已变（如已被支付或已被定时任务取消）。
        int rows = bizOrderMapper.cancelOrder(orderId, OrderStatusEnum.CANCELLED.getCode(),
                PayStatusEnum.PAY_PENDING.getCode(), LocalDateTime.now(), "用户取消");
        if (rows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        // 归还本单扣减的库存。
        restoreOrderStock(orderId);
    }

    /**
     * 超时自动取消（由 {@code PaymentTimeoutTask} 定时任务调用）。
     *
     * <p>用条件 UPDATE 原子地取消订单：rows>0 表示本次成功取消（此前未被用户支付/取消），
     * 此时才需要还库存。rows=0 表示订单已不在待支付态（已被处理），直接跳过，无副作用。
     * 因此天然幂等，多实例/多次执行都安全。
     *
     * @param orderId 待取消订单
     */
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

    /**
     * 管理端/门店取消「已接单未制作」订单并整单全额退款。
     *
     * <p>流程：加锁订单 → 退款幂等/状态校验 → 加锁余额账户 → 回款 → 写余额/退款流水 →
     * 更新订单状态为已取消+已退款 → 还库存。
     *
     * @param orderId      订单 ID
     * @param cancelReason 取消原因（写入订单与流水备注）
     * @param operatorId   操作人 ID（记录到余额流水便于追溯）
     * @throws ServiceException 订单不存在、状态不允许退款（制作中/已完成等）、状态已变更
     */
    @Override
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void cancelPaidOrderWithRefund(Long orderId, String cancelReason, Long operatorId)
    {
        // 【加锁订单】悲观锁，串行化退款操作。
        BizOrder locked = bizOrderMapper.selectByIdForUpdate(orderId);
        if (locked == null)
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        // 【退款幂等】已退款成功则直接返回，防止重复退款。
        if (Integer.valueOf(RefundStatusEnum.REFUND_SUCCESS.getCode()).equals(locked.getRefundStatus()))
        {
            log.debug("退款幂等返回，orderId={} 已退款", orderId);
            return;
        }
        // 【退款前置校验】仅允许「已接单(1) + 已支付(1) + 未退款(0)」。制作中(2)及之后不可退。
        if (!Integer.valueOf(OrderStatusEnum.ACCEPTED.getCode()).equals(locked.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(locked.getPayStatus())
                || !Integer.valueOf(RefundStatusEnum.REFUND_NONE.getCode()).equals(locked.getRefundStatus()))
        {
            throw new ServiceException("订单状态不允许退款", HttpStatus.CONFLICT);
        }

        // 【加锁余额账户 + 回款】悲观锁后单次加回全额。
        CUserBalance balance = cUserBalanceMapper.selectByUserIdForUpdate(locked.getUserId());
        if (balance == null)
        {
            throw new ServiceException("余额账户不存在");
        }
        int rows = cUserBalanceMapper.updateLockedBalance(locked.getUserId(), locked.getTotalAmount());
        if (rows == 0)
        {
            throw new ServiceException("退款失败，请重试");
        }
        int afterBalance = balance.getBalance() + locked.getTotalAmount();

        // 【写余额流水】退款入账，幂等键 orderId:balance:REFUND 唯一。
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

        // 【写退款流水】整单全额退款记录，幂等键 orderId:refund 唯一（每单仅一次）。
        RefundLedger refund = new RefundLedger();
        refund.setOrderId(orderId);
        refund.setUserId(locked.getUserId());
        refund.setAmount(locked.getTotalAmount());
        refund.setIdempotentKey(orderId + ":refund");
        refund.setStatus(1);
        refundLedgerMapper.insert(refund);

        // 【更新订单状态：已接单+已支付+未退款 → 已取消+已退款+退款成功】条件 UPDATE。
        // rows=0 说明并发期间状态已变，抛异常回滚（含已回款、已写流水）。
        int orderRows = bizOrderMapper.updateOrderForRefund(orderId, OrderStatusEnum.CANCELLED.getCode(),
                PayStatusEnum.PAY_REFUNDED.getCode(), RefundStatusEnum.REFUND_SUCCESS.getCode(),
                LocalDateTime.now(), cancelReason);
        if (orderRows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        // 归还库存。
        restoreOrderStock(orderId);
        log.info("退款成功 orderId={} orderNo={} userId={} shopId={} amount={} orderType={} pickupDisplay={} operatorId={}",
                orderId, locked.getOrderNo(), locked.getUserId(), locked.getShopId(),
                locked.getTotalAmount(), locked.getOrderType(), locked.getPickupDisplay(), operatorId);
    }

    /**
     * 归还订单扣减的库存。
     *
     * <p>按 shopProductId 聚合本单明细数量，再按 id 升序逐个归还（固定顺序避免并发死锁）。
     * {@code restoreStock} 内部带幂等键校验，重复归还会被识别并安全跳过。
     */
    private void restoreOrderStock(Long orderId)
    {
        List<BizOrderItem> items = bizOrderItemMapper.selectByOrderId(orderId);
        // 聚合：同一门店商品的数量合并（一单可能含同商品多规格）。
        Map<Long, Integer> restores = new LinkedHashMap<>();
        for (BizOrderItem item : items)
        {
            restores.merge(item.getShopProductId(), item.getQuantity(), Integer::sum);
        }
        // 按 shopProductId 升序归还，与下单扣减顺序一致，降低死锁概率。
        List<Long> sortedIds = new ArrayList<>(restores.keySet());
        Collections.sort(sortedIds);
        for (Long shopProductId : sortedIds)
        {
            productCenterService.restoreStock(orderId, shopProductId, restores.get(shopProductId));
        }
    }
}
