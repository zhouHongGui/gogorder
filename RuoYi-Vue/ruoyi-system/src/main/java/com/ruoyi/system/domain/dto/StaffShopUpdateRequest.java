package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 员工-门店关联修改请求（改默认门店标识与岗位）。
 */
public class StaffShopUpdateRequest
{
    /** 是否默认门店：1=是（每用户唯一）。 */
    @NotNull(message = "是否默认门店不能为空")
    @Min(value = 0, message = "默认门店标识不正确")
    @Max(value = 1, message = "默认门店标识不正确")
    private Integer isDefault;

    /** 门店岗位：STAFF/MANAGER/ADMIN。 */
    @NotNull(message = "门店岗位不能为空")
    @Pattern(regexp = "^(STAFF|MANAGER|ADMIN)$", message = "门店岗位不正确")
    private String role;

    public Integer getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }
}
