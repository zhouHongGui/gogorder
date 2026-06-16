package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

/**
 * 员工-门店关联保存请求（把员工关联到门店，设置默认门店与岗位）。
 */
public class StaffShopRequest
{
    /** 员工（管理端用户）ID。 */
    @NotNull(message = "员工ID不能为空")
    private Long userId;

    /** 是否默认门店：1=是（每用户唯一，靠 default_user_id 唯一索引保证）。 */
    @NotNull(message = "是否默认门店不能为空")
    @Min(value = 0, message = "默认门店标识不正确")
    @Max(value = 1, message = "默认门店标识不正确")
    private Integer isDefault;

    /** 门店岗位：STAFF=店员 / MANAGER=店长 / ADMIN=管理员。 */
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
