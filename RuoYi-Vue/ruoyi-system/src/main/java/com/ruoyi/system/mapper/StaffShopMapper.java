package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.StaffShop;

/**
 * 员工-门店关联数据层
 */
public interface StaffShopMapper
{
    public List<StaffShop> selectStaffByShopId(Long shopId);

    public StaffShop selectStaffShop(@Param("shopId") Long shopId, @Param("userId") Long userId);

    public int countShopsByUserId(Long userId);

    public int insertStaffShop(StaffShop staffShop);

    public int updateStaffShop(StaffShop staffShop);

    public int clearDefaultByUserId(Long userId);

    public int deleteStaffShop(@Param("shopId") Long shopId, @Param("userId") Long userId);
}
