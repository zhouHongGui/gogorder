package com.ruoyi.system.domain.dto;

import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

/** 门店员工端商品行。 */
public class BShopProductView
{
    private Long shopProductId;
    private Long shopId;
    private Long productId;
    private String productName;
    private String productImage;
    private String categoryNames;
    private Integer basePrice;
    private Integer price;
    private Integer effectivePrice;
    private Integer status;
    private String statusDesc;
    private Integer stock;
    private String stockDesc;
    private Boolean soldOut;
    private Integer sortOrder;
    private Integer todaySales;
    private Integer monthlySales;
    private LocalDateTime createTime;

    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getCategoryNames() { return categoryNames; }
    public void setCategoryNames(String categoryNames) { this.categoryNames = categoryNames; }
    public Integer getBasePrice() { return basePrice; }
    public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getEffectivePrice() { return effectivePrice; }
    public void setEffectivePrice(Integer effectivePrice) { this.effectivePrice = effectivePrice; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusDesc() { return statusDesc; }
    public void setStatusDesc(String statusDesc) { this.statusDesc = statusDesc; }
    @JsonIgnore
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public String getStockDesc() { return stockDesc; }
    public void setStockDesc(String stockDesc) { this.stockDesc = stockDesc; }
    public Boolean getSoldOut() { return soldOut; }
    public void setSoldOut(Boolean soldOut) { this.soldOut = soldOut; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getTodaySales() { return todaySales; }
    public void setTodaySales(Integer todaySales) { this.todaySales = todaySales; }
    public Integer getMonthlySales() { return monthlySales; }
    public void setMonthlySales(Integer monthlySales) { this.monthlySales = monthlySales; }
    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
