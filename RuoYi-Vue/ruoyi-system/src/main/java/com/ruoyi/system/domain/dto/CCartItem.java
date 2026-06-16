package com.ruoyi.system.domain.dto;

import java.util.List;
import java.util.Map;

/**
 * 购物车条目（序列化为 JSON 存 Redis Hash 的 {@code item:{cartItemId}} 字段）。
 * {@code cartItemId} 由 productId+规格归一化串哈希得出，决定「同商品同规格」是否合并。
 */
public class CCartItem
{
    /** 条目 ID（SHA-256 前 24 位，同商品同规格 → 相同 ID → 合并数量）。 */
    private String cartItemId;
    /** 门店商品 ID。 */
    private Long shopProductId;
    /** 商品 ID。 */
    private Long productId;
    /** 商品名。 */
    private String productName;
    /** 商品图片。 */
    private String image;
    /** 归一化规格 {templateId → [optionId]}（参与 cartItemId 计算）。 */
    private Map<String, List<String>> specs;
    /** 选中的规格选项（含加价，展示用）。 */
    private List<CCartSpecOption> selectedSpecs;
    /** 规格展示文本（如「大杯、去冰」）。 */
    private String specText;
    /** 单价（分，加购时后端重算快照）。 */
    private Integer unitPrice;
    /** 数量。 */
    private Integer quantity;
    /** 小计金额（分，= unitPrice × quantity）。 */
    private Integer amount;
    /** 库存快照（-1=无限，加购时快照，下单以后端实时校验为准）。 */
    private Integer stock;

    public String getCartItemId() { return cartItemId; }
    public void setCartItemId(String cartItemId) { this.cartItemId = cartItemId; }
    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public Map<String, List<String>> getSpecs() { return specs; }
    public void setSpecs(Map<String, List<String>> specs) { this.specs = specs; }
    public List<CCartSpecOption> getSelectedSpecs() { return selectedSpecs; }
    public void setSelectedSpecs(List<CCartSpecOption> selectedSpecs) { this.selectedSpecs = selectedSpecs; }
    public String getSpecText() { return specText; }
    public void setSpecText(String specText) { this.specText = specText; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getAmount() { return amount; }
    public void setAmount(Integer amount) { this.amount = amount; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
}
