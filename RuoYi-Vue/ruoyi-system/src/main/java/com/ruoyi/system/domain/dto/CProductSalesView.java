package com.ruoyi.system.domain.dto;

/**
 * 商品近 30 天销量（{@code selectCProductMonthlySales} 的行映射，按 productId 分组求和）。
 * 用于菜单列表销量批量回填，避免逐商品相关子查询。
 */
public class CProductSalesView
{
    /** 商品 ID。 */
    private Long productId;
    /** 近 30 天销量（已支付订单的商品数量求和）。 */
    private Long monthlySales;

    public Long getProductId()
    {
        return productId;
    }

    public void setProductId(Long productId)
    {
        this.productId = productId;
    }

    public Long getMonthlySales()
    {
        return monthlySales;
    }

    public void setMonthlySales(Long monthlySales)
    {
        this.monthlySales = monthlySales;
    }
}
