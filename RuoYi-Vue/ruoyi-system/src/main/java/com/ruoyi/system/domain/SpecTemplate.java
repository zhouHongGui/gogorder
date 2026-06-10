package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/**
 * 规格模板 spec_template
 */
public class SpecTemplate extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @NotBlank(message = "规格名称不能为空")
    @Size(max = 50, message = "规格名称长度不能超过50个字符")
    private String name;

    @NotNull(message = "规格类型不能为空")
    @Min(value = 1, message = "规格类型不正确")
    @Max(value = 2, message = "规格类型不正确")
    private Integer type;

    @NotNull(message = "是否必选不能为空")
    @Min(value = 0, message = "必选标识不正确")
    @Max(value = 1, message = "必选标识不正确")
    private Integer isRequired;

    @NotNull(message = "最少选择数量不能为空")
    @Min(value = 0, message = "最少选择数量不能小于0")
    private Integer minSelect;

    @NotNull(message = "最多选择数量不能为空")
    @Min(value = 1, message = "最多选择数量不能小于1")
    private Integer maxSelect;

    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    @Min(value = 0, message = "规格状态不正确")
    @Max(value = 1, message = "规格状态不正确")
    private Integer status;

    private Integer optionCount;
    private Integer defaultOptionCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public Integer getType() { return type; }
    public void setType(Integer type) { this.type = type; }
    public Integer getIsRequired() { return isRequired; }
    public void setIsRequired(Integer isRequired) { this.isRequired = isRequired; }
    public Integer getMinSelect() { return minSelect; }
    public void setMinSelect(Integer minSelect) { this.minSelect = minSelect; }
    public Integer getMaxSelect() { return maxSelect; }
    public void setMaxSelect(Integer maxSelect) { this.maxSelect = maxSelect; }
    public Integer getSortOrder() { return sortOrder; }
    public void setSortOrder(Integer sortOrder) { this.sortOrder = sortOrder; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public Integer getOptionCount() { return optionCount; }
    public void setOptionCount(Integer optionCount) { this.optionCount = optionCount; }
    public Integer getDefaultOptionCount() { return defaultOptionCount; }
    public void setDefaultOptionCount(Integer defaultOptionCount) { this.defaultOptionCount = defaultOptionCount; }
}
