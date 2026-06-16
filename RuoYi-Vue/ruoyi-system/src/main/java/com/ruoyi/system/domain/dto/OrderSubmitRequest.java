package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下单请求（{@code /api/c/order/submit}）。submitToken 保证幂等，价格由后端重算。
 */
public class OrderSubmitRequest
{
    /** 门店 ID。 */
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    /** 下单幂等令牌（客户端生成，同令牌返回同一订单，防重复下单）。 */
    @NotBlank(message = "提交令牌不能为空")
    @Size(max = 64, message = "提交令牌长度不能超过64个字符")
    private String submitToken;

    /** 订单类型：NORMAL=即时单 / PREORDER=预订单。 */
    @NotBlank(message = "订单类型不能为空")
    private String orderType;

    /** 预约取餐时间（仅预订单，格式 yyyy-MM-dd HH:mm）。 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime scheduledPickupTime;

    /** 备注。 */
    @Size(max = 200, message = "备注长度不能超过200个字符")
    private String remark;

    /** 商品明细（1~20 项，逐项校验）。 */
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
