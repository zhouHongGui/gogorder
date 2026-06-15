package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.BizOrderItem;

public interface BizOrderItemMapper
{
    int batchInsert(@Param("items") List<BizOrderItem> items);
    List<BizOrderItem> selectByOrderId(Long orderId);
    List<BizOrderItem> selectByOrderIds(@Param("orderIds") List<Long> orderIds);
}
