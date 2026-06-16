package com.ruoyi.system.domain.dto;

import java.util.Date;
import com.ruoyi.system.domain.Category;

/**
 * 商品-分类关联的扁平视图（JOIN product_category + category 的查询结果）。
 * {@link #toCategory()} 转成 {@link Category} 实体供展示。
 */
public class CProductCategoryView
{
    /** 商品 ID。 */
    private Long productId;
    /** 分类 ID。 */
    private Long categoryId;
    /** 分类名。 */
    private String name;
    /** 分类内排序。 */
    private Integer sortOrder;
    /** 分类状态。 */
    private Integer status;
    private Date createTime;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Date getCreateTime() { return createTime; }
    public void setCreateTime(Date createTime) { this.createTime = createTime; }

    public Category toCategory()
    {
        Category category = new Category();
        category.setId(categoryId);
        category.setName(name);
        category.setSortOrder(sortOrder);
        category.setStatus(status);
        category.setCreateTime(createTime);
        return category;
    }
}
