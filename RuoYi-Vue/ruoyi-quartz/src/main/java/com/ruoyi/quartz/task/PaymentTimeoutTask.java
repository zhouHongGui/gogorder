package com.ruoyi.quartz.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.config.GogorderOrderProperties;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.service.IOrderCancelService;

/**
 * 定时任务：超时未支付订单自动取消 + 归还库存。
 *
 * <h3>调度</h3>
 * 由若依 Quartz（{@code sys_job}）调度，cron 默认每分钟执行一次，调用目标
 * {@code paymentTimeoutTask.cancelTimeoutOrders}。超时阈值见 {@link GogorderOrderProperties#getPayTimeoutMinutes()}（默认 15 分钟）。
 *
 * <h3>分布式锁（接手必读）</h3>
 * 多实例部署时，Quartz 的 {@code concurrent=1} 只能防单 JVM 内并发，无法防跨节点重复执行。
 * 因此用 <b>Redis 分布式锁</b>（{@code SET NX}，TTL 10 分钟）保证同一时刻只有一个节点执行：
 * 抢不到锁的节点直接跳过。释放锁用 owner-checked Lua（见 {@link RedisCache#releaseLock}），防止误删他人锁。
 *
 * <h3>批处理</h3>
 * 用 id 游标分页扫描（{@code id > minId}），避免一次性锁住大量订单；单笔取消失败不影响其他订单。
 */
@Component("paymentTimeoutTask")
public class PaymentTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutTask.class);
    /** 分布式锁 key。 */
    private static final String TASK_LOCK_KEY = "job:payment-timeout";

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private IOrderCancelService orderCancelService;
    @Autowired private RedisCache redisCache;
    @Autowired private GogorderOrderProperties orderProperties;

    /**
     * 任务入口：抢分布式锁后执行批量取消。
     * 调度器（若依 Quartz）通过反射调用此方法。
     */
    public void cancelTimeoutOrders()
    {
        // 抢锁：生成唯一 owner 值，SET NX 抢锁，TTL 10 分钟兜底（防节点宕机锁不释放）。
        String lockValue = IdUtils.fastSimpleUUID();
        if (!redisCache.setCacheObjectIfAbsent(TASK_LOCK_KEY, lockValue, 10, TimeUnit.MINUTES))
        {
            // 其他节点正在执行，本节点跳过。
            log.info("支付超时取消任务已有实例执行，本次跳过");
            return;
        }
        try
        {
            doCancelTimeoutOrders();
        }
        finally
        {
            // finally 释放锁（owner-checked，只删自己的锁）。
            redisCache.releaseLock(TASK_LOCK_KEY, lockValue);
        }
    }

    /**
     * 批量取消超时订单。按 id 游标分页，逐笔调用 {@link IOrderCancelService#cancelTimeoutOrder}。
     * 该方法是 REQUIRES_NEW 事务且幂等（条件 UPDATE），单笔失败被 catch 记录，不影响后续。
     */
    private void doCancelTimeoutOrders()
    {
        // 超时截止时间：现在 - 支付超时分钟数。
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderProperties.getPayTimeoutMinutes());
        long minId = 0L;   // id 游标，从 0 开始
        int totalCancelled = 0;
        while (true)
        {
            // 每批取 timeoutBatchSize 条待支付且超时的订单（id > minId 升序）。
            List<BizOrder> batch = bizOrderMapper.selectPendingTimeoutOrders(
                    deadline, minId, orderProperties.getTimeoutBatchSize());
            if (batch.isEmpty())
            {
                break;
            }
            for (BizOrder order : batch)
            {
                minId = order.getId();   // 推进游标
                try
                {
                    orderCancelService.cancelTimeoutOrder(order.getId());
                    totalCancelled++;
                }
                catch (Exception e)
                {
                    // 单笔失败不中断整体，记录日志继续。
                    log.error("超时取消订单失败，orderId={}", order.getId(), e);
                }
            }
            // 本批不足一批说明已到末尾，结束循环。
            if (batch.size() < orderProperties.getTimeoutBatchSize())
            {
                break;
            }
        }
        if (totalCancelled > 0)
        {
            log.info("支付超时取消完成，共处理{}笔", totalCancelled);
        }
    }
}
