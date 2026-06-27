package com.ruoyi.system.domain.dto;

import java.util.List;

/** 门店员工端订单分页结果。 */
public class BOrderPageView
{
    private List<BOrderListItemView> rows;
    private Long total;
    private Integer pageNum;
    private Integer pageSize;

    public BOrderPageView(List<BOrderListItemView> rows, Long total, Integer pageNum, Integer pageSize)
    {
        this.rows = rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
    }

    public List<BOrderListItemView> getRows() { return rows; }
    public void setRows(List<BOrderListItemView> rows) { this.rows = rows; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
}
