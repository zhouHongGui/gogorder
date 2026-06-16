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

/**
 * 支付事务服务（余额支付）。
 *
 * <p>V1.0 仅支持「余额支付」：直接扣减 {@code c_user_balance.balance}，无第三方支付、无支付密码。
 * 由 {@link BalanceServiceImpl#pay} 调用本类的 {@link #payAttempt}，后者是真正的事务边界。
 *
 * <h3>事务与并发模型（接手必读）</h3>
 * <ul>
 *   <li><b>事务传播</b>：{@link Propagation#REQUIRES_NEW}。每个支付尝试开独立事务，
 *       目的是配合 {@link BalanceServiceImpl} 在「取餐码 token 碰撞」时能回滚后重试（每次重试新事务）。</li>
 *   <li><b>并发支付防重</b>：入口 {@code selectByIdForUpdate} 给订单行加悲观锁，
 *       同一订单的并发支付请求在此串行化；先到者完成支付，后到者拿到锁后看到 pay_status 已成功，走幂等返回。
 *       因此不会重复扣款。</li>
 *   <li><b>幂等键</b>：{@code payment_ledger.idempotent_key = orderId:pay}（唯一），
 *       {@code balance_ledger.idempotent_key = orderId:balance:PAY}（唯一）。</li>
 *   <li><b>余额扣减</b>：悲观锁行（{@code selectByUserIdForUpdate}）+ 单次 {@code updateLockedBalance}，
 *       不再使用乐观锁 version 重试（避免「悲观+乐观」双重锁的自相矛盾）。</li>
 *   <li><b>取餐号分配顺序</b>：先扣余额（确定能成交），再分配取餐展示号；余额不足时事务回滚，
 *       不会产生取餐号空洞、也不会无效占用取餐号行锁。</li>
 *   <li><b>支付成功 = 自动接单</b>：订单状态 order_status 直接 0(待支付)→1(已接单)，无人工接单环节。</li>
 * </ul>
 *
 * <h3>取餐号格式</h3>
 * 展示号 {@code pickup_display} 为「字母+3位数字」（A001~A999, B001~B999, …, Z999），
 * 按门店+日期递增（见 {@code pickup_sequence} 表）；另存 {@code pickup_token}（12位全局随机）用于扫码核销。
 *
 * @see BalanceServiceImpl 外层重试编排（token 碰撞重试）
 * @see OrderCancelServiceImpl 取消退款（反向操作）
 */
@Service
public class PaymentTransactionService
{
    private static final Logger log = LoggerFactory.getLogger(PaymentTransactionService.class);
    /** 取餐展示号每组大小：A001~A999 为一组（999 个），溢出后字母进位。 */
    private static final int PICKUP_DISPLAY_GROUP_SIZE = 999;
    /** 取餐展示号上限：26 个字母 × 999 = 25974，超出报「当日取餐号已满」。 */
    private static final int PICKUP_DISPLAY_MAX_SEQUENCE = 26 * PICKUP_DISPLAY_GROUP_SIZE;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private CUserBalanceMapper cUserBalanceMapper;
    @Autowired private BalanceLedgerMapper balanceLedgerMapper;
    @Autowired private PaymentLedgerMapper paymentLedgerMapper;
    /** 订单可调阈值（支付超时分钟数等），见 {@link GogorderOrderProperties}。 */
    @Autowired private GogorderOrderProperties orderProperties;

    /**
     * 单次支付尝试（事务边界）。
     *
     * <p>步骤：加锁订单 → 幂等/状态校验 → 超时校验 → 加锁余额账户 → 扣款 → 分配取餐号 →
     * 写余额流水/支付流水 → 更新订单状态为已接单+已支付。
     *
     * @param userId      当前用户
     * @param orderId     待支付订单
     * @param pickupToken 由 {@link BalanceServiceImpl} 生成的取餐核销令牌（12位随机），支付成功时写入订单
     * @return 支付结果（订单号、取餐展示号、扣款后余额）
     * @throws ServiceException 订单不存在/越权、状态不允许、已超时、余额不足、状态已变更 等
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public PayResponse payAttempt(Long userId, Long orderId, String pickupToken)
    {
        // 【加锁订单】悲观锁，串行化同一订单的并发支付，是防重复扣款的核心。
        BizOrder locked = bizOrderMapper.selectByIdForUpdate(orderId);
        if (locked == null || !Objects.equals(locked.getUserId(), userId))
        {
            throw new ServiceException("订单不存在", HttpStatus.NOT_FOUND);
        }
        // 【幂等：已支付则直接返回】并发支付时，后到者拿到锁后走这里，避免重复扣款。
        if (Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(locked.getPayStatus()))
        {
            // 校验支付流水一致性：已支付订单必须能查到 payment_ledger，缺失说明数据异常。
            PaymentLedger ledger = paymentLedgerMapper.selectByIdempotentKey(orderId + ":pay");
            if (ledger == null)
            {
                log.error("支付数据不一致: orderId={} pay_status=SUCCESS 但 payment_ledger 缺失", orderId);
                throw new ServiceException("订单支付数据异常，请联系管理员");
            }
            CUserBalance current = cUserBalanceMapper.selectByUserId(userId);
            return new PayResponse(locked.getOrderNo(), locked.getPickupDisplay(), current == null ? 0 : current.getBalance());
        }
        // 【状态前置校验】只有「待支付」订单才能支付（order_status=0 且 pay_status=0）。
        if (!Integer.valueOf(OrderStatusEnum.PENDING_PAY.getCode()).equals(locked.getOrderStatus())
                || !Integer.valueOf(PayStatusEnum.PAY_PENDING.getCode()).equals(locked.getPayStatus()))
        {
            throw new ServiceException("订单状态不允许支付", HttpStatus.CONFLICT);
        }
        // 【超时校验】下单超过 payTimeoutMinutes(默认15分钟) 不可支付。配合定时任务自动取消。
        if (locked.getCreateTime() != null && locked.getCreateTime().isBefore(
                LocalDateTime.now().minusMinutes(orderProperties.getPayTimeoutMinutes())))
        {
            throw new ServiceException("订单已超时，请重新下单", HttpStatus.BAD_REQUEST);
        }

        // 【确定取餐号归属日期】即时单=今天；预订单=预约取餐日（用 pickup_date 防跨天碰撞）。
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
        // 【加锁余额账户】悲观锁，扣减期间余额行不可被其他事务改动。
        CUserBalance balance = cUserBalanceMapper.selectByUserIdForUpdate(userId);
        if (balance == null)
        {
            throw new ServiceException("余额账户不存在");
        }
        // 【余额校验】不足则抛异常，并附带 {balance, required} 数据，前端展示「余额 X，需支付 Y」。
        if (balance.getBalance() < locked.getTotalAmount())
        {
            log.warn("余额不足 orderId={} userId={} balance={} required={}",
                    orderId, userId, balance.getBalance(), locked.getTotalAmount());
            throw new ServiceException("余额不足", HttpStatus.BAD_REQUEST).setData(
                    Map.of("balance", balance.getBalance(), "required", locked.getTotalAmount()));
        }

        // 【扣减余额】单次 UPDATE（悲观锁已保证无并发竞争）。rows=0 视为异常（理论上不应发生）。
        int rows = cUserBalanceMapper.updateLockedBalance(userId, -locked.getTotalAmount());
        if (rows == 0)
        {
            throw new ServiceException("余额扣减失败，请重试");
        }
        // 扣款后余额（供流水与返回值使用）。
        int afterBalance = balance.getBalance() - locked.getTotalAmount();

        // 【分配取餐展示号】INSERT...ON DUPLICATE KEY UPDATE 自增 pickup_sequence 计数，再读出当前序号。
        // 放在余额扣减之后：余额不足会先回滚，避免浪费取餐号与无谓持锁。
        bizOrderMapper.allocatePickupDisplay(locked.getShopId(), pickupDate);
        int sequence = bizOrderMapper.selectPickupSeq(locked.getShopId(), pickupDate);
        String pickupDisplay = formatPickupDisplay(sequence);

        // 【写余额流水】记录扣款前后余额，幂等键 orderId:balance:PAY 唯一，防重复记账。
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

        // 【写支付流水】记录这笔支付，幂等键 orderId:pay 唯一。
        PaymentLedger paymentLedger = new PaymentLedger();
        paymentLedger.setOrderId(orderId);
        paymentLedger.setUserId(userId);
        paymentLedger.setAmount(locked.getTotalAmount());
        paymentLedger.setIdempotentKey(orderId + ":pay");
        paymentLedger.setStatus(1);
        paymentLedgerMapper.insert(paymentLedger);

        // 【更新订单状态：待支付 → 已接单+已支付】条件 UPDATE（要求 order_status=0 且 pay_status=0），
        // rows=0 说明并发期间状态被改（如已被取消），抛异常回滚本事务（含已扣余额、已写流水）。
        LocalDateTime now = LocalDateTime.now();
        rows = bizOrderMapper.updateOrderStatus(orderId, OrderStatusEnum.ACCEPTED.getCode(),
                PayStatusEnum.PAY_SUCCESS.getCode(), RefundStatusEnum.REFUND_NONE.getCode(),
                now, now, pickupToken, pickupDisplay, pickupDate);
        if (rows == 0)
        {
            throw new ServiceException("订单状态已变更", HttpStatus.CONFLICT);
        }
        log.info("支付成功 orderId={} orderNo={} userId={} shopId={} amount={} orderType={} pickupDisplay={}",
                orderId, locked.getOrderNo(), userId, locked.getShopId(),
                locked.getTotalAmount(), locked.getOrderType(), pickupDisplay);
        return new PayResponse(locked.getOrderNo(), pickupDisplay, afterBalance);
    }

    /**
     * 把自增序号格式化为取餐展示号（A001~A999, B001~B999, …, Z999）。
     *
     * @param sequence pickup_sequence.current_seq，从 1 开始
     * @throws ServiceException 序号越界（≤0 或超过 25974）
     */
    private String formatPickupDisplay(int sequence)
    {
        if (sequence <= 0 || sequence > PICKUP_DISPLAY_MAX_SEQUENCE)
        {
            throw new ServiceException("当日取餐号已满，请联系门店");
        }
        int zeroBased = sequence - 1;                                  // 转为 0 基，便于整除取字母
        char prefix = (char) ('A' + zeroBased / PICKUP_DISPLAY_GROUP_SIZE);  // 每 999 个换一个字母
        int number = zeroBased % PICKUP_DISPLAY_GROUP_SIZE + 1;        // 组内序号 1~999
        return String.format("%c%03d", prefix, number);               // 字母 + 3 位数字
    }
}
