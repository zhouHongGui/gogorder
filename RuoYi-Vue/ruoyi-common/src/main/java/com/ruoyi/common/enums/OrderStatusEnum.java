package com.ruoyi.common.enums;

/**
 * 订单状态枚举（biz_order.order_status）。
 *
 * <h3>状态流转（接手必读）</h3>
 * <pre>
 *   下单          支付成功        自动串行推进    扫码出餐       完成/自动归档
 *   PENDING_PAY → ACCEPTED → MAKING → READY → COMPLETED
 *      ↓(用户取消/超时)
 *   CANCELLED
 * </pre>
 * 关键规则：支付成功即自动接单（无人工接单）；用户仅可取消「待支付」订单，
 * 支付后的取消走管理端退款（状态直接到 CANCELLED）。
 */
public enum OrderStatusEnum
{
    /** 待支付（下单后初始态，15 分钟超时自动取消）。 */
    PENDING_PAY(0, "待支付"),
    /** 已接单（支付成功即进入，无人工接单环节）。 */
    ACCEPTED(1, "已接单"),
    /** 制作中（门店开始制作，此态后不可退款）。 */
    MAKING(2, "制作中"),
    /** 待取餐（扫码出餐后，等用户取餐或自动归档）。 */
    READY(3, "待取餐"),
    /** 已完成（店员完成、待取餐4小时超时或凌晨兜底归档后的终态）。 */
    COMPLETED(4, "已完成"),
    /** 已取消（用户取消未支付订单/超时取消/退款取消）。 */
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
