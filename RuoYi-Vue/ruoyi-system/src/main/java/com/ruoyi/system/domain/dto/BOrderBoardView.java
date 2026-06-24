package com.ruoyi.system.domain.dto;

import java.util.List;

/** 门店员工端制作看板聚合视图。 */
public class BOrderBoardView
{
    private List<BOrderCardView> preorders;
    private List<BOrderCardView> pending;
    private List<BOrderCardView> making;
    private List<BOrderCardView> waiting;
    private Integer preordersCount;
    private Integer pendingCount;
    private Integer makingCount;
    private Integer waitingCount;
    private Integer totalActiveCount;

    public List<BOrderCardView> getPreorders() { return preorders; }
    public void setPreorders(List<BOrderCardView> preorders) { this.preorders = preorders; }
    public List<BOrderCardView> getPending() { return pending; }
    public void setPending(List<BOrderCardView> pending) { this.pending = pending; }
    public List<BOrderCardView> getMaking() { return making; }
    public void setMaking(List<BOrderCardView> making) { this.making = making; }
    public List<BOrderCardView> getWaiting() { return waiting; }
    public void setWaiting(List<BOrderCardView> waiting) { this.waiting = waiting; }
    public Integer getPreordersCount() { return preordersCount; }
    public void setPreordersCount(Integer preordersCount) { this.preordersCount = preordersCount; }
    public Integer getPendingCount() { return pendingCount; }
    public void setPendingCount(Integer pendingCount) { this.pendingCount = pendingCount; }
    public Integer getMakingCount() { return makingCount; }
    public void setMakingCount(Integer makingCount) { this.makingCount = makingCount; }
    public Integer getWaitingCount() { return waitingCount; }
    public void setWaitingCount(Integer waitingCount) { this.waitingCount = waitingCount; }
    public Integer getTotalActiveCount() { return totalActiveCount; }
    public void setTotalActiveCount(Integer totalActiveCount) { this.totalActiveCount = totalActiveCount; }
}
