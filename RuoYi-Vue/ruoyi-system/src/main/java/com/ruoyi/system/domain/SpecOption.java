package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 规格选项 spec_option
 */
public class SpecOption extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private String optionId;

    private Long templateId;

    @NotBlank(message = "选项名称不能为空")
    @Size(max = 50, message = "选项名称长度不能超过50个字符")
    private String label;

    @Min(value = 0, message = "加价不能小于0")
    private Integer priceAdd;

    @Min(value = 0, message = "默认标识不正确")
    @Max(value = 1, message = "默认标识不正确")
    private Integer isDefault;

    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    @Min(value = 0, message = "选项状态不正确")
    @Max(value = 1, message = "选项状态不正确")
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getOptionId() { return optionId; }
    public void setOptionId(String optionId) { this.optionId = optionId; }
    public Long getTemplateId() { return templateId; }
    public void setTemplateId(Long templateId) { this.templateId = templateId; }
    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }
    public Integer getPriceAdd() { return priceAdd; }
    public void setPriceAdd(Integer priceAdd) { this.priceAdd = priceAdd; }
    public Integer getIsDefault() { return isDefault; }
    public void setIsDefault(Integer isDefault) { this.isDefault = isDefault; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
