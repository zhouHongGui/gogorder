package com.ruoyi.system.domain;

import java.math.BigDecimal;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.ruoyi.common.core.domain.BaseEntity;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * 门店对象 shop
 */
public class Shop extends BaseEntity
{
    private static final long serialVersionUID = 1L;

    private Long id;

    @Size(max = 20, message = "门店编号长度不能超过20个字符")
    private String shopCode;

    @NotBlank(message = "门店名称不能为空")
    @Size(max = 100, message = "门店名称长度不能超过100个字符")
    private String name;

    @Size(max = 255, message = "门店图片地址长度不能超过255个字符")
    private String image;

    @NotBlank(message = "联系电话不能为空")
    @Size(max = 20, message = "联系电话长度不能超过20个字符")
    private String phone;

    @Size(max = 50, message = "省份长度不能超过50个字符")
    private String province;

    @Size(max = 50, message = "城市长度不能超过50个字符")
    private String city;

    @Size(max = 50, message = "区县长度不能超过50个字符")
    private String district;

    @Size(max = 255, message = "详细地址长度不能超过255个字符")
    private String address;

    @DecimalMin(value = "-180.0", message = "经度不能小于-180")
    @DecimalMax(value = "180.0", message = "经度不能大于180")
    private BigDecimal longitude;

    @DecimalMin(value = "-90.0", message = "纬度不能小于-90")
    @DecimalMax(value = "90.0", message = "纬度不能大于90")
    private BigDecimal latitude;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    @Min(value = 0, message = "最早预约分钟数不能小于0")
    private Integer preorderMinMinutes;

    @Min(value = 1, message = "最长预约天数不能小于1")
    private Integer preorderMaxDays;

    @Min(value = 0, message = "制作提前分钟数不能小于0")
    private Integer makeLeadMinutes;

    @Min(value = 0, message = "门店状态不正确")
    @Max(value = 2, message = "门店状态不正确")
    private Integer status;

    @Size(max = 500, message = "门店公告长度不能超过500个字符")
    private String notice;

    @Min(value = 0, message = "包装费不能小于0")
    private Integer packFee;

    @Min(value = 0, message = "排序号不能小于0")
    private Integer sortOrder;

    private Integer delFlag;

    public Long getId()
    {
        return id;
    }

    public void setId(Long id)
    {
        this.id = id;
    }

    public String getShopCode()
    {
        return shopCode;
    }

    public void setShopCode(String shopCode)
    {
        this.shopCode = shopCode;
    }

    public String getName()
    {
        return name;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getImage()
    {
        return image;
    }

    public void setImage(String image)
    {
        this.image = image;
    }

    public String getPhone()
    {
        return phone;
    }

    public void setPhone(String phone)
    {
        this.phone = phone;
    }

    public String getProvince()
    {
        return province;
    }

    public void setProvince(String province)
    {
        this.province = province;
    }

    public String getCity()
    {
        return city;
    }

    public void setCity(String city)
    {
        this.city = city;
    }

    public String getDistrict()
    {
        return district;
    }

    public void setDistrict(String district)
    {
        this.district = district;
    }

    public String getAddress()
    {
        return address;
    }

    public void setAddress(String address)
    {
        this.address = address;
    }

    public BigDecimal getLongitude()
    {
        return longitude;
    }

    public void setLongitude(BigDecimal longitude)
    {
        this.longitude = longitude;
    }

    public BigDecimal getLatitude()
    {
        return latitude;
    }

    public void setLatitude(BigDecimal latitude)
    {
        this.latitude = latitude;
    }

    public LocalTime getOpenTime()
    {
        return openTime;
    }

    public void setOpenTime(LocalTime openTime)
    {
        this.openTime = openTime;
    }

    public LocalTime getCloseTime()
    {
        return closeTime;
    }

    public void setCloseTime(LocalTime closeTime)
    {
        this.closeTime = closeTime;
    }

    public Integer getPreorderMinMinutes()
    {
        return preorderMinMinutes;
    }

    public void setPreorderMinMinutes(Integer preorderMinMinutes)
    {
        this.preorderMinMinutes = preorderMinMinutes;
    }

    public Integer getPreorderMaxDays()
    {
        return preorderMaxDays;
    }

    public void setPreorderMaxDays(Integer preorderMaxDays)
    {
        this.preorderMaxDays = preorderMaxDays;
    }

    public Integer getMakeLeadMinutes()
    {
        return makeLeadMinutes;
    }

    public void setMakeLeadMinutes(Integer makeLeadMinutes)
    {
        this.makeLeadMinutes = makeLeadMinutes;
    }

    public Integer getStatus()
    {
        return status;
    }

    public void setStatus(Integer status)
    {
        this.status = status;
    }

    public String getNotice()
    {
        return notice;
    }

    public void setNotice(String notice)
    {
        this.notice = notice;
    }

    public Integer getPackFee()
    {
        return packFee;
    }

    public void setPackFee(Integer packFee)
    {
        this.packFee = packFee;
    }

    public Integer getSortOrder()
    {
        return sortOrder;
    }

    public void setSortOrder(Integer sortOrder)
    {
        this.sortOrder = sortOrder;
    }

    public Integer getDelFlag()
    {
        return delFlag;
    }

    public void setDelFlag(Integer delFlag)
    {
        this.delFlag = delFlag;
    }

    /**
     * 判断指定时间是否处于营业时段，支持跨午夜营业。
     */
    public boolean isOpenAt(LocalTime time)
    {
        if (time == null || openTime == null || closeTime == null)
        {
            return false;
        }
        if (!openTime.isAfter(closeTime))
        {
            return !time.isBefore(openTime) && !time.isAfter(closeTime);
        }
        return !time.isBefore(openTime) || !time.isAfter(closeTime);
    }
}
