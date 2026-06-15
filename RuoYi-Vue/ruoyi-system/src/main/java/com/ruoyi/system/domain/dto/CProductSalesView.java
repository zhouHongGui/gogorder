package com.ruoyi.system.domain.dto;

public class CProductSalesView
{
    private Long productId;
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
