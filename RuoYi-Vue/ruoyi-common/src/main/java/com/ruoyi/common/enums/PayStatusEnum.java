package com.ruoyi.common.enums;

/**
 * 支付状态枚举（biz_order.pay_status）。
 *
 * <p>与 {@link OrderStatusEnum} 独立维护：PENDING_PAY→SUCCESS（支付成功）或→TIMEOUT（超时自动取消）；
 * 退款时 SUCCESS→REFUNDED。V1.0 余额支付即时完成，REFUNDING 中间态基本不出现。
 * <p><b>注意</b>：业务代码里实际用到的是 PAY_PENDING(0)/PAY_SUCCESS(1)，
 * 而 {@link OrderCancelServiceImpl} 取消未支付时写回 PAY_PENDING、超时取消写 PAY_TIMEOUT、退款写 PAY_REFUNDED。
 */
public enum PayStatusEnum
{
    /** 待支付。 */
    PAY_PENDING(0, "待支付"),
    /** 支付成功（余额已扣）。 */
    PAY_SUCCESS(1, "支付成功"),
    /** 支付超时（定时任务自动取消时标记）。 */
    PAY_TIMEOUT(2, "支付超时"),
    /** 退款处理中（V1.0 余额退款即时完成，基本不出现）。 */
    PAY_REFUNDING(3, "退款处理中"),
    /** 已全额退款。 */
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
