package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.dto.CPreorderSlot;
import com.ruoyi.system.domain.dto.CShopView;

/**
 * C 端门店查询服务契约。实现见 {@link com.ruoyi.system.service.impl.CShopServiceImpl}。
 */
public interface ICShopService
{
    /** 附近门店列表（按经纬度算距离，可按关键字筛选）。 */
    List<CShopView> selectNearbyShops(BigDecimal longitude, BigDecimal latitude, String keyword);

    /** 门店详情。 */
    CShopView selectShopDetail(Long id);

    /** 预订单可选取餐时段。 */
    List<CPreorderSlot> selectPreorderSlots(Long id);
}
