package com.ruoyi.system.domain.dto;

import java.util.LinkedHashMap;
import java.util.Map;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public class CCartAddRequest
{
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    @NotNull(message = "商品ID不能为空")
    private Long productId;

    private Map<String, Object> specs = new LinkedHashMap<>();

    @NotNull(message = "数量不能为空")
    @Min(value = 1, message = "数量不能小于1")
    @Max(value = 99, message = "数量不能大于99")
    private Integer quantity;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Map<String, Object> getSpecs() { return specs; }
    public void setSpecs(Map<String, Object> specs) { this.specs = specs; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
