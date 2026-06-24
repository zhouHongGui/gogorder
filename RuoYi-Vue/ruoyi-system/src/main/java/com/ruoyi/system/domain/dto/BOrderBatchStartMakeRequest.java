package com.ruoyi.system.domain.dto;

import java.util.List;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

/** 门店员工端批量开始制作请求。 */
public class BOrderBatchStartMakeRequest
{
    @NotEmpty(message = "请选择订单")
    @Size(max = 50, message = "单次最多处理50个订单")
    private List<Long> orderIds;

    public List<Long> getOrderIds() { return orderIds; }
    public void setOrderIds(List<Long> orderIds) { this.orderIds = orderIds; }
}
