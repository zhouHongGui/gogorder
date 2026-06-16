package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderPageView;
import com.ruoyi.system.domain.dto.OrderSubmitRequest;
import com.ruoyi.system.domain.dto.OrderSubmitResponse;

/**
 * 订单服务契约（下单/取消/查询）。实现见 {@link com.ruoyi.system.service.impl.OrderServiceImpl}，
 * 并发/幂等/事务模型等细节在实现类 Javadoc 中说明。
 */
public interface IOrderService
{
    /** 提交订单（幂等：同 submitToken 返回同一订单）。 */
    OrderSubmitResponse submitOrder(Long userId, OrderSubmitRequest request);
    /** 用户取消未支付订单。 */
    void cancelOrder(Long userId, Long orderId);
    /** 分页查询订单列表（TODAY/HISTORY）。 */
    OrderPageView getOrderList(Long userId, String dateScope, Integer pageNum, Integer pageSize);
    /** 查询订单详情（越权返回「订单不存在」）。 */
    OrderDetailView getOrderDetail(Long userId, Long orderId);
}
