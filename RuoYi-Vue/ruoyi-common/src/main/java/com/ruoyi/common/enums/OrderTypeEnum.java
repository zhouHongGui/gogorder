package com.ruoyi.common.enums;

public enum OrderTypeEnum
{
    NORMAL("NORMAL", "即时单"),
    PREORDER("PREORDER", "预订单");

    private final String code;
    private final String desc;

    OrderTypeEnum(String code, String desc)
    {
        this.code = code;
        this.desc = desc;
    }

    public String getCode() { return code; }
    public String getDesc() { return desc; }
}
