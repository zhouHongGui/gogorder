package com.ruoyi.system.domain.dto;

/**
 * 规格快照（下单时固化到订单明细，保证日后改价/改规格不影响历史订单）。
 * 持久化为 JSON 存入 {@code biz_order_item.specs}。
 */
public class SpecSnapshot
{
    /** 规格模板 ID（如「杯型」）。 */
    private Long templateId;
    /** 模板名（如「杯型」）。 */
    private String templateName;
    /** 选项 ID（全局唯一，不可变）。 */
    private String optionId;
    /** 选项展示文案（如「大杯」）。 */
    private String label;
    /** 加价（分，叠加到基础价）。 */
    private int priceAdd;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public int getPriceAdd() { return priceAdd; }
    public void setPriceAdd(int priceAdd) { this.priceAdd = priceAdd; }
}
