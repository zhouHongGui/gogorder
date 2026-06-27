package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.BOrderBoardView;
import com.ruoyi.system.domain.dto.BOrderPageView;
import com.ruoyi.system.domain.dto.BOrderQuery;
import com.ruoyi.system.domain.dto.OrderDetailView;

/** 门店员工端订单服务契约。 */
public interface IBOrderService
{
    BOrderBoardView getBoard(Long shopId);
    BOrderPageView listOrders(Long shopId, BOrderQuery query);
    OrderDetailView getDetail(Long shopId, Long orderId);
    void startMake(Long shopId, Long orderId, Long operatorId);
    void notifyPickup(Long shopId, Long orderId, Long operatorId);
    OrderDetailView scanOut(Long shopId, String code, Long operatorId);
    void completeOrder(Long shopId, Long orderId, Long operatorId);
}
