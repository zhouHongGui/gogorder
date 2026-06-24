package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.BShopContextView;

/**
 * 员工-门店关联数据层
 */
public interface StaffShopMapper
{
    public List<StaffShop> selectStaffByShopId(Long shopId);

    public StaffShop selectStaffShop(@Param("shopId") Long shopId, @Param("staffId") Long staffId);

    public List<BShopContextView> selectShopContextsByStaffId(Long staffId);

    public int countShopsByStaffId(Long staffId);

    public int insertStaffShop(StaffShop staffShop);

    public int updateStaffShop(StaffShop staffShop);

    public int clearDefaultByStaffId(Long staffId);

    public int deleteStaffShop(@Param("shopId") Long shopId, @Param("staffId") Long staffId);
}
