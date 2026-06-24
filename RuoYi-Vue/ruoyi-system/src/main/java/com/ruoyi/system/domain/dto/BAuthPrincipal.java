package com.ruoyi.system.domain.dto;

/** 门店员工 JWT 解析结果。 */
public class BAuthPrincipal
{
    private final Long staffId;
    private final String phone;
    private final Integer tokenVersion;

    public BAuthPrincipal(Long staffId, String phone, Integer tokenVersion)
    {
        this.staffId = staffId;
        this.phone = phone;
        this.tokenVersion = tokenVersion;
    }

    public Long getStaffId() { return staffId; }
    public String getPhone() { return phone; }
    public Integer getTokenVersion() { return tokenVersion; }
}
