package com.ruoyi.system.domain.dto;

public class PayResponse
{
    private String orderNo;
    private String pickupDisplay;
    private Integer balanceAfter;

    public PayResponse() {}

    public PayResponse(String orderNo, String pickupDisplay, Integer balanceAfter)
    {
        this.orderNo = orderNo;
        this.pickupDisplay = pickupDisplay;
        this.balanceAfter = balanceAfter;
    }

    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public String getPickupDisplay() { return pickupDisplay; }
    public void setPickupDisplay(String pickupDisplay) { this.pickupDisplay = pickupDisplay; }
    public Integer getBalanceAfter() { return balanceAfter; }
    public void setBalanceAfter(Integer balanceAfter) { this.balanceAfter = balanceAfter; }
}
