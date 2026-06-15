package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;

public class OrderSubmitResponse
{
    private Long orderId;
    private String orderNo;
    private Integer totalAmount;
    private Integer balance;
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
