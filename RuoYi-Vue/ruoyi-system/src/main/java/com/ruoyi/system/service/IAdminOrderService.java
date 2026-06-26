package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.dto.AdminOrderDetailView;
import com.ruoyi.system.domain.dto.AdminOrderListItemView;
import com.ruoyi.system.domain.dto.AdminOrderQuery;

public interface IAdminOrderService
{
    List<AdminOrderListItemView> selectOrderList(AdminOrderQuery query);

    AdminOrderDetailView selectOrderDetail(Long orderId);
}
