package com.ruoyi.quartz.task;

import java.time.LocalDate;
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
 * 每日兜底归档：凌晨 4 点将昨日及更早 pickup_date 的 1/2/3 订单统一置为已完成。
 */
@Component("dailyFinalizeTask")
public class DailyFinalizeTask
{
    private static final Logger log = LoggerFactory.getLogger(DailyFinalizeTask.class);
    private static final String TASK_LOCK_KEY = "job:daily-finalize";
    private static final int BATCH_SIZE = 200;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private RedisCache redisCache;

    public void finalizePreviousDayOrders()
    {
        String lockValue = IdUtils.fastSimpleUUID();
        if (!redisCache.setCacheObjectIfAbsent(TASK_LOCK_KEY, lockValue, 30, TimeUnit.MINUTES))
        {
            log.info("每日订单兜底归档任务已有实例执行，本次跳过");
            return;
        }
        try
        {
            doFinalizePreviousDayOrders();
        }
        finally
        {
            redisCache.releaseLock(TASK_LOCK_KEY, lockValue);
        }
    }

    private void doFinalizePreviousDayOrders()
    {
        LocalDate cutoffPickupDate = LocalDate.now();
        long minId = 0L;
        int total = 0;
        while (true)
        {
            List<BizOrder> batch = bizOrderMapper.selectDailyFinalizeOrders(cutoffPickupDate, minId, BATCH_SIZE);
            if (batch.isEmpty())
            {
                break;
            }
            for (BizOrder order : batch)
            {
                minId = order.getId();
                try
                {
                    int rows = bizOrderMapper.updateDailyFinalizeOrder(order.getId(), LocalDateTime.now());
                    if (rows > 0)
                    {
                        total++;
                        log.info("每日兜底归档订单 orderId={} orderNo={} shopId={} fromStatus={}",
                                order.getId(), order.getOrderNo(), order.getShopId(), order.getOrderStatus());
                    }
                }
                catch (Exception e)
                {
                    log.error("每日兜底归档订单失败 orderId={}", order.getId(), e);
                }
            }
            if (batch.size() < BATCH_SIZE)
            {
                break;
            }
        }
        if (total > 0)
        {
            log.info("每日订单兜底归档结束，共处理{}笔", total);
        }
    }
}
