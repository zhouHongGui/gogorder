package com.ruoyi.system.domain.dto;

import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 下单请求中的单个商品明细。价格由后端按规格重算，不信任前端。
 */
public class OrderItemRequest
{
    /** 商品 ID。 */
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    /** 规格：key=templateId，value=单个 optionId 或 optionId 数组（最多 16 组，防超大 payload）。 */
    @Size(max = 16, message = "商品规格不能超过16组")
    private Map<String, Object> specs;

    /** 数量（1~99）。 */
    @NotNull(message = "商品数量不能为空")
    @Min(value = 1, message = "商品数量不能小于1")
    @Max(value = 99, message = "单个商品数量不能超过99")
    private Integer quantity;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Map<String, Object> getSpecs() { return specs; }
    public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
