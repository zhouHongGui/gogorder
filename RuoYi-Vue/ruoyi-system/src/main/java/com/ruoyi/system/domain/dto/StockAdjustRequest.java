package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台库存调整请求
 */
public class StockAdjustRequest
{
    @NotBlank(message = "库存调整请求ID不能为空")
    @Size(max = 50, message = "库存调整请求ID长度不能超过50个字符")
    private String requestId;

    @NotNull(message = "目标库存不能为空")
    @Min(value = -1, message = "库存不能小于-1")
    private Integer stock;

    @NotBlank(message = "库存调整原因不能为空")
    @Size(max = 200, message = "库存调整原因长度不能超过200个字符")
    private String reason;

    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
