package com.ruoyi.system.domain.dto;

import java.util.List;

public class SpecValidationResult
{
    private List<SpecSnapshot> selectedOptions;
    private int unitPrice;
    private Long shopProductId;
    private String productName;
    private String productImage;
    private int stock;
    private java.util.Map<String, java.util.List<String>> normalizedSpecs;

    public List<SpecSnapshot> getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(List<SpecSnapshot> selectedOptions) { this.selectedOptions = selectedOptions; }
    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public java.util.Map<String, java.util.List<String>> getNormalizedSpecs() { return normalizedSpecs; }
    public void setNormalizedSpecs(java.util.Map<String, java.util.List<String>> normalizedSpecs) { this.normalizedSpecs = normalizedSpecs; }
}
