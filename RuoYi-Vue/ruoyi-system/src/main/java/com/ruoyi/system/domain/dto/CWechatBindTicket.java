package com.ruoyi.system.domain.dto;

import java.io.Serializable;

/**
 * 微信绑定凭证（序列化存 Redis，5 分钟有效）。
 * loginByWechat 未绑定时颁发，bindWechatPhone 凭它取回 openid/unionid 完成绑定。
 */
public class CWechatBindTicket implements Serializable
{
    private static final long serialVersionUID = 1L;

    /** 微信 openid。 */
    private String openid;
    /** 微信 unionid（可能为空）。 */
    private String unionid;

    public CWechatBindTicket()
    {
    }

    public CWechatBindTicket(String openid, String unionid)
    {
        this.openid = openid;
        this.unionid = unionid;
    }

    public String getOpenid() { return openid; }
    public void setOpenid(String openid) { this.openid = openid; }
    public String getUnionid() { return unionid; }
    public void setUnionid(String unionid) { this.unionid = unionid; }
}
