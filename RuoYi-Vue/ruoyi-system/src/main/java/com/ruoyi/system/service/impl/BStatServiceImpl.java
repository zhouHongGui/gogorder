package com.ruoyi.system.service.impl;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.system.domain.dto.BStatOverviewView;
import com.ruoyi.system.domain.dto.BStatStatusCountView;
import com.ruoyi.system.mapper.BStatMapper;
import com.ruoyi.system.service.IBStatService;

@Service
public class BStatServiceImpl implements IBStatService
{
    private static final int PRODUCT_RANK_LIMIT = 10;

    @Autowired
    private BStatMapper bStatMapper;

    @Override
    public BStatOverviewView getOverview(Long shopId, String range)
    {
        StatRange statRange = resolveRange(range);
        BStatOverviewView overview = bStatMapper.selectOverview(shopId, statRange.startDate(), statRange.endDate());
        if (overview == null)
        {
            overview = new BStatOverviewView();
        }
        overview.setRange(statRange.code());
        overview.setRangeDesc(statRange.desc());
        overview.setStartDate(statRange.startDate());
        overview.setEndDate(statRange.endDate());
        overview.setTurnoverAmount(defaultZero(overview.getTurnoverAmount()));
        overview.setOrderCount(defaultZero(overview.getOrderCount()));
        overview.setCupCount(defaultZero(overview.getCupCount()));
        overview.setAvgOrderAmount(overview.getOrderCount() == 0 ? 0
                : overview.getTurnoverAmount() / overview.getOrderCount());
        overview.setStatusCounts(fillStatusCounts(
                bStatMapper.selectStatusCounts(shopId, statRange.startDate(), statRange.endDate())));
        overview.setProductRanks(bStatMapper.selectProductRank(
                shopId, statRange.startDate(), statRange.endDate(), PRODUCT_RANK_LIMIT));
        return overview;
    }

    private List<BStatStatusCountView> fillStatusCounts(List<BStatStatusCountView> rows)
    {
        Map<Integer, Long> countMap = new LinkedHashMap<>();
        if (rows != null)
        {
            for (BStatStatusCountView row : rows)
            {
                countMap.put(row.getOrderStatus(), defaultZero(row.getCount()));
            }
        }
        List<BStatStatusCountView> result = new ArrayList<>();
        for (OrderStatusEnum status : OrderStatusEnum.values())
        {
            result.add(new BStatStatusCountView(status.getCode(), staffOrderStatusDesc(status),
                    countMap.getOrDefault(status.getCode(), 0L)));
        }
        return result;
    }

    private String staffOrderStatusDesc(OrderStatusEnum status)
    {
        return status == OrderStatusEnum.ACCEPTED ? "待制作" : status.getDesc();
    }

    private Long defaultZero(Long value)
    {
        return value == null ? 0L : value;
    }

    private StatRange resolveRange(String range)
    {
        LocalDate today = LocalDate.now();
        String normalized = range == null ? "TODAY" : range.trim().toUpperCase();
        return switch (normalized)
        {
            case "YESTERDAY" -> new StatRange("YESTERDAY", "昨日", today.minusDays(1), today.minusDays(1));
            case "LAST_7_DAYS", "LAST7" -> new StatRange("LAST_7_DAYS", "近7天", today.minusDays(6), today);
            default -> new StatRange("TODAY", "今日", today, today);
        };
    }

    private record StatRange(String code, String desc, LocalDate startDate, LocalDate endDate) {}
}
