package com.ruoyi.system.domain.dto;

public class SpecSnapshot
{
    private Long templateId;
    private String templateName;
    private String optionId;
    private String label;
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
