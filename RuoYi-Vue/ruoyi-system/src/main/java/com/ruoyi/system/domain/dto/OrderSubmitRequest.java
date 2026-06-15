package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderSubmitRequest
{
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    @NotBlank(message = "提交令牌不能为空")
    @Size(max = 64, message = "提交令牌长度不能超过64个字符")
    private String submitToken;

    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledPickupTime;

    @Size(max = 200, message = "备注长度不能超过200个字符")
    private String remark;

    @NotEmpty(message = "订单商品不能为空")
    @Size(max = 20, message = "订单明细不能超过20项")
    @Valid
    private List<OrderItemRequest> items;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getSubmitToken() { return submitToken; }
    public void setSubmitToken(String submitToken) { this.submitToken = submitToken; }
    public String getOrderType() { return orderType; }
    public void setOrderType(String orderType) { this.orderType = orderType; }
    public LocalDateTime getScheduledPickupTime() { return scheduledPickupTime; }
    public void setScheduledPickupTime(LocalDateTime scheduledPickupTime) { this.scheduledPickupTime = scheduledPickupTime; }
    public String getRemark() { return remark; }
    public void setRemark(String remark) { this.remark = remark; }
    public List<OrderItemRequest> getItems() { return items; }
    public void setItems(List<OrderItemRequest> items) { this.items = items; }
}
