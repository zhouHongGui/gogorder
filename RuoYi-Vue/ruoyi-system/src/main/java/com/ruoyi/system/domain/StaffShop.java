package com.ruoyi.system.domain;

import com.ruoyi.common.core.domain.BaseEntity;

/**
 * 员工-门店关联实体（对应 {@code staff_shop}，多对多）。
 *
 * <p>一个独立门店员工可关联多家门店。
 * B 端操作必须校验门店越权（请求头 {@code X-Shop-Id} 必须在员工关联范围内）。
 *
 * <h3>关键约束</h3>
 * <ul>
 *   <li>{@code (staffId, shopId)} 唯一。</li>
 *   <li>{@code isDefault}=1 表示默认门店（每个员工仅一个默认，靠 {@code default_staff_id} 唯一索引保证）。</li>
 *   <li>account/nickname/phone/staffStatus：JOIN shop_staff 回填的展示字段。</li>
 * </ul>
 */
public class StaffShop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;
    /** 独立门店员工 ID。 */
    private Long staffId;
    /** 关联门店 ID。 */
    private Long shopId;
    /** 是否默认门店：1=是（每个用户唯一）。 */
    private Integer isDefault;
    // 以下为 JOIN shop_staff 回填的展示字段
    private String account;
    private String nickname;
    private String phone;
    private Integer staffStatus;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public Long getStaffId()
    {
        return staffId;
    }

    public void setStaffId(Long staffId)
    {
        this.staffId = staffId;
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

    public String getAccount()
    {
        return account;
    }

    public void setAccount(String account)
    {
        this.account = account;
    }

    public String getNickname()
    {
        return nickname;
    }

    public void setNickname(String nickname)
    {
        this.nickname = nickname;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public Integer getStaffStatus()
    {
        return staffStatus;
    }

    public void setStaffStatus(Integer staffStatus)
    {
        this.staffStatus = staffStatus;
    }
}
