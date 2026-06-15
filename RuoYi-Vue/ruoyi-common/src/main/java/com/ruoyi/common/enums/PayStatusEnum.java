package com.ruoyi.common.enums;

public enum PayStatusEnum
{
    PAY_PENDING(0, "待支付"),
    PAY_SUCCESS(1, "支付成功"),
    PAY_TIMEOUT(2, "支付超时"),
    PAY_REFUNDING(3, "退款处理中"),
    PAY_REFUNDED(4, "已全额退款");

    private final int code;
    private final String desc;

    PayStatusEnum(int code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public int getCode() { return code; }
    public String getDesc() { return desc; }
}
