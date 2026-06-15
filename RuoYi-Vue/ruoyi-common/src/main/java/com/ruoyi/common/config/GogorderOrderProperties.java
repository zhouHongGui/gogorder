package com.ruoyi.common.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "gogorder.order")
public class GogorderOrderProperties
{
    private int payTimeoutMinutes = 15;
    private int maxPendingOrdersPerUser = 3;
    private int timeoutBatchSize = 100;

    public int getPayTimeoutMinutes() { return payTimeoutMinutes; }
    public void setPayTimeoutMinutes(int payTimeoutMinutes) { this.payTimeoutMinutes = payTimeoutMinutes; }
    public int getMaxPendingOrdersPerUser() { return maxPendingOrdersPerUser; }
    public void setMaxPendingOrdersPerUser(int maxPendingOrdersPerUser) { this.maxPendingOrdersPerUser = maxPendingOrdersPerUser; }
    public int getTimeoutBatchSize() { return timeoutBatchSize; }
    public void setTimeoutBatchSize(int timeoutBatchSize) { this.timeoutBatchSize = timeoutBatchSize; }
}
