package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.ShopStaffCreateRequest;
import com.ruoyi.system.domain.dto.ShopStaffUpdateRequest;

/** 独立门店员工账号服务。 */
public interface IShopStaffService
{
    List<ShopStaff> selectList(ShopStaff condition);
    ShopStaff selectById(Long id);
    int insert(ShopStaffCreateRequest request);
    int update(Long id, ShopStaffUpdateRequest request);
    int delete(Long id);
}
