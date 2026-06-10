package com.ruoyi.system.service;

import java.math.BigDecimal;
import java.util.List;
import com.ruoyi.system.domain.dto.CPreorderSlot;
import com.ruoyi.system.domain.dto.CShopView;

/**
 * C端门店查询服务。
 */
public interface ICShopService
{
    List<CShopView> selectNearbyShops(BigDecimal longitude, BigDecimal latitude, String keyword);

    CShopView selectShopDetail(Long id);

    List<CPreorderSlot> selectPreorderSlots(Long id);
}
