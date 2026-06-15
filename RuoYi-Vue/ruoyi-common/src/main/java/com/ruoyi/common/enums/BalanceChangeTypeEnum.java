package com.ruoyi.common.enums;

public enum BalanceChangeTypeEnum
{
    RECHARGE("RECHARGE", "充值"),
    PAY("PAY", "支付扣款"),
    REFUND("REFUND", "退款入账"),
    ADJUST("ADJUST", "后台调账");

    private final String code;
    private final String desc;

    BalanceChangeTypeEnum(String code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
