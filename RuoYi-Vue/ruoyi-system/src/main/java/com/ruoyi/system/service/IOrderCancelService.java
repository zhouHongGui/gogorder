package com.ruoyi.system.service;

public interface IOrderCancelService
{
    void cancelUnpaidOrder(Long userId, Long orderId);
    void cancelTimeoutOrder(Long orderId);
    void cancelPaidOrderWithRefund(Long orderId, String cancelReason, Long operatorId);
}
