package com.ruoyi.system.domain.dto;

import java.util.List;
import com.ruoyi.system.domain.SpecOption;

/**
 * C 端商品规格展示对象（商品详情返回，供规格选择 UI）。
 */
public class CSpecView
{
    /** 模板 ID。 */
    private Long templateId;
    /** 模板名（如「杯型」）。 */
    private String name;
    /** 类型：1=单选 2=多选。 */
    private Integer type;
    /** 是否必选。 */
    private boolean required;
    /** 最少选择数。 */
    private Integer minSelect;
    /** 最多选择数。 */
    private Integer maxSelect;
    /** 启用选项列表（禁用选项已过滤）。 */
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
