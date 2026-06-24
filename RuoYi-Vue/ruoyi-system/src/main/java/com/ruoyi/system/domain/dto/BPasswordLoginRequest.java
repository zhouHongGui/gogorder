package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** 门店员工端账号密码登录请求。 */
public class BPasswordLoginRequest
{
    @NotBlank(message = "账号不能为空")
    @Size(min = 2, max = 20, message = "账号长度应为2到20个字符")
    private String account;

    @NotBlank(message = "密码不能为空")
    @Size(min = 5, max = 20, message = "密码长度应为5到20个字符")
    private String password;

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public String getPassword()
    {
        return password;
    }

    public void setPassword(String password)
    {
        this.password = password;
    }
}
