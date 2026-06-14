package com.ruoyi.system.service.impl;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.dto.CPreorderSlot;
import com.ruoyi.system.domain.dto.CShopView;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.ICShopService;

/**
 * C端门店查询服务实现。
 */
@Service
public class CShopServiceImpl implements ICShopService
{
    private static final double EARTH_RADIUS_METERS = 6_371_000D;
    private static final int NEARBY_SHOP_LIMIT = 50;
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public List<CShopView> selectNearbyShops(BigDecimal longitude, BigDecimal latitude, String keyword)
    {
        validateLocation(longitude, latitude);
        String normalizedKeyword = StringUtils.trim(keyword);
        LocalTime now = LocalTime.now();
        return shopMapper.selectCNearbyShops(longitude, latitude, normalizedKeyword, NEARBY_SHOP_LIMIT).stream()
                .map(shop -> buildView(shop, longitude, latitude, now))
                .toList();
    }

    @Override
    public CShopView selectShopDetail(Long id)
    {
        Shop shop = requireShop(id);
        return buildView(shop, null, null, LocalTime.now());
    }

    @Override
    public List<CPreorderSlot> selectPreorderSlots(Long id)
    {
        Shop shop = requireShop(id);
        int minMinutes = StringUtils.nvl(shop.getPreorderMinMinutes(), 30);
        int maxDays = StringUtils.nvl(shop.getPreorderMaxDays(), 7);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = ceilToHalfHour(now.plusMinutes(minMinutes));
        LocalDateTime end = now.plusDays(maxDays);
        return start.toLocalDate().datesUntil(end.toLocalDate().plusDays(1))
                .flatMap(date -> buildDaySlots(shop, date, start, end).stream())
                .toList();
    }

    private List<CPreorderSlot> buildDaySlots(Shop shop, LocalDate date, LocalDateTime start, LocalDateTime end)
    {
        return java.util.stream.IntStream.range(0, 48)
                .mapToObj(index -> date.atStartOfDay().plusMinutes(index * 30L))
                .filter(slot -> !slot.isBefore(start) && !slot.isAfter(end))
                .filter(slot -> shop.isOpenAt(slot.toLocalTime()))
                .map(slot -> new CPreorderSlot(slot, dateLabel(slot.toLocalDate()), slot.format(TIME_FORMATTER)))
                .toList();
    }

    private CShopView buildView(Shop shop, BigDecimal longitude, BigDecimal latitude, LocalTime now)
    {
        boolean withinBusinessHours = shop.isOpenAt(now);
        boolean instantAvailable = Integer.valueOf(1).equals(shop.getStatus()) && withinBusinessHours;
        CShopView view = new CShopView();
        view.setId(shop.getId());
        view.setName(shop.getName());
        view.setImage(shop.getImage());
        view.setPhone(shop.getPhone());
        view.setAddress(fullAddress(shop));
        view.setLongitude(shop.getLongitude());
        view.setLatitude(shop.getLatitude());
        view.setOpenTime(shop.getOpenTime());
        view.setCloseTime(shop.getCloseTime());
        view.setStatus(shop.getStatus());
        view.setStatusName(statusName(shop.getStatus(), withinBusinessHours));
        view.setNotice(shop.getNotice());
        view.setPackFee(shop.getPackFee());
        view.setPreorderMinMinutes(shop.getPreorderMinMinutes());
        view.setPreorderMaxDays(shop.getPreorderMaxDays());
        view.setMakeLeadMinutes(shop.getMakeLeadMinutes());
        view.setDistance(distance(longitude, latitude, shop.getLongitude(), shop.getLatitude()));
        view.setIsOpen(withinBusinessHours);
        view.setInstantAvailable(instantAvailable);
        view.setPreorderAvailable(true);
        return view;
    }

    private Shop requireShop(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("门店ID不能为空");
        }
        Shop shop = shopMapper.selectShopById(id);
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        return shop;
    }

    private String fullAddress(Shop shop)
    {
        return StringUtils.join(
                StringUtils.defaultString(shop.getProvince()),
                StringUtils.defaultString(shop.getCity()),
                StringUtils.defaultString(shop.getDistrict()),
                StringUtils.defaultString(shop.getAddress()));
    }

    private String statusName(Integer status, boolean withinBusinessHours)
    {
        if (Integer.valueOf(0).equals(status))
        {
            return "休息中";
        }
        if (Integer.valueOf(2).equals(status))
        {
            return "暂停即时接单";
        }
        return withinBusinessHours ? "营业中" : "非营业时间";
    }

    private Long distance(BigDecimal fromLongitude, BigDecimal fromLatitude,
            BigDecimal toLongitude, BigDecimal toLatitude)
    {
        if (fromLongitude == null || fromLatitude == null || toLongitude == null || toLatitude == null)
        {
            return null;
        }
        double latitudeDistance = Math.toRadians(toLatitude.doubleValue() - fromLatitude.doubleValue());
        double longitudeDistance = Math.toRadians(toLongitude.doubleValue() - fromLongitude.doubleValue());
        double startLatitude = Math.toRadians(fromLatitude.doubleValue());
        double endLatitude = Math.toRadians(toLatitude.doubleValue());
        double haversine = Math.sin(latitudeDistance / 2) * Math.sin(latitudeDistance / 2)
                + Math.cos(startLatitude) * Math.cos(endLatitude)
                * Math.sin(longitudeDistance / 2) * Math.sin(longitudeDistance / 2);
        return BigDecimal.valueOf(2 * EARTH_RADIUS_METERS * Math.asin(Math.sqrt(haversine)))
                .setScale(0, RoundingMode.HALF_UP)
                .longValue();
    }

    private void validateLocation(BigDecimal longitude, BigDecimal latitude)
    {
        if ((longitude == null) != (latitude == null))
        {
            throw new ServiceException("经纬度必须同时传入");
        }
        if (longitude != null && (longitude.compareTo(BigDecimal.valueOf(-180)) < 0
                || longitude.compareTo(BigDecimal.valueOf(180)) > 0
                || latitude.compareTo(BigDecimal.valueOf(-90)) < 0
                || latitude.compareTo(BigDecimal.valueOf(90)) > 0))
        {
            throw new ServiceException("经纬度范围不正确");
        }
    }

    private LocalDateTime ceilToHalfHour(LocalDateTime value)
    {
        LocalDateTime minute = value.withSecond(0).withNano(0);
        int remainder = minute.getMinute() % 30;
        if (remainder == 0)
        {
            return minute;
        }
        return minute.plusMinutes(30 - remainder);
    }

    private String dateLabel(LocalDate date)
    {
        LocalDate today = LocalDate.now();
        if (date.equals(today))
        {
            return "今天";
        }
        if (date.equals(today.plusDays(1)))
        {
            return "明天";
        }
        return date.getMonthValue() + "月" + date.getDayOfMonth() + "日";
    }
}
