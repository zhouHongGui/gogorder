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

@Component("paymentTimeoutTask")
public class PaymentTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutTask.class);
    private static final String TASK_LOCK_KEY = "job:payment-timeout";

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private IOrderCancelService orderCancelService;
    @Autowired private RedisCache redisCache;
    @Autowired private GogorderOrderProperties orderProperties;

    public void cancelTimeoutOrders()
    {
        String lockValue = IdUtils.fastSimpleUUID();
        if (!redisCache.setCacheObjectIfAbsent(TASK_LOCK_KEY, lockValue, 10, TimeUnit.MINUTES))
        {
            log.info("支付超时取消任务已有实例执行，本次跳过");
            return;
        }
        try
        {
            doCancelTimeoutOrders();
        }
        finally
        {
            redisCache.releaseLock(TASK_LOCK_KEY, lockValue);
        }
    }

    private void doCancelTimeoutOrders()
    {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(orderProperties.getPayTimeoutMinutes());
        long minId = 0L;
        int totalCancelled = 0;
        while (true)
        {
            List<BizOrder> batch = bizOrderMapper.selectPendingTimeoutOrders(
                    deadline, minId, orderProperties.getTimeoutBatchSize());
            if (batch.isEmpty())
            {
                break;
            }
            for (BizOrder order : batch)
            {
                minId = order.getId();
                try
                {
                    orderCancelService.cancelTimeoutOrder(order.getId());
                    totalCancelled++;
                }
                catch (Exception e)
                {
                    log.error("超时取消订单失败，orderId={}", order.getId(), e);
                }
            }
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
