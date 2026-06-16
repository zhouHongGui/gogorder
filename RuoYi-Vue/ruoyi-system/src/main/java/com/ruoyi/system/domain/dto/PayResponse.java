package com.ruoyi.system.domain.dto;

/**
 * 支付响应（{@code /api/c/order/pay/{orderId}} 返回）。
 * 余额支付成功后返回，供前端跳转取餐结果页展示。
 */
public class PayResponse
{
    /** 订单号。 */
    private String orderNo;
    /** 取餐展示号（字母+3位数字，门店叫号用）。 */
    private String pickupDisplay;
    /** 扣款后余额（分）。 */
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
