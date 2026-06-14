package com.ruoyi.system.domain.dto;

import java.util.List;
import java.util.Map;

public class CCartItem
{
    private String cartItemId;
    private Long shopProductId;
    private Long productId;
    private String productName;
    private String image;
    private Map<String, List<String>> specs;
    private List<CCartSpecOption> selectedSpecs;
    private String specText;
    private Integer unitPrice;
    private Integer quantity;
    private Integer amount;
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
