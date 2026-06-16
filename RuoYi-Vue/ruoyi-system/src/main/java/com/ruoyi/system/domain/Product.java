package com.ruoyi.system.domain;

import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 商品库主数据实体（对应 {@code product}）。
 *
 * <p>商品是「商品库 → 分配到门店（shop_product）」两级模型：商品库存基础价/规格模板，
 * 门店商品可覆盖售价、设置库存。商品可属多个分类（product_category 关联表）。
 *
 * <h3>JSON 字段（接手必读）</h3>
 * images/tags/specTemplateIds 是 List，DB 以 JSON 字符串存储（imagesJson/tagsJson/specTemplateIdsJson），
 * {@code @JsonIgnore} 的 xxxJson 不返回前端，由 Service 层负责序列化/反序列化。
 *
 * <h3>非持久化字段</h3>
 * categoryId/categoryIds/categories/specTemplates 仅用于管理后台读写商品时的关联数据，
 * 非直接映射列。金额单位：分。
 */
public class Product extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "商品名称不能为空")
    @Size(max = 100, message = "商品名称长度不能超过100个字符")
    private String name;

    @Size(max = 255, message = "商品主图地址长度不能超过255个字符")
    private String image;

    private List<String> images;

    @JsonIgnore
    private String imagesJson;

    @Size(max = 500, message = "商品描述长度不能超过500个字符")
    private String description;

    @Min(value = 0, message = "基础价格不能小于0")
    private Integer basePrice;

    private List<Long> specTemplateIds;

    @JsonIgnore
    private String specTemplateIdsJson;

    private List<String> tags;

    @JsonIgnore
    private String tagsJson;

    @Min(value = 0, message = "商品状态不正确")
    @Max(value = 1, message = "商品状态不正确")
    private Integer status;

    private Long categoryId;
    private List<Long> categoryIds;
    private List<Category> categories;
    private List<SpecTemplate> specTemplates;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }
    public String getImagesJson() { return imagesJson; }
    public void setImagesJson(String imagesJson) { this.imagesJson = imagesJson; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public Integer getBasePrice() { return basePrice; }
    public void setBasePrice(Integer basePrice) { this.basePrice = basePrice; }
    public List<Long> getSpecTemplateIds() { return specTemplateIds; }
    public void setSpecTemplateIds(List<Long> specTemplateIds) { this.specTemplateIds = specTemplateIds; }
    public String getSpecTemplateIdsJson() { return specTemplateIdsJson; }
    public void setSpecTemplateIdsJson(String specTemplateIdsJson) { this.specTemplateIdsJson = specTemplateIdsJson; }
    public List<String> getTags() { return tags; }
    public void setTags(List<String> tags) { this.tags = tags; }
    public String getTagsJson() { return tagsJson; }
    public void setTagsJson(String tagsJson) { this.tagsJson = tagsJson; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public List<Long> getCategoryIds() { return categoryIds; }
    public void setCategoryIds(List<Long> categoryIds) { this.categoryIds = categoryIds; }
    public List<Category> getCategories() { return categories; }
    public void setCategories(List<Category> categories) { this.categories = categories; }
    public List<SpecTemplate> getSpecTemplates() { return specTemplates; }
    public void setSpecTemplates(List<SpecTemplate> specTemplates) { this.specTemplates = specTemplates; }
}
