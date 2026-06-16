package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 规格选项实体（对应 {@code spec_option}，如「大杯」「中杯」「去冰」「全糖」）。
 *
 * <h3>关键不变量（接手必读）</h3>
 * <ul>
 *   <li>{@code optionId}：全局唯一且<b>创建后不可变</b>（业务主键，下单时前端传它）。
 *       之所以独立于自增 id，是为了保证历史订单快照里的 optionId 永远可解析。</li>
 *   <li>{@code priceAdd}：该选项加价（分），叠加到商品基础价上。</li>
 *   <li>{@code isDefault}：1=默认选中（满足 minSelect 时自动带出）；单选模板最多 1 个默认。</li>
 *   <li>{@code status}：0=禁用 1=启用。禁用后不参与下单/展示，但保留行以保证历史订单引用。</li>
 * </ul>
 */
public class SpecOption extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 自增主键。 */
    private Long id;
    /** 业务主键（全局唯一 UUID，创建后不可变），下单时前端传此值。 */
    private String optionId;

    /** 所属规格模板 ID（创建后不可改）。 */
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
