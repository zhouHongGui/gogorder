package com.ruoyi.quartz.task;

import java.time.LocalDateTime;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.service.IOrderCancelService;

@Component("paymentTimeoutTask")
public class PaymentTimeoutTask
{
    private static final Logger log = LoggerFactory.getLogger(PaymentTimeoutTask.class);
    private static final int BATCH_SIZE = 100;

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private IOrderCancelService orderCancelService;

    public void cancelTimeoutOrders()
    {
        LocalDateTime deadline = LocalDateTime.now().minusMinutes(15);
        long minId = 0L;
        int totalCancelled = 0;
        while (true)
        {
            List<BizOrder> batch = bizOrderMapper.selectPendingTimeoutOrders(deadline, minId, BATCH_SIZE);
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
            if (batch.size() < BATCH_SIZE)
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
