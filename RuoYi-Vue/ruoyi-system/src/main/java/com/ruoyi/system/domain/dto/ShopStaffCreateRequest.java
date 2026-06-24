package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** 创建独立门店员工账号。 */
public class ShopStaffCreateRequest
{
    @NotBlank(message = "员工账号不能为空")
    @Pattern(regexp = "^[a-zA-Z0-9_]{2,30}$", message = "员工账号只能包含字母、数字和下划线")
    private String account;
    @NotBlank(message = "员工姓名不能为空")
    @Size(max = 50, message = "员工姓名不能超过50个字符")
    private String nickname;
    @NotBlank(message = "手机号不能为空")
    @Pattern(regexp = "^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;
    @NotBlank(message = "密码不能为空")
    @Size(min = 8, max = 20, message = "密码长度应为8到20个字符")
    private String password;
    @Min(value = 0, message = "员工状态不正确")
    @Max(value = 1, message = "员工状态不正确")
    private Integer status = 0;

    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
