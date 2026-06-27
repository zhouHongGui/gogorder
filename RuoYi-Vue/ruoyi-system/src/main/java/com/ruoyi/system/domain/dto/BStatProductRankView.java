package com.ruoyi.system.domain.dto;

/**
 * Product sales rank item for staff-side shop statistics.
 */
public class BStatProductRankView
{
    private Long productId;
    private String productName;
    private Long quantity;
    private Long salesAmount;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public Long getQuantity() { return quantity; }
    public void setQuantity(Long quantity) { this.quantity = quantity; }
    public Long getSalesAmount() { return salesAmount; }
    public void setSalesAmount(Long salesAmount) { this.salesAmount = salesAmount; }
}
