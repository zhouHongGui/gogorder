package com.ruoyi.system.domain.dto;

import java.util.List;

public class OrderPageView
{
    private List<OrderListItemView> rows;
    private long total;
    private int pageNum;
    private int pageSize;
    private boolean hasMore;

    public OrderPageView(List<OrderListItemView> rows, long total, int pageNum, int pageSize)
    {
        this.rows = rows;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.hasMore = (long) pageNum * pageSize < total;
    }

    public List<OrderListItemView> getRows() { return rows; }
    public void setRows(List<OrderListItemView> rows) { this.rows = rows; }
    public long getTotal() { return total; }
    public void setTotal(long total) { this.total = total; }
    public int getPageNum() { return pageNum; }
    public void setPageNum(int pageNum) { this.pageNum = pageNum; }
    public int getPageSize() { return pageSize; }
    public void setPageSize(int pageSize) { this.pageSize = pageSize; }
    public boolean isHasMore() { return hasMore; }
    public void setHasMore(boolean hasMore) { this.hasMore = hasMore; }
}
