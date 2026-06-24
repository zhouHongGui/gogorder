package com.ruoyi.system.domain.dto;

import java.math.BigDecimal;
import java.time.LocalTime;
import com.fasterxml.jackson.annotation.JsonFormat;

/**
 * C端门店展示对象。
 */
public class CShopView
{
    /** 门店 ID。 */
    private Long id;
    /** 门店名。 */
    private String name;
    /** 门店图片。 */
    private String image;
    /** 联系电话。 */
    private String phone;
    /** 完整地址（省市区+详细）。 */
    private String address;
    /** 经度。 */
    private BigDecimal longitude;
    /** 纬度。 */
    private BigDecimal latitude;

    /** 营业开始时间（支持跨午夜）。 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime openTime;

    /** 营业结束时间（>openTime 表示跨午夜）。 */
    @JsonFormat(pattern = "HH:mm")
    private LocalTime closeTime;

    /** 门店状态：0休息 1营业 2暂停即时。 */
    private Integer status;
    /** 状态中文描述。 */
    private String statusName;
    /** 门店公告。 */
    private String notice;
    /** 每杯包装费（分/杯）。 */
    private Integer packFee;
    /** 最早预约分钟数。 */
    private Integer preorderMinMinutes;
    /** 最长预约天数。 */
    private Integer preorderMaxDays;
    /** 制作提前分钟数。 */
    private Integer makeLeadMinutes;
    /** 距调用点的距离（米，详情接口为 null）。 */
    private Long distance;
    /** 当前是否在营业时段。 */
    private boolean isOpen;
    /** 是否可下即时单（status=1 且在营业时段）。 */
    private boolean instantAvailable;
    /** 是否可下预订单（恒 true，所有门店均支持预订单）。 */
    private boolean preorderAvailable;
    /** 当前制作队列订单数（已接单/制作中）。 */
    private Integer makingQueueOrders;
    /** 当前制作队列杯数（已接单/制作中订单明细数量合计）。 */
    private Integer makingQueueCups;

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
    public Integer getMakingQueueOrders() { return makingQueueOrders; }
    public void setMakingQueueOrders(Integer makingQueueOrders) { this.makingQueueOrders = makingQueueOrders; }
    public Integer getMakingQueueCups() { return makingQueueCups; }
    public void setMakingQueueCups(Integer makingQueueCups) { this.makingQueueCups = makingQueueCups; }
}
