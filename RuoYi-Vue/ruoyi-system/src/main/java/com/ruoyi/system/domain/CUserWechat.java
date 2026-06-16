package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * 微信身份绑定实体（对应 {@code c_user_wechat}）。
 *
 * <p>微信身份与手机号解耦：用户首次用微信登录时未绑定手机号，需用 phone code 换手机号完成绑定，
 * 绑定记录存此表（openid/unionid → userId）。{@code (platform, openid)} 唯一。
 *
 * <p>{@code platform}：MP=小程序（当前仅支持）。
 */
public class CUserWechat implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 主键。 */
    private Long id;
    /** 绑定的 C 端用户 ID。 */
    private Long userId;
    /** 平台：MP=小程序。 */
    private String platform;
    /** 微信 openid（与 platform 组合唯一）。 */
    private String openid;
    /** 微信 unionid（可能为空）。 */
    private String unionid;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }
    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }
}
