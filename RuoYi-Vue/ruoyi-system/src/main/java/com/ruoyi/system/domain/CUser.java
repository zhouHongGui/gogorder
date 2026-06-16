package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * C 端用户实体（对应 {@code c_user}）。
 *
 * <p>用户在首次登录时自动创建（手机号去重），初始余额为 0（余额存在独立的 {@code c_user_balance} 表）。
 * 账号唯一标识为手机号；微信身份（openid）存独立表 {@code c_user_wechat}，与手机号解耦。
 *
 * <h3>状态</h3>
 * {@code status}：1=正常 0=禁用。禁用后：登录被拒、token 有效期内请求也被 CAuthTokenFilter 拦截。
 */
public class CUser extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    private Long id;
    /** 手机号（唯一，账号主标识）。 */
    private String phone;
    /** 昵称。 */
    private String nickname;
    /** 头像 URL。 */
    private String avatar;
    /** 状态：1=正常 0=禁用。 */
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getNickname() { return nickname; }
    public void setNickname(String nickname) { this.nickname = nickname; }
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
