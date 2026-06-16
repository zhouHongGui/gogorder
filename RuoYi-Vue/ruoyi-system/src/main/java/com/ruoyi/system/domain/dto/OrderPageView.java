package com.ruoyi.system.domain.dto;

import java.util.List;

/**
 * 订单列表分页结果。hasMore 由 pageNum×pageSize < total 推导，供前端判断是否可加载更多。
 */
public class OrderPageView
{
    /** 当前页订单列表。 */
    private List<OrderListItemView> rows;
    /** 总条数。 */
    private long total;
    /** 当前页码（从 1 起）。 */
    private int pageNum;
    /** 每页条数（1~20）。 */
    private int pageSize;
    /** 是否还有下一页。 */
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
