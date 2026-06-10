package com.ruoyi.system.domain.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * C端门店展示对象。
 */
public class CShopView
{
    private Long id;
    private String name;
    private String image;
    private String phone;
    private String address;
    private BigDecimal longitude;
    private BigDecimal latitude;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    private Integer status;
    private String statusName;
    private String notice;
    private Integer packFee;
    private Integer preorderMinMinutes;
    private Integer preorderMaxDays;
    private Integer makeLeadMinutes;
    private Long distance;
    private boolean isOpen;
    private boolean instantAvailable;
    private boolean preorderAvailable;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getImage() { return image; }
    public void setImage(String image) { this.image = image; }
    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }
    public String getAddress() { return address; }
    public void setAddress(String address) { this.address = address; }
    public BigDecimal getLongitude() { return longitude; }
    public void setLongitude(BigDecimal longitude) { this.longitude = longitude; }
    public BigDecimal getLatitude() { return latitude; }
    public void setLatitude(BigDecimal latitude) { this.latitude = latitude; }
    public LocalTime getOpenTime() { return openTime; }
    public void setOpenTime(LocalTime openTime) { this.openTime = openTime; }
    public LocalTime getCloseTime() { return closeTime; }
    public void setCloseTime(LocalTime closeTime) { this.closeTime = closeTime; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
    public String getStatusName() { return statusName; }
    public void setStatusName(String statusName) { this.statusName = statusName; }
    public String getNotice() { return notice; }
    public void setNotice(String notice) { this.notice = notice; }
    public Integer getPackFee() { return packFee; }
    public void setPackFee(Integer packFee) { this.packFee = packFee; }
    public Integer getPreorderMinMinutes() { return preorderMinMinutes; }
    public void setPreorderMinMinutes(Integer preorderMinMinutes) { this.preorderMinMinutes = preorderMinMinutes; }
    public Integer getPreorderMaxDays() { return preorderMaxDays; }
    public void setPreorderMaxDays(Integer preorderMaxDays) { this.preorderMaxDays = preorderMaxDays; }
    public Integer getMakeLeadMinutes() { return makeLeadMinutes; }
    public void setMakeLeadMinutes(Integer makeLeadMinutes) { this.makeLeadMinutes = makeLeadMinutes; }
    public Long getDistance() { return distance; }
    public void setDistance(Long distance) { this.distance = distance; }
    public boolean getIsOpen() { return isOpen; }
    public void setIsOpen(boolean isOpen) { this.isOpen = isOpen; }
    public boolean isInstantAvailable() { return instantAvailable; }
    public void setInstantAvailable(boolean instantAvailable) { this.instantAvailable = instantAvailable; }
    public boolean isPreorderAvailable() { return preorderAvailable; }
    public void setPreorderAvailable(boolean preorderAvailable) { this.preorderAvailable = preorderAvailable; }
}
