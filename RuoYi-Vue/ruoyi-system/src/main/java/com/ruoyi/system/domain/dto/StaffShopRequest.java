package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

/**
 * 员工-门店授权保存请求（把员工授权到门店，并设置默认门店）。
 */
public class StaffShopRequest
{
    /** 独立门店员工 ID。 */
    @NotNull(message = "员工ID不能为空")
    private Long staffId;

    /** 是否默认门店：1=是（每员工唯一，靠 default_staff_id 唯一索引保证）。 */
    @NotNull(message = "是否默认门店不能为空")
    @Min(value = 0, message = "默认门店标识不正确")
    @Max(value = 1, message = "默认门店标识不正确")
    private Integer isDefault;

    public Long getStaffId()
    {
        return staffId;
    }

    public void setStaffId(Long staffId)
    {
        this.staffId = staffId;
    }

    public Integer getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault)
    {
        this.isDefault = isDefault;
    }

}
