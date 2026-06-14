package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class CCartUpdateRequest
{
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    @NotBlank(message = "购物车条目ID不能为空")
    private String cartItemId;

    @NotNull(message = "数量不能为空")
    @Min(value = 0, message = "数量不能小于0")
    @Max(value = 99, message = "数量不能大于99")
    private Integer quantity;

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getCartItemId() { return cartItemId; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
