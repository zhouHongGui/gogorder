package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;

/**
 * 下单响应（{@code /api/c/order/submit} 返回）。
 *
 * <p>字段：orderId(订单ID，下一步支付用) / orderNo(订单号) / totalAmount(应付金额，分) /
 * balance(用户当前余额，分，供前端判断是否够付) / createTime(下单时间)。
 */
public class OrderSubmitResponse
{
    /** 订单 ID（下一步支付接口用）。 */
    private Long orderId;
    /** 订单号。 */
    private String orderNo;
    /** 应付金额（分）。 */
    private Integer totalAmount;
    /** 用户当前余额（分，供前端判断是否够付）。 */
    private Integer balance;
    /** 下单时间。 */
    private LocalDateTime createTime;

    public OrderSubmitResponse() {}

    public OrderSubmitResponse(Long orderId, String orderNo, Integer totalAmount, Integer balance, LocalDateTime createTime)
    {
        this.orderId = orderId;
        this.orderNo = orderNo;
        this.totalAmount = totalAmount;
        this.balance = balance;
        this.createTime = createTime;
    }

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Integer getTotalAmount() { return totalAmount; }
    public void setTotalAmount(Integer totalAmount) { this.totalAmount = totalAmount; }
    public Integer getBalance() { return balance; }
    public void setBalance(Integer balance) { this.balance = balance; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
