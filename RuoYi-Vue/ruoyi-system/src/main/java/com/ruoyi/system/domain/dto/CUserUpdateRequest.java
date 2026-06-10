package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Size;

public class CUserUpdateRequest
{
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatar;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
