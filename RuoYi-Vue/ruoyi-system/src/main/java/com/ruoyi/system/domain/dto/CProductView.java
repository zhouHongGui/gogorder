package com.ruoyi.system.domain.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.system.domain.Category;

/**
 * C端门店商品展示对象。
 */
public class CProductView
{
    private Long shopProductId;
    private Long productId;
    private String name;
    private String image;
    private String description;
    private Integer price;
    private Integer displayPrice;
    private Integer stock;
    private boolean soldOut;
    private boolean hasSpecs;
    private Long monthlySales;
    private List<String> tags;
    private List<Category> categories;
    private List<CSpecView> specs;

    @JsonIgnore
    private String tagsJson;

    @JsonIgnore
    private String specTemplateIdsJson;

    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getPrice() { return price; }
    public void setPrice(Integer price) { this.price = price; }
    public Integer getDisplayPrice() { return displayPrice; }
    public void setDisplayPrice(Integer displayPrice) { this.displayPrice = displayPrice; }
    public Integer getStock() { return stock; }
    public void setStock(Integer stock) { this.stock = stock; }
    public boolean isSoldOut() { return soldOut; }
    public void setSoldOut(boolean soldOut) { this.soldOut = soldOut; }
    public boolean isHasSpecs() { return hasSpecs; }
    public void setHasSpecs(boolean hasSpecs) { this.hasSpecs = hasSpecs; }
    public Long getMonthlySales() { return monthlySales; }
    public void setMonthlySales(Long monthlySales) { this.monthlySales = monthlySales; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public List<CSpecView> getSpecs() { return specs; }
    public void setSpecs(List<CSpecView> specs) { this.specs = specs; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public String getSpecTemplateIdsJson() { return specTemplateIdsJson; }
    public void setSpecTemplateIdsJson(String specTemplateIdsJson) { this.specTemplateIdsJson = specTemplateIdsJson; }
}
