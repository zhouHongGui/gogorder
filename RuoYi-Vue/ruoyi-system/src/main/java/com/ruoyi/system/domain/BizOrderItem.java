package com.ruoyi.system.domain;

/**
 * 订单明细实体（对应 {@code biz_order_item}）。
 *
 * <p>下单时做完整快照：商品名/图片/单价/规格都固化到明细，保证日后商品改价/改规格不影响历史订单展示。
 * 金额单位均为「分」。
 */
public class BizOrderItem
{
    /** 主键。 */
    private Long id;
    /** 所属订单 ID。 */
    private Long orderId;
    /** 商品 ID（商品库主数据）。 */
    private Long productId;
    /** 门店商品 ID（shop_product，库存/扣减维度）。 */
    private Long shopProductId;
    /** 商品名快照（下单时固化）。 */
    private String productName;
    /** 商品图片快照。 */
    private String productImage;
    /** 规格快照（JSON，含 templateId/optionId/label/priceAdd 列表）。 */
    private String specs;
    /** 单价快照（分，已含规格加价）。 */
    private Integer unitPrice;
    /** 数量。 */
    private Integer quantity;
    /** 小计（分，= unitPrice × quantity）。 */
    private Integer subtotal;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getShopProductId() { return shopProductId; }
    public void setShopProductId(Long shopProductId) { this.shopProductId = shopProductId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductImage() { return productImage; }
    public void setProductImage(String productImage) { this.productImage = productImage; }
    public String getSpecs() { return specs; }
    public void setSpecs(String specs) { this.specs = specs; }
    public Integer getUnitPrice() { return unitPrice; }
    public void setUnitPrice(Integer unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public Integer getSubtotal() { return subtotal; }
    public void setSubtotal(Integer subtotal) { this.subtotal = subtotal; }
}
