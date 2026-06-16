package com.ruoyi.common.enums;

/**
 * 余额变动类型枚举（balance_ledger.type，字符串码）。
 *
 * <p>对应余额流水的方向：PAY 为负、RECHARGE/REFUND 为正、ADJUST 视情况。
 * V1.0 仅管理后台可充值（C 端无充值入口）。
 */
public enum BalanceChangeTypeEnum
{
    /** 充值（后台手动，amount 为正）。 */
    RECHARGE("RECHARGE", "充值"),
    /** 支付扣款（下单支付，amount 为负）。 */
    PAY("PAY", "支付扣款"),
    /** 退款入账（取消退款，amount 为正）。 */
    REFUND("REFUND", "退款入账"),
    /** 后台调账（人工修正）。 */
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
