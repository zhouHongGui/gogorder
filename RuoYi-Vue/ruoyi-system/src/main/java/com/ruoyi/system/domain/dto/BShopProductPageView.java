package com.ruoyi.system.domain.dto;

import java.util.List;

/** 门店员工端商品分页结果。 */
public class BShopProductPageView
{
    private List<BShopProductView> rows;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public BShopProductPageView(List<BShopProductView> rows, Long total, Integer pageNum, Integer pageSize)
    {
        this.rows = rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<BShopProductView> getRows() { return rows; }
    public void setRows(List<BShopProductView> rows) { this.rows = rows; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
