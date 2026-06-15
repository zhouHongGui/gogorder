package com.ruoyi.system.domain.dto;

import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class OrderItemRequest
{
    @NotNull(message = "商品ID不能为空")
    private Long productId;

    @Size(max = 16, message = "商品规格不能超过16组")
    private Map<String, Object> specs;

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
