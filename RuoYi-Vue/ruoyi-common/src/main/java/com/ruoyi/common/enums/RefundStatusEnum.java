package com.ruoyi.common.enums;

/**
 * 退款状态枚举（biz_order.refund_status）。
 *
 * <p>V1.0 每单严格一次整单全额退款，由 {@link OrderCancelServiceImpl#cancelPaidOrderWithRefund} 处理：
 * NONE→SUCCESS（退款成功）。PROCESSING/FAILED 中间态在 V1.0 即时退款下基本不出现。
 */
public enum RefundStatusEnum
{
    /** 无退款（初始态）。 */
    REFUND_NONE(0, "无退款"),
    /** 退款处理中。 */
    REFUND_PROCESSING(1, "退款处理中"),
    /** 退款成功（每单仅一次）。 */
    REFUND_SUCCESS(2, "退款成功"),
    /** 退款失败。 */
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
