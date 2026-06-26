package com.ruoyi.quartz.task;

import java.util.List;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.service.impl.ProductionScheduleService;

/**
 * 定时任务：制作调度兜底扫描（串行队列，M15 §6.4）。
 *
 * <p>事件驱动（订单支付成功 / 通知取餐）已能推进队列；本任务兜底，防事件遗漏或预订单到窗口未及时推进。
 * 由若依 Quartz（{@code sys_job}）调度，cron 默认每分钟，调用目标 {@code productionSweepTask.sweep}。
 *
 * <h3>分布式锁</h3>
 * 多实例部署用 Redis SET NX（TTL 5 分钟）保证同一时刻只有一个节点执行，抢不到锁的节点跳过。
 *
 * <h3>逐门店独立事务</h3>
 * 对每个有队列订单的门店调 {@link ProductionScheduleService#sweepShop}（REQUIRES_NEW），
 * 单店失败被 catch 记录，不影响其他门店。
 */
@Component("productionSweepTask")
public class ProductionSweepTask
{
    private static final Logger log = LoggerFactory.getLogger(ProductionSweepTask.class);
    /** 分布式锁 key。 */
    private static final String TASK_LOCK_KEY = "job:production-sweep";

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private ProductionScheduleService productionScheduleService;
    @Autowired private RedisCache redisCache;

    /**
     * 任务入口：抢分布式锁后扫描所有有队列订单的门店，逐个推进制作。
     * 调度器（若依 Quartz）通过反射调用此方法。
     */
    public void sweep()
    {
        String lockValue = IdUtils.fastSimpleUUID();
        if (!redisCache.setCacheObjectIfAbsent(TASK_LOCK_KEY, lockValue, 5, TimeUnit.MINUTES))
        {
            log.info("制作调度扫描任务已有实例执行，本次跳过");
            return;
        }
        try
        {
            List<Long> shopIds = bizOrderMapper.selectShopIdsWithQueue();
            for (Long shopId : shopIds)
            {
                try
                {
                    productionScheduleService.sweepShop(shopId);
                }
                catch (Exception e)
                {
                    // 单店失败不中断整体，记录日志继续。
                    log.warn("制作调度扫描异常 shopId={}", shopId, e);
                }
            }
        }
        finally
        {
            redisCache.releaseLock(TASK_LOCK_KEY, lockValue);
        }
    }
}
