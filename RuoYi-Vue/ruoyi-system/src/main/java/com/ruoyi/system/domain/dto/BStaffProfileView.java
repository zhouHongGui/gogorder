package com.ruoyi.system.domain.dto;

/** 员工端展示所需的最小员工资料。 */
public class BStaffProfileView
{
    private Long staffId;
    private String account;
    private String nickname;
    private String phone;

    public Long getStaffId() { return staffId; }
    public void setStaffId(Long staffId) { this.staffId = staffId; }
    public String getAccount() { return account; }
    public void setAccount(String account) { this.account = account; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
}
