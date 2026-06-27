package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** 门店员工端商品售空请求。 */
public class BShopProductSoldOutRequest
{
    @NotNull(message = "售空状态不能为空")
    private Boolean soldOut;

    /** 幂等请求 ID（同一 ID 视为重复操作，返回原流水）。 */
    @NotBlank(message = "商品售空操作请求ID不能为空")
    @Size(max = 50, message = "商品售空操作请求ID长度不能超过50个字符")
    private String requestId;

    /** 操作原因（写入流水便于审计）。 */
    @NotBlank(message = "商品售空操作原因不能为空")
    @Size(max = 200, message = "商品售空操作原因长度不能超过200个字符")
    private String reason;

    public Boolean getSoldOut() { return soldOut; }
    public void setSoldOut(Boolean soldOut) { this.soldOut = soldOut; }
    public String getRequestId() { return requestId; }
    public void setRequestId(String requestId) { this.requestId = requestId; }
    public String getReason() { return reason; }
    public void setReason(String reason) { this.reason = reason; }
}
