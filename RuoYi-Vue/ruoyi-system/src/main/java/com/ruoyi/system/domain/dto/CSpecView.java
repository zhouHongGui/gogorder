package com.ruoyi.system.domain.dto;

import java.util.List;
import com.ruoyi.system.domain.SpecOption;

/**
 * C端商品规格展示对象。
 */
public class CSpecView
{
    private Long templateId;
    private String name;
    private Integer type;
    private boolean required;
    private Integer minSelect;
    private Integer maxSelect;
    private List<SpecOption> options;

    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public boolean isRequired() { return required; }
    public void setRequired(boolean required) { this.required = required; }
    public Integer getMinSelect() { return minSelect; }
    public void setMinSelect(Integer minSelect) { this.minSelect = minSelect; }
    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }
    public List<SpecOption> getOptions() { return options; }
    public void setOptions(List<SpecOption> options) { this.options = options; }
}
