package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 员工-门店关联保存请求
 */
public class StaffShopRequest
{
    @NotNull(message = "员工ID不能为空")
    private Long userId;

    @NotNull(message = "是否默认门店不能为空")
    @Min(value = 0, message = "默认门店标识不正确")
    @Max(value = 1, message = "默认门店标识不正确")
    private Integer isDefault;

    @NotNull(message = "门店岗位不能为空")
    @Pattern(regexp = "^(STAFF|MANAGER|ADMIN)$", message = "门店岗位不正确")
    private String role;

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

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
