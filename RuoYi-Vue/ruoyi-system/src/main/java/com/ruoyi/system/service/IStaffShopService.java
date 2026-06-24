package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.BShopContextView;
import com.ruoyi.system.domain.dto.StaffShopRequest;
import com.ruoyi.system.domain.dto.StaffShopUpdateRequest;

/**
 * 员工-门店关联服务契约（staff_shop 多对多）。员工可关联多家门店，一个为默认。
 */
public interface IStaffShopService
{
    public List<StaffShop> selectStaffByShopId(Long shopId);

    public List<BShopContextView> selectShopsByStaffId(Long staffId);

    public int insertStaffShop(Long shopId, StaffShopRequest request);

    public int updateStaffShop(Long shopId, Long staffId, StaffShopUpdateRequest request);

    public int deleteStaffShop(Long shopId, Long staffId);

    /**
     * 校验独立门店员工是否可访问指定门店。
     *
     * @param staffId 当前员工 ID
     * @param shopId 当前门店 ID
     * @throws com.ruoyi.common.exception.ServiceException 用户停用、门店未授权或参数无效
     */
    public void requireShopAccess(Long staffId, Long shopId);
}
