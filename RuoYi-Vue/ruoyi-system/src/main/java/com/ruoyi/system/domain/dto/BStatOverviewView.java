package com.ruoyi.system.domain.dto;

import java.time.LocalDate;
import java.util.List;

/**
 * Staff-side shop business statistics overview.
 */
public class BStatOverviewView
{
    private String range;
    private String rangeDesc;
    private LocalDate startDate;
    private LocalDate endDate;
    private Long turnoverAmount;
    private Long orderCount;
    private Long cupCount;
    private Long avgOrderAmount;
    private List<BStatStatusCountView> statusCounts;
    private List<BStatProductRankView> productRanks;

    public String getRange() { return range; }
    public void setRange(String range) { this.range = range; }
    public String getRangeDesc() { return rangeDesc; }
    public void setRangeDesc(String rangeDesc) { this.rangeDesc = rangeDesc; }
    public LocalDate getStartDate() { return startDate; }
    public void setStartDate(LocalDate startDate) { this.startDate = startDate; }
    public LocalDate getEndDate() { return endDate; }
    public void setEndDate(LocalDate endDate) { this.endDate = endDate; }
    public Long getTurnoverAmount() { return turnoverAmount; }
    public void setTurnoverAmount(Long turnoverAmount) { this.turnoverAmount = turnoverAmount; }
    public Long getOrderCount() { return orderCount; }
    public void setOrderCount(Long orderCount) { this.orderCount = orderCount; }
    public Long getCupCount() { return cupCount; }
    public void setCupCount(Long cupCount) { this.cupCount = cupCount; }
    public Long getAvgOrderAmount() { return avgOrderAmount; }
    public void setAvgOrderAmount(Long avgOrderAmount) { this.avgOrderAmount = avgOrderAmount; }
    public List<BStatStatusCountView> getStatusCounts() { return statusCounts; }
    public void setStatusCounts(List<BStatStatusCountView> statusCounts) { this.statusCounts = statusCounts; }
    public List<BStatProductRankView> getProductRanks() { return productRanks; }
    public void setProductRanks(List<BStatProductRankView> productRanks) { this.productRanks = productRanks; }
}
