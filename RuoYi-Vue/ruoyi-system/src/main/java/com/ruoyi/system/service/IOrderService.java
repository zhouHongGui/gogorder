package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderPageView;
import com.ruoyi.system.domain.dto.OrderSubmitRequest;
import com.ruoyi.system.domain.dto.OrderSubmitResponse;

public interface IOrderService
{
    OrderSubmitResponse submitOrder(Long userId, OrderSubmitRequest request);
    void cancelOrder(Long userId, Long orderId);
    OrderPageView getOrderList(Long userId, String dateScope, Integer pageNum, Integer pageSize);
    OrderDetailView getOrderDetail(Long userId, Long orderId);
}
