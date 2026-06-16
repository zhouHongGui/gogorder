package com.ruoyi.system.domain.dto;

import java.util.List;

/**
 * 规格校验与定价结果（{@code SpecValidationServiceImpl.validateAndPrice} 返回）。
 * 下单/加购据此组装订单明细与购物车条目，价格由后端重算，不信任前端。
 */
public class SpecValidationResult
{
    /** 选中的规格快照列表（含加价信息，用于订单明细持久化）。 */
    private List<SpecSnapshot> selectedOptions;
    /** 单价（分，= 基础价 + 各选项加价，后端重算）。 */
    private int unitPrice;
    /** 门店商品 ID（库存/扣减维度）。 */
    private Long shopProductId;
    /** 商品名快照。 */
    private String productName;
    /** 商品图片快照。 */
    private String productImage;
    /** 库存（-1=无限库存）。 */
    private int stock;
    /** 归一化规格 {templateId → [排序去重的 optionId]}，用于购物车 cartItemId 计算等。 */
    private java.util.Map<String, java.util.List<String>> normalizedSpecs;

    public List<SpecSnapshot> getSelectedOptions() { return selectedOptions; }
    public void setSelectedOptions(List<SpecSnapshot> selectedOptions) { this.selectedOptions = selectedOptions; }
    public int getUnitPrice() { return unitPrice; }
    public void setUnitPrice(int unitPrice) { this.unitPrice = unitPrice; }
    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public int getStock() { return stock; }
    public void setStock(int stock) { this.stock = stock; }
    public java.util.Map<String, java.util.List<String>> getNormalizedSpecs() { return normalizedSpecs; }
    public void setNormalizedSpecs(java.util.Map<String, java.util.List<String>> normalizedSpecs) { this.normalizedSpecs = normalizedSpecs; }
}
