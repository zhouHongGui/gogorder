package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.core.domain.entity.SysUser;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.StaffShopRequest;
import com.ruoyi.system.domain.dto.StaffShopUpdateRequest;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.mapper.StaffShopMapper;
import com.ruoyi.system.mapper.SysUserMapper;
import com.ruoyi.system.service.IStaffShopService;

/**
 * 员工-门店关联服务层处理
 */
@Service
public class StaffShopServiceImpl implements IStaffShopService
{
    @Autowired
    private StaffShopMapper staffShopMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private SysUserMapper userMapper;

    @Override
    public List<StaffShop> selectStaffByShopId(Long shopId)
    {
        requireShop(shopId);
        return staffShopMapper.selectStaffByShopId(shopId);
    }

    @Override
    @Transactional
    public int insertStaffShop(Long shopId, StaffShopRequest request)
    {
        requireShop(shopId);
        requireUser(request.getUserId());
        if (staffShopMapper.selectStaffShop(shopId, request.getUserId()) != null)
        {
            throw new ServiceException("该员工已关联此门店");
        }
        if (Integer.valueOf(1).equals(request.getIsDefault()))
        {
            staffShopMapper.clearDefaultByUserId(request.getUserId());
        }
        StaffShop staffShop = toStaffShop(shopId, request);
        return staffShopMapper.insertStaffShop(staffShop);
    }

    @Override
    @Transactional
    public int updateStaffShop(Long shopId, Long userId, StaffShopUpdateRequest request)
    {
        requireShop(shopId);
        StaffShop current = requireStaffShop(shopId, userId);
        if (Integer.valueOf(1).equals(request.getIsDefault()))
        {
            staffShopMapper.clearDefaultByUserId(userId);
        }
        current.setRole(request.getRole());
        current.setIsDefault(request.getIsDefault());
        return staffShopMapper.updateStaffShop(current);
    }

    @Override
    @Transactional
    public int deleteStaffShop(Long shopId, Long userId)
    {
        requireShop(shopId);
        StaffShop current = requireStaffShop(shopId, userId);
        if (Integer.valueOf(1).equals(current.getIsDefault()) && staffShopMapper.countShopsByUserId(userId) > 1)
        {
            throw new ServiceException("请先为该员工指定新的默认门店");
        }
        return staffShopMapper.deleteStaffShop(shopId, userId);
    }

    private StaffShop toStaffShop(Long shopId, StaffShopRequest request)
    {
        StaffShop staffShop = new StaffShop();
        staffShop.setShopId(shopId);
        staffShop.setUserId(request.getUserId());
        staffShop.setRole(request.getRole());
        staffShop.setIsDefault(request.getIsDefault());
        return staffShop;
    }

    private Shop requireShop(Long shopId)
    {
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        return shop;
    }

    private SysUser requireUser(Long userId)
    {
        SysUser user = userMapper.selectUserById(userId);
        if (user == null)
        {
            throw new ServiceException("员工不存在或已删除");
        }
        return user;
    }

    private StaffShop requireStaffShop(Long shopId, Long userId)
    {
        StaffShop staffShop = staffShopMapper.selectStaffShop(shopId, userId);
        if (staffShop == null)
        {
            throw new ServiceException("员工未关联此门店");
        }
        return staffShop;
    }
}
