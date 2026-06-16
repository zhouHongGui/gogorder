package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 后台库存调整请求（直接设置目标库存，非增减）。幂等：requestId 相同返回原记录。
 */
public class StockAdjustRequest
{
    /** 幂等请求 ID（同一 ID 视为重复调整，返回原流水）。 */
    @NotBlank(message = "库存调整请求ID不能为空")
    @Size(max = 50, message = "库存调整请求ID长度不能超过50个字符")
    private String requestId;

    /** 目标库存（-1=设为无限库存）。 */
    @NotNull(message = "目标库存不能为空")
    @Min(value = -1, message = "库存不能小于-1")
    private Integer stock;

    /** 调整原因（写入流水便于审计）。 */
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
