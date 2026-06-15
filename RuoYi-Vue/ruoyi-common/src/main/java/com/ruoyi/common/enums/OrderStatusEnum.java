package com.ruoyi.common.enums;

public enum OrderStatusEnum
{
    PENDING_PAY(0, "待支付"),
    ACCEPTED(1, "已接单"),
    MAKING(2, "制作中"),
    READY(3, "待取餐"),
    COMPLETED(4, "已完成"),
    CANCELLED(5, "已取消");

    private final int code;
    private final String desc;

    OrderStatusEnum(int code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
