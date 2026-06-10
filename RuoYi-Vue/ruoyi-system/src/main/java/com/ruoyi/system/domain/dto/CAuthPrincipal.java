package com.ruoyi.system.domain.dto;

public class CAuthPrincipal
{
    private Long userId;
    private String phone;

    public CAuthPrincipal(Long userId, String phone)
    {
        this.userId = userId;
        this.phone = phone;
    }

    public Long getUserId() { return userId; }
    public String getPhone() { return phone; }
}
