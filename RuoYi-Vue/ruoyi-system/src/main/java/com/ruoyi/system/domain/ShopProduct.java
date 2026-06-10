package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 门店商品 shop_product
 */
public class ShopProduct extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long shopId;
    private Long productId;

    @Min(value = 0, message = "门店售价不能小于0")
    private Integer price;

    @Min(value = 0, message = "商品状态不正确")
    @Max(value = 1, message = "商品状态不正确")
    private Integer status;

    private Integer stock;

    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    private String productName;
    private String productImage;
    private Integer basePrice;
    private Integer effectivePrice;
    private String shopName;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public Integer getBasePrice() { return basePrice; }
    public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }
    public Integer getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(Integer effectivePrice) { this.effectivePrice = effectivePrice; }
    public String getShopName() { return shopName; }
    public void setShopName(String shopName) { this.shopName = shopName; }
}
