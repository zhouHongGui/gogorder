package com.ruoyi.system.domain.dto;

/**
 * 购物车条目里选中的单个规格选项（展示用，结构与 {@link SpecSnapshot} 一致）。
 */
public class CCartSpecOption
{
    /** 规格模板 ID。 */
    private Long templateId;
    /** 模板名。 */
    private String templateName;
    /** 选项 ID。 */
    private String optionId;
    /** 选项展示文案。 */
    private String label;
    /** 加价（分）。 */
    private Integer priceAdd;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getTemplateName() { return templateName; }
    public void setTemplateName(String templateName) { this.templateName = templateName; }
    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getPriceAdd() { return priceAdd; }
    public void setPriceAdd(Integer priceAdd) { this.priceAdd = priceAdd; }
}
