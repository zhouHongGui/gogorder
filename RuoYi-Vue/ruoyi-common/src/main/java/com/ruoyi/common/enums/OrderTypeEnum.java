package com.ruoyi.common.enums;

/**
 * 订单类型枚举（biz_order.order_type，字符串码）。
 *
 * <p>NORMAL=即时单（需门店营业中，支付后立即制作）；
 * PREORDER=预订单（选预约取餐时间，休息/暂停门店也可下）。
 */
public enum OrderTypeEnum
{
    /** 即时单：需门店 status=1 且在营业时段。 */
    NORMAL("NORMAL", "即时单"),
    /** 预订单：选择预约取餐时间，需落在门店可预约窗口内。 */
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
