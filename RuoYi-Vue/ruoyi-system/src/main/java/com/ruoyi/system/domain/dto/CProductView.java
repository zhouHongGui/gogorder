package com.ruoyi.system.domain.dto;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.system.domain.Category;

/**
 * C端门店商品展示对象。
 */
public class CProductView
{
    /** 门店商品 ID。 */
    private Long shopProductId;
    /** 商品 ID。 */
    private Long productId;
    /** 商品名。 */
    private String name;
    /** 商品主图。 */
    private String image;
    /** 商品描述。 */
    private String description;
    /** 生效价（分，门店售价或基础价）。 */
    private Integer price;
    /** 展示起售价（分，= price + 必选规格最小加价）。 */
    private Integer displayPrice;
    /** 库存（-1=无限库存）。 */
    private Integer stock;
    /** 是否售罄（stock=0）。 */
    private boolean soldOut;
    /** 是否有规格（决定详情页是否拉规格）。 */
    private boolean hasSpecs;
    /** 近 30 天销量（批量 GROUP BY 查询回填）。 */
    private Long monthlySales;
    /** 标签列表。 */
    private List<String> tags;
    /** 所属分类列表（商品可多分类）。 */
    private List<Category> categories;
    /** 规格列表（详情接口返回完整规格，列表接口返回空）。 */
    private List<CSpecView> specs;

    /** 标签 JSON（DB 原始字段，不返回前端）。 */
    @JsonIgnore
    private String tagsJson;

    /** 规格模板 ID JSON（DB 原始字段，不返回前端）。 */
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
