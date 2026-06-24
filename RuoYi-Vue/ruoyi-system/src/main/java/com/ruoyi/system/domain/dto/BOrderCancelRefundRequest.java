package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 门店员工端制作前取消并退款请求。 */
public class BOrderCancelRefundRequest
{
    @NotBlank(message = "取消原因不能为空")
    @Size(max = 200, message = "取消原因不能超过200个字符")
    private String reason;

    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
