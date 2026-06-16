package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 修改购物车数量请求（{@code /api/c/cart/update}）。quantity=0 表示删除该条目。
 */
public class CCartUpdateRequest
{
    /** 门店 ID（决定购物车 key）。 */
    @NotNull(message = "门店ID不能为空")
    private Long shopId;

    /** 购物车条目 ID（cartItemId）。 */
    @NotBlank(message = "购物车条目ID不能为空")
    private String cartItemId;

    /** 新数量（0~99，0 表示删除）。 */
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
