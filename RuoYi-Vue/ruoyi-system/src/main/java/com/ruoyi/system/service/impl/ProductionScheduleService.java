package com.ruoyi.system.service.impl;

import java.time.LocalDateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.ShopMapper;

/**
 * 制作调度服务（串行队列）：按门店一次一单自动推进订单进入制作。
 *
 * <h3>调度模型</h3>
 * <ul>
 *   <li>串行制作：同店同一时间最多一单处于制作中（{@code order_status=2}）。</li>
 *   <li>制作中无单时，队列（{@code order_status=1}）最早单自动推进 {@code 1→2}。</li>
 *   <li>预订单需到制作窗口（{@code scheduled_pickup_time - make_lead_minutes <= now}）才进队列。</li>
 * </ul>
 *
 * <h3>触发时机</h3>
 * <ul>
 *   <li>订单支付成功 → {@link #onOrderPaid}：算预计取餐时间 + 若无制作中订单则推进。</li>
 *   <li>订单通知取餐（{@code 2→3}）→ {@link #onOrderScannedOut}：释放串行制作位，推进队列下一个。</li>
 *   <li>兜底：定时任务（{@code ProductionSweepTask}）每分钟扫描，防事件遗漏。</li>
 * </ul>
 *
 * <p><b>不可自动</b>：通知取餐（{@code 2→3}）必须门店人工确认（物理完成唯一信号），本服务不负责。
 *
 * <p>事务用 {@link Propagation#REQUIRES_NEW}，与支付/完成主事务隔离，调度失败不影响主流程。
 *
 * @see PaymentTransactionService 支付成功调 onOrderPaid
 * @see BOrderServiceImpl#notifyPickup 通知取餐后调 onOrderScannedOut
 */
@Service
public class ProductionScheduleService
{
    private static final Logger log = LoggerFactory.getLogger(ProductionScheduleService.class);

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private ShopMapper shopMapper;

    /**
     * 订单支付成功后：算预计取餐时间 + 若无制作中订单则推进制作。
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void onOrderPaid(Long orderId)
    {
        BizOrder order = bizOrderMapper.selectById(orderId);
        if (order == null)
        {
            return;
        }
        Shop shop = shopMapper.selectShopById(order.getShopId());
        if (shop == null)
        {
            return;
        }
        int minutesPerCup = nvl(shop.getMinutesPerCup(), 3);
        // 算预计取餐时间并写入
        LocalDateTime estimated = calculateEstimated(order, minutesPerCup);
        bizOrderMapper.updateEstimatedReadyTime(orderId, estimated);
        // 串行推进（预订单未到窗口则不推进，等定时任务到窗口再推）
        tryPromoteNext(order.getShopId(), nvl(shop.getMakeLeadMinutes(), 30));
    }

    /**
     * 订单通知取餐（{@code 2→3}）后：释放串行制作位，推进队列下一个进入制作。
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void onOrderScannedOut(Long shopId)
    {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null)
        {
            return;
        }
        tryPromoteNext(shopId, nvl(shop.getMakeLeadMinutes(), 30));
    }

    /**
     * 兜底扫描某门店：推进队列（供定时任务调用，每个门店独立事务，互不影响）。
     */
    @Transactional(rollbackFor = Exception.class, propagation = Propagation.REQUIRES_NEW)
    public void sweepShop(Long shopId)
    {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null)
        {
            return;
        }
        tryPromoteNext(shopId, nvl(shop.getMakeLeadMinutes(), 30));
    }

    /**
     * 串行队列：制作中订单数为 0 时，推进队列最早单 {@code 1→2}。
     */
    private void tryPromoteNext(Long shopId, int makeLeadMinutes)
    {
        // 门店级锁：锁 shop 行串行化同店调度，防止并发回调同时推进多单。
        if (shopMapper.selectShopForUpdate(shopId) == null)
        {
            return;
        }
        if (bizOrderMapper.countMakingOrders(shopId) > 0)
        {
            return;
        }
        BizOrder next = bizOrderMapper.selectNextQueuedForUpdate(shopId, makeLeadMinutes);
        if (next == null)
        {
            return;
        }
        int rows = bizOrderMapper.updateStartMake(next.getId(), shopId, LocalDateTime.now());
        if (rows > 0)
        {
            log.info("自动串行推进制作 orderId={} orderNo={} shopId={}", next.getId(), next.getOrderNo(), shopId);
        }
    }

    /**
     * 预计取餐时间 = 支付时间 + (前面排队杯数 + 本单杯数) × 单杯时间。
     */
    private LocalDateTime calculateEstimated(BizOrder order, int minutesPerCup)
    {
        // 预订单：预计取餐时间 = 预约取餐时间（按预约时间制作/取餐，不按支付时间算排队）
        if (OrderTypeEnum.PREORDER.getCode().equals(order.getOrderType())
                && order.getScheduledPickupTime() != null)
        {
            return order.getScheduledPickupTime();
        }
        Integer ahead = bizOrderMapper.countQueueAheadCups(order.getId());
        int cupsAhead = nvl(ahead, 0);
        int myCups = bizOrderMapper.sumOrderCups(order.getId());
        int totalCups = cupsAhead + myCups;
        int minutes = totalCups * minutesPerCup;
        LocalDateTime base = order.getPayTime() != null ? order.getPayTime() : LocalDateTime.now();
        return base.plusMinutes(minutes);
    }

    private static int nvl(Integer v, int def)
    {
        return v == null || v < 1 ? def : v;
    }
}
