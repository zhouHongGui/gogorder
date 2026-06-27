package com.ruoyi.system.mapper;

import java.time.LocalDate;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.dto.BStatOverviewView;
import com.ruoyi.system.domain.dto.BStatProductRankView;
import com.ruoyi.system.domain.dto.BStatStatusCountView;

public interface BStatMapper
{
    BStatOverviewView selectOverview(@Param("shopId") Long shopId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<BStatStatusCountView> selectStatusCounts(@Param("shopId") Long shopId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);

    List<BStatProductRankView> selectProductRank(@Param("shopId") Long shopId,
            @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate,
            @Param("limit") int limit);
}
