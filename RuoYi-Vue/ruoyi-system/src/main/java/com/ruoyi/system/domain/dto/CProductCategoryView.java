package com.ruoyi.system.domain.dto;

import java.util.Date;
import com.ruoyi.system.domain.Category;

public class CProductCategoryView
{
    private Long productId;
    private Long categoryId;
    private String name;
    private Integer sortOrder;
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
