package com.ruoyi.system.service;

import com.ruoyi.system.domain.dto.BOrderBoardView;
import com.ruoyi.system.domain.dto.OrderDetailView;

/** 门店员工端订单服务契约。 */
public interface IBOrderService
{
    BOrderBoardView getBoard(Long shopId);
    OrderDetailView getDetail(Long shopId, Long orderId);
    void startMake(Long shopId, Long orderId);
    int batchStartMake(Long shopId, java.util.List<Long> orderIds);
    void completeMake(Long shopId, Long orderId);
    OrderDetailView verify(Long shopId, String pickupToken);
}
