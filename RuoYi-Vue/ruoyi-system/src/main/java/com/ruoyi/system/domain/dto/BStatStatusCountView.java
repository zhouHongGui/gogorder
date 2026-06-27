package com.ruoyi.system.domain.dto;

/**
 * Order status count item for staff-side shop statistics.
 */
public class BStatStatusCountView
{
    private Integer orderStatus;
    private String orderStatusDesc;
    private Long count;

    public BStatStatusCountView() {}

    public BStatStatusCountView(Integer orderStatus, String orderStatusDesc, Long count)
    {
        this.orderStatus = orderStatus;
        this.orderStatusDesc = orderStatusDesc;
        this.count = count;
    }

    public Integer getOrderStatus() { return orderStatus; }
    public void setOrderStatus(Integer orderStatus) { this.orderStatus = orderStatus; }
    public String getOrderStatusDesc() { return orderStatusDesc; }
    public void setOrderStatusDesc(String orderStatusDesc) { this.orderStatusDesc = orderStatusDesc; }
    public Long getCount() { return count; }
    public void setCount(Long count) { this.count = count; }
}
