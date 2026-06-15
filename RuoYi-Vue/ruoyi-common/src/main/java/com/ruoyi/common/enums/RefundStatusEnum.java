package com.ruoyi.common.enums;

public enum RefundStatusEnum
{
    REFUND_NONE(0, "无退款"),
    REFUND_PROCESSING(1, "退款处理中"),
    REFUND_SUCCESS(2, "退款成功"),
    REFUND_FAILED(3, "退款失败");

    private final int code;
    private final String desc;

    RefundStatusEnum(int code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
