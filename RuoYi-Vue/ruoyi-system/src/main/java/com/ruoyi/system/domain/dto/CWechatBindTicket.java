package com.ruoyi.system.domain.dto;

import java.io.Serializable;

public class CWechatBindTicket implements Serializable
{
    private static final long serialVersionUID = 1L;

    private String openid;
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
