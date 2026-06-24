package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.BShopContextView;
import com.ruoyi.system.domain.dto.StaffShopRequest;
import com.ruoyi.system.domain.dto.StaffShopUpdateRequest;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.mapper.StaffShopMapper;
import com.ruoyi.system.mapper.ShopStaffMapper;
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
    private ShopStaffMapper shopStaffMapper;

    @Override
    public List<StaffShop> selectStaffByShopId(Long shopId)
    {
        requireShop(shopId);
        return staffShopMapper.selectStaffByShopId(shopId);
    }

    @Override
    public List<BShopContextView> selectShopsByStaffId(Long staffId)
    {
        requireStaff(staffId);
        return staffShopMapper.selectShopContextsByStaffId(staffId);
    }

    @Override
    @Transactional
    public int insertStaffShop(Long shopId, StaffShopRequest request)
    {
        requireShop(shopId);
        requireStaff(request.getStaffId());
        if (staffShopMapper.selectStaffShop(shopId, request.getStaffId()) != null)
        {
            throw new ServiceException("该员工已关联此门店");
        }
        // 首个门店关联强制设为默认，保证每个员工至少有一个默认门店（登录依赖默认门店）。
        boolean firstShop = staffShopMapper.countShopsByStaffId(request.getStaffId()) == 0;
        Integer effectiveDefault = firstShop ? 1 : request.getIsDefault();
        if (Integer.valueOf(1).equals(effectiveDefault))
        {
            staffShopMapper.clearDefaultByStaffId(request.getStaffId());
        }
        StaffShop staffShop = toStaffShop(shopId, request);
        staffShop.setIsDefault(effectiveDefault);
        return staffShopMapper.insertStaffShop(staffShop);
    }

    @Override
    @Transactional
    public int updateStaffShop(Long shopId, Long staffId, StaffShopUpdateRequest request)
    {
        requireShop(shopId);
        StaffShop current = requireStaffShop(shopId, staffId);
        if (Integer.valueOf(1).equals(current.getIsDefault())
                && Integer.valueOf(0).equals(request.getIsDefault()))
        {
            throw new ServiceException("默认门店不能直接取消，请先指定新的默认门店");
        }
        if (Integer.valueOf(1).equals(request.getIsDefault()))
        {
            staffShopMapper.clearDefaultByStaffId(staffId);
        }
        current.setIsDefault(request.getIsDefault());
        return staffShopMapper.updateStaffShop(current);
    }

    @Override
    @Transactional
    public int deleteStaffShop(Long shopId, Long staffId)
    {
        requireShop(shopId);
        StaffShop current = requireStaffShop(shopId, staffId);
        if (Integer.valueOf(1).equals(current.getIsDefault()) && staffShopMapper.countShopsByStaffId(staffId) > 1)
        {
            throw new ServiceException("请先为该员工指定新的默认门店");
        }
        return staffShopMapper.deleteStaffShop(shopId, staffId);
    }

    @Override
    public void requireShopAccess(Long staffId, Long shopId)
    {
        if (staffId == null || shopId == null)
        {
            throw new ServiceException("门店授权参数不能为空", HttpStatus.BAD_REQUEST);
        }
        ShopStaff staff = requireStaff(staffId);
        if (!Integer.valueOf(0).equals(staff.getStatus()))
        {
            throw new ServiceException("员工账号已停用", HttpStatus.FORBIDDEN);
        }
        if (staffShopMapper.selectStaffShop(shopId, staffId) == null)
        {
            throw new ServiceException("无权访问当前门店", HttpStatus.FORBIDDEN);
        }
    }

    private StaffShop toStaffShop(Long shopId, StaffShopRequest request)
    {
        StaffShop staffShop = new StaffShop();
        staffShop.setShopId(shopId);
        staffShop.setStaffId(request.getStaffId());
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

    private ShopStaff requireStaff(Long staffId)
    {
        ShopStaff staff = shopStaffMapper.selectById(staffId);
        if (staff == null)
        {
            throw new ServiceException("员工不存在或已删除");
        }
        return staff;
    }

    private StaffShop requireStaffShop(Long shopId, Long staffId)
    {
        StaffShop staffShop = staffShopMapper.selectStaffShop(shopId, staffId);
        if (staffShop == null)
        {
            throw new ServiceException("员工未关联此门店");
        }
        return staffShop;
    }
}
