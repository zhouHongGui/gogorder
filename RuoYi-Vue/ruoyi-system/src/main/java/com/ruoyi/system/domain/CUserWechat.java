package com.ruoyi.system.domain;

import java.io.Serializable;

/**
 * C端微信身份绑定 c_user_wechat
 */
public class CUserWechat implements Serializable
{
    private static final long serialVersionUID = 1L;

    private Long id;
    private Long userId;
    private String platform;
    private String openid;
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
