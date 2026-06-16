package com.ruoyi.system.domain.dto;

/**
 * 订单明细展示对象（订单详情/列表通用，来自订单明细快照）。
 */
public class OrderItemView
{
    /** 商品 ID。 */
    private Long productId;
    /** 商品名快照。 */
    private String productName;
    /** 商品图片快照。 */
    private String productImage;
    /** 规格展示文本（如「大杯、去冰」）。 */
    private String specText;
    /** 规格快照列表（SpecSnapshot，反序列化自明细 specs JSON）。 */
    private Object specs;
    /** 单价快照（分）。 */
    private Integer unitPrice;
    /** 数量。 */
    private Integer quantity;
    /** 小计（分）。 */
    private Integer subtotal;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getSpecText() { return specText; }
    public void setSpecText(String specText) { this.specText = specText; }
    public Object getSpecs() { return specs; }
    public void setSpecs(Object specs) { this.specs = specs; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
}
