package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * 订单模块可调阈值（配置前缀 {@code gogorder.order}）。
 *
 * <p>把原本散落在代码里的硬编码阈值集中到这里，便于运营调整且不漏改。
 * 在 {@code application.yml} 中用 {@code gogorder.order.xxx} 覆盖默认值。
 *
 * <ul>
 *   <li>{@code pay-timeout-minutes}：支付超时分钟数。{@link com.ruoyi.system.service.impl.PaymentTransactionService}
 *       拒绝支付与 {@link com.ruoyi.quartz.task.PaymentTimeoutTask} 自动取消都用此值，保证两者一致。</li>
 *   <li>{@code max-pending-orders-per-user}：单用户「待支付」订单上限，防恶意下单占库存。</li>
 *   <li>{@code timeout-batch-size}：超时取消任务每批扫描数量（游标分页）。</li>
 * </ul>
 */
@Component
@ConfigurationProperties(prefix = "gogorder.order")
public class GogorderOrderProperties
{
    /** 支付超时分钟数（默认 15）。超过则不可支付，并由定时任务自动取消。 */
    private int payTimeoutMinutes = 15;
    /** 单用户待支付订单上限（默认 3）。超出则拒绝下单，防止占库存攻击。 */
    private int maxPendingOrdersPerUser = 3;
    /** 超时取消任务每批扫描数量（默认 100）。 */
    private int timeoutBatchSize = 100;

    public int getPayTimeoutMinutes() { return payTimeoutMinutes; }
    public void setPayTimeoutMinutes(int payTimeoutMinutes) { this.payTimeoutMinutes = payTimeoutMinutes; }
    public int getMaxPendingOrdersPerUser() { return maxPendingOrdersPerUser; }
    public void setMaxPendingOrdersPerUser(int maxPendingOrdersPerUser) { this.maxPendingOrdersPerUser = maxPendingOrdersPerUser; }
    public int getTimeoutBatchSize() { return timeoutBatchSize; }
    public void setTimeoutBatchSize(int timeoutBatchSize) { this.timeoutBatchSize = timeoutBatchSize; }
}
