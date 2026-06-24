package com.ruoyi.system.service;

/**
 * 订单取消与退款服务契约。实现见 {@link com.ruoyi.system.service.impl.OrderCancelServiceImpl}。
 */
public interface IOrderCancelService
{
    /** 用户取消未支付订单（仅还库存）。 */
    void cancelUnpaidOrder(Long userId, Long orderId);
    /** 定时任务超时取消未支付订单（幂等，多实例安全）。 */
    void cancelTimeoutOrder(Long orderId);
    /** 管理端/门店取消已接单订单并整单全额退款（每单仅一次）。
     *  service 同时校验 operatorId 的门店授权和订单归属；cancelReason 必填且最多 200 字。 */
    void cancelPaidOrderWithRefund(Long orderId, Long shopId, String cancelReason, Long operatorId);
}
