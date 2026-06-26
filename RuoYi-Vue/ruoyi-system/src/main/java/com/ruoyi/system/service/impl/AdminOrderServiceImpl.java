package com.ruoyi.system.service.impl;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BizOrderItem;
import com.ruoyi.system.domain.dto.AdminOrderDetailView;
import com.ruoyi.system.domain.dto.AdminOrderListItemView;
import com.ruoyi.system.domain.dto.AdminOrderQuery;
import com.ruoyi.system.domain.dto.OrderItemView;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.service.IAdminOrderService;

@Service
public class AdminOrderServiceImpl implements IAdminOrderService
{
    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;

    @Override
    public List<AdminOrderListItemView> selectOrderList(AdminOrderQuery query)
    {
        List<AdminOrderListItemView> list = bizOrderMapper.selectAdminOrderList(query);
        list.forEach(this::fillListDesc);
        return list;
    }

    @Override
    public AdminOrderDetailView selectOrderDetail(Long orderId)
    {
        AdminOrderDetailView detail = bizOrderMapper.selectAdminOrderDetail(orderId);
        if (detail == null)
        {
            throw new ServiceException("订单不存在");
        }
        fillDetailDesc(detail);
        detail.setItems(bizOrderItemMapper.selectByOrderId(orderId).stream()
                .map(this::buildItemView)
                .toList());
        return detail;
    }

    private void fillListDesc(AdminOrderListItemView view)
    {
        view.setUserPhoneMasked(maskPhone(view.getUserPhone()));
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), view.getOrderType()));
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), view.getOrderStatus()));
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), view.getPayStatus()));
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), view.getRefundStatus()));
    }

    private void fillDetailDesc(AdminOrderDetailView view)
    {
        view.setUserPhoneMasked(maskPhone(view.getUserPhone()));
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), view.getOrderType()));
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), view.getOrderStatus()));
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), view.getPayStatus()));
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), view.getRefundStatus()));
    }

    private OrderItemView buildItemView(BizOrderItem item)
    {
        List<SpecSnapshot> specs = StringUtils.isEmpty(item.getSpecs())
                ? Collections.emptyList() : JSON.parseArray(item.getSpecs(), SpecSnapshot.class);
        OrderItemView view = new OrderItemView();
        view.setProductId(item.getProductId());
        view.setProductName(item.getProductName());
        view.setProductImage(item.getProductImage());
        view.setSpecs(specs);
        view.setSpecText(specs.isEmpty() ? "默认规格"
                : specs.stream().map(SpecSnapshot::getLabel).collect(Collectors.joining("、")));
        view.setUnitPrice(item.getUnitPrice());
        view.setQuantity(item.getQuantity());
        view.setSubtotal(item.getSubtotal());
        return view;
    }

    private String maskPhone(String phone)
    {
        if (StringUtils.isEmpty(phone) || phone.length() < 7)
        {
            return phone;
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    private String enumDesc(OrderTypeEnum[] values, String code)
    {
        for (OrderTypeEnum value : values) if (value.getCode().equals(code)) return value.getDesc();
        return "";
    }

    private String enumDesc(OrderStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (OrderStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(PayStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (PayStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    private String enumDesc(RefundStatusEnum[] values, Integer code)
    {
        if (code == null) return "";
        for (RefundStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }
}
