package com.ruoyi.quartz.task;

import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.mapper.BizOrderMapper;

/**
 * 待取餐超时自动完成：READY(3) 超过 4 小时未手动完成，则归档为 COMPLETED(4)。
 */
@Component("pickupTimeoutTask")
public class PickupTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(PickupTimeoutTask.class);
    private static final String TASK_LOCK_KEY = "job:pickup-timeout";
    private static final int BATCH_SIZE = 200;
    private static final int READY_TIMEOUT_HOURS = 4;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private RedisCache redisCache;

    public void completeTimeoutReadyOrders()
    {
        String lockValue = IdUtils.fastSimpleUUID();
        if (!redisCache.setCacheObjectIfAbsent(TASK_LOCK_KEY, lockValue, 10, TimeUnit.MINUTES))
        {
            log.info("待取餐超时完成任务已有实例执行，本次跳过");
            return;
        }
        try
        {
            doCompleteTimeoutReadyOrders();
        }
        finally
        {
            redisCache.releaseLock(TASK_LOCK_KEY, lockValue);
        }
    }

    private void doCompleteTimeoutReadyOrders()
    {
        LocalDateTime deadline = LocalDateTime.now().minusHours(READY_TIMEOUT_HOURS);
        long minId = 0L;
        int total = 0;
        while (true)
        {
            List<BizOrder> batch = bizOrderMapper.selectReadyTimeoutOrders(deadline, minId, BATCH_SIZE);
            if (batch.isEmpty())
            {
                break;
            }
            for (BizOrder order : batch)
            {
                minId = order.getId();
                try
                {
                    int rows = bizOrderMapper.updateCompleteOrder(order.getId(), order.getShopId(), LocalDateTime.now());
                    if (rows > 0)
                    {
                        total++;
                        log.info("待取餐超时自动完成 orderId={} orderNo={} shopId={}",
                                order.getId(), order.getOrderNo(), order.getShopId());
                    }
                }
                catch (Exception e)
                {
                    log.error("待取餐超时自动完成失败 orderId={}", order.getId(), e);
                }
            }
            if (batch.size() < BATCH_SIZE)
            {
                break;
            }
        }
        if (total > 0)
        {
            log.info("待取餐超时自动完成结束，共处理{}笔", total);
        }
    }
}
