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
 * C 端门店查询服务（公开，无需登录）。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li><b>附近门店</b>：按经纬度查询门店并计算距离（Haversine 球面距离），返回营业态/可即时单/可预订单等展示字段。</li>
 *   <li><b>门店详情</b>：组装门店完整信息供菜单页/下单页。</li>
 *   <li><b>预订单时段</b>：按门店营业时间 + 预约窗口生成可选的取餐时段（每 30 分钟一档）。</li>
 * </ul>
 *
 * <h3>业务要点（接手必读）</h3>
 * <ul>
 *   <li><b>营业时间跨午夜</b>：open_time > close_time 表示跨日（如 18:00–02:00），判断见 {@link Shop#isOpenAt}。</li>
 *   <li><b>即时单可用</b>：status=1（营业中）且当前在营业时段内。休息/暂停门店只能下预订单。</li>
 *   <li><b>预订单时段</b>：从「现在 + preorderMinMinutes」向上取整到半小时开始，到「现在 + preorderMaxDays」结束，
 *       只保留落在营业时段内的整点/半点。</li>
 * </ul>
 */
@Service
public class CShopServiceImpl implements ICShopService
{
    /** 地球半径（米），用于 Haversine 距离计算。 */
    private static final double EARTH_RADIUS_METERS = 6_371_000D;
    /** 附近门店返回上限。 */
    private static final int NEARBY_SHOP_LIMIT = 50;
    /** 时段时间格式。 */
    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    @Autowired
    private ShopMapper shopMapper;

    /**
     * 附近门店列表。校验经纬度后查询，按距离远近（DB 侧粗排）返回，并计算每家门店到调用点的距离。
     *
     * @param longitude 调用点经度（可选）
     * @param latitude  调用点纬度（可选）
     * @param keyword   名称关键字（可选）
     */
    @Override
    public List<CShopView> selectNearbyShops(BigDecimal longitude, BigDecimal latitude, String keyword)
    {
        validateLocation(longitude, latitude);
        String normalizedKeyword = StringUtils.trim(keyword);
        LocalTime now = LocalTime.now();
        return shopMapper.selectCNearbyShops(longitude, latitude, normalizedKeyword, NEARBY_SHOP_LIMIT).stream()
                .map(shop -> buildView(shop, longitude, latitude, now, false))
                .toList();
    }

    /** 门店详情（不计算距离）。 */
    @Override
    public CShopView selectShopDetail(Long id)
    {
        Shop shop = requireShop(id);
        return buildView(shop, null, null, LocalTime.now(), true);
    }

    /**
     * 生成预订单可选取餐时段：从「现在+最早预约分钟」向上取整到半小时，到「现在+最大预约天数」，
     * 逐日逐半小时枚举，过滤出落在营业时段内的档位。
     */
    @Override
    public List<CPreorderSlot> selectPreorderSlots(Long id)
    {
        Shop shop = requireShop(id);
        int minMinutes = StringUtils.nvl(shop.getPreorderMinMinutes(), 30);
        int maxDays = StringUtils.nvl(shop.getPreorderMaxDays(), 7);
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime start = ceilToHalfHour(now.plusMinutes(minMinutes));   // 起点向上取整到半小时
        LocalDateTime end = now.plusDays(maxDays);
        // 按天枚举，每天拆成 48 个半小时档，过滤窗口与营业时段。
        return start.toLocalDate().datesUntil(end.toLocalDate().plusDays(1))
                .flatMap(date -> buildDaySlots(shop, date, start, end).stream())
                .toList();
    }

    /** 枚举某一天的半小时档位，保留在 [start,end] 窗口内且营业时段开放的档位。 */
    private List<CPreorderSlot> buildDaySlots(Shop shop, LocalDate date, LocalDateTime start, LocalDateTime end)
    {
        return java.util.stream.IntStream.range(0, 48)   // 48 个半小时 = 24 小时
                .mapToObj(index -> date.atStartOfDay().plusMinutes(index * 30L))
                .filter(slot -> !slot.isBefore(start) && !slot.isAfter(end))
                .filter(slot -> shop.isOpenAt(slot.toLocalTime()))   // 仅营业时段
                .map(slot -> new CPreorderSlot(slot, dateLabel(slot.toLocalDate()), slot.format(TIME_FORMATTER)))
                .toList();
    }

    /**
     * 组装门店展示视图，含营业态判定与距离计算。
     *
     * @param longitude 调用点经度（详情接口传 null，不计算距离）
     */
    private CShopView buildView(Shop shop, BigDecimal longitude, BigDecimal latitude, LocalTime now, boolean includeQueue)
    {
        boolean withinBusinessHours = shop.isOpenAt(now);
        // 即时单可用 = 门店状态营业中(1) 且当前在营业时段。
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
        view.setPreorderAvailable(true);   // 预订单对所有门店开放
        if (includeQueue)
        {
            view.setMakingQueueOrders(shopMapper.countMakingQueueOrdersByShopId(shop.getId()));
            view.setMakingQueueCups(shopMapper.countMakingQueueCupsByShopId(shop.getId()));
        }
        else
        {
            view.setMakingQueueOrders(0);
            view.setMakingQueueCups(0);
        }
        return view;
    }

    /** 校验门店存在并返回。 */
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

    /** 拼接完整地址：省+市+区+详细地址。 */
    private String fullAddress(Shop shop)
    {
        return StringUtils.join(
                StringUtils.defaultString(shop.getProvince()),
                StringUtils.defaultString(shop.getCity()),
                StringUtils.defaultString(shop.getDistrict()),
                StringUtils.defaultString(shop.getAddress()));
    }

    /**
     * 门店状态中文描述。status：0=休息中，2=暂停即时接单（仅可预订单），1=营业中（再细分是否在营业时段）。
     */
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

    /**
     * Haversine 球面距离计算（米）。任一坐标缺失返回 null。
     */
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

    /**
     * 校验经纬度：要么都传、要么都不传；范围合法。
     *
     * @throws ServiceException 仅传一个 / 范围越界
     */
    private void validateLocation(BigDecimal longitude, BigDecimal latitude)
    {
        // 经纬度必须同时传或同时不传。
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

    /**
     * 向上取整到半小时（秒/纳秒清零），用于预订单时段起点对齐。
     */
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

    /** 预订单日期展示文案：今天/明天/「M月D日」。 */
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
