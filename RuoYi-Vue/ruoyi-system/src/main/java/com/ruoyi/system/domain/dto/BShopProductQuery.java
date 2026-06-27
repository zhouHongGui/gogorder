package com.ruoyi.system.domain.dto;

/** 门店员工端商品查询条件。 */
public class BShopProductQuery
{
    private Long shopId;
    private String keyword;
    private Long categoryId;
    private Integer status;
    /** ZERO=只看已售空，可为空。 */
    private String stockState;
    private Integer pageNum = 1;
    private Integer pageSize = 20;

    public void normalize()
    {
        if (pageNum == null || pageNum < 1) pageNum = 1;
        if (pageSize == null || pageSize < 1) pageSize = 20;
        if (pageSize > 100) pageSize = 100;
        if (stockState != null && !"ZERO".equals(stockState)) stockState = null;
    }

    public Integer getOffset()
    {
        normalize();
        return (pageNum - 1) * pageSize;
    }

    public Long getShopId() { return shopId; }
    public void setShopId(Long shopId) { this.shopId = shopId; }
    public String getKeyword() { return keyword; }
    public void setKeyword(String keyword) { this.keyword = keyword; }
    public Long getCategoryId() { return categoryId; }
    public void setCategoryId(Long categoryId) { this.categoryId = categoryId; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStockState() { return stockState; }
    public void setStockState(String stockState) { this.stockState = stockState; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
