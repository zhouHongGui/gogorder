package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.StaffShopRequest;
import com.ruoyi.system.domain.dto.StaffShopUpdateRequest;

/**
 * 员工-门店关联服务层
 */
public interface IStaffShopService
{
    public List<StaffShop> selectStaffByShopId(Long shopId);

    public int insertStaffShop(Long shopId, StaffShopRequest request);

    public int updateStaffShop(Long shopId, Long userId, StaffShopUpdateRequest request);

    public int deleteStaffShop(Long shopId, Long userId);
}
