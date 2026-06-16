package com.ruoyi.system.domain.dto;

import jakarta.validation.constraints.Size;

/**
 * 修改用户资料请求（{@code /api/c/user/update}，仅昵称/头像可改）。
 */
public class CUserUpdateRequest
{
    /** 昵称。 */
    @Size(max = 50, message = "昵称长度不能超过50个字符")
    private String nickname;

    /** 头像 URL。 */
    @Size(max = 255, message = "头像地址长度不能超过255个字符")
    private String avatar;

    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
}
