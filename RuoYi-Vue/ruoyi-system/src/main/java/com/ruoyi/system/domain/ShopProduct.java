package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

/**
 * 门店商品实体（对应 {@code shop_product}，门店 × 商品的关联 + 售价/库存）。
 *
 * <p>这是下单/库存的核心维度：库存存在此表，下单按 shopProductId 扣减。
 *
 * <h3>关键字段（接手必读）</h3>
 * <ul>
 *   <li>{@code stock}：商品级库存，{@code -1 = 无限库存}。有限库存走条件 UPDATE 原子扣减 + stock_ledger 流水。</li>
 *   <li>{@code price}：门店售价（分），为 null 表示「使用基础价」（回落 product.basePrice）。
 *       effectivePrice 是服务层计算的「生效价」（price 非空取 price，否则取 basePrice）。</li>
 *   <li>{@code status}：0=下架 1=上架。门店可单独控制商品上下架。</li>
 *   <li>{@code (shopId, productId)} 唯一：同一商品在同一门店只有一条记录。</li>
 *   <li>productName/productImage/basePrice/shopName：查询时 JOIN 回填的展示字段，非本表列。</li>
 * </ul>
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

    /** 商品级库存。-1=无限库存；有限库存原子扣减。 */
    private Integer stock;

    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    // 以下为查询时 JOIN 回填的展示字段（非本表持久化列）
    private String productName;
    private String productImage;
    private Integer basePrice;
    /** 生效价：price 非空取 price，否则取 basePrice（分）。 */
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
