package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 员工-门店关联实体（对应 {@code staff_shop}，多对多）。
 *
 * <p>一个员工（管理端用户）可关联多家门店：店员默认 1 家，店长可关联多家。
 * B 端操作必须校验门店越权（请求头 {@code X-Shop-Id} 必须在员工关联范围内）。
 *
 * <h3>关键约束</h3>
 * <ul>
 *   <li>{@code (userId, shopId)} 唯一。</li>
 *   <li>{@code isDefault}=1 表示默认门店（每个用户仅一个默认，靠 {@code default_user_id} 唯一索引保证）。</li>
 *   <li>userName/nickName/phonenumber/userStatus：JOIN sys_user 回填的展示字段。</li>
 * </ul>
 */
public class StaffShop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 员工（管理端用户）ID。 */
    private Long userId;
    /** 关联门店 ID。 */
    private Long shopId;
    /** 是否默认门店：1=是（每个用户唯一）。 */
    private Integer isDefault;
    /** 角色：店员/店长等。 */
    private String role;
    // 以下为 JOIN sys_user 回填的展示字段
    private String userName;
    private String nickName;
    private String phonenumber;
    private String userStatus;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getUserId()
    {
        return userId;
    }

    public void setUserId(Long userId)
    {
        this.userId = userId;
    }

    public Long getShopId()
    {
        return shopId;
    }

    public void setShopId(Long shopId)
    {
        this.shopId = shopId;
    }

    public Integer getIsDefault()
    {
        return isDefault;
    }

    public void setIsDefault(Integer isDefault)
    {
        this.isDefault = isDefault;
    }

    public String getRole()
    {
        return role;
    }

    public void setRole(String role)
    {
        this.role = role;
    }

    public String getUserName()
    {
        return userName;
    }

    public void setUserName(String userName)
    {
        this.userName = userName;
    }

    public String getNickName()
    {
        return nickName;
    }

    public void setNickName(String nickName)
    {
        this.nickName = nickName;
    }

    public String getPhonenumber()
    {
        return phonenumber;
    }

    public void setPhonenumber(String phonenumber)
    {
        this.phonenumber = phonenumber;
    }

    public String getUserStatus()
    {
        return userStatus;
    }

    public void setUserStatus(String userStatus)
    {
        this.userStatus = userStatus;
    }
}
