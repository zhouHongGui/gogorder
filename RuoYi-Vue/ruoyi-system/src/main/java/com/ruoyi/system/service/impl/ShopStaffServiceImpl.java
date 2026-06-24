package com.ruoyi.system.service.impl;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.ShopStaffCreateRequest;
import com.ruoyi.system.domain.dto.ShopStaffUpdateRequest;
import com.ruoyi.system.mapper.ShopStaffMapper;
import com.ruoyi.system.mapper.StaffShopMapper;
import com.ruoyi.system.service.IShopStaffService;

@Service
public class ShopStaffServiceImpl implements IShopStaffService
{
    @Autowired private ShopStaffMapper shopStaffMapper;
    @Autowired private StaffShopMapper staffShopMapper;

    @Override
    public List<ShopStaff> selectList(ShopStaff condition) { return shopStaffMapper.selectList(condition); }

    @Override
    public ShopStaff selectById(Long id)
    {
        ShopStaff staff = shopStaffMapper.selectById(id);
        if (staff == null) throw new ServiceException("员工不存在", HttpStatus.NOT_FOUND);
        return staff;
    }

    @Override
    @Transactional
    public int insert(ShopStaffCreateRequest request)
    {
        requireUnique(null, request.getAccount(), request.getPhone());
        ShopStaff staff = new ShopStaff();
        staff.setAccount(request.getAccount().trim());
        staff.setNickname(request.getNickname().trim());
        staff.setPhone(request.getPhone());
        staff.setPassword(SecurityUtils.encryptPassword(request.getPassword()));
        staff.setStatus(request.getStatus() == null ? 0 : request.getStatus());
        try { return shopStaffMapper.insert(staff); }
        catch (DuplicateKeyException e) { throw new ServiceException("员工账号或手机号已存在", HttpStatus.CONFLICT); }
    }

    @Override
    @Transactional
    public int update(Long id, ShopStaffUpdateRequest request)
    {
        ShopStaff current = selectById(id);
        requireUnique(id, current.getAccount(), request.getPhone());
        current.setNickname(request.getNickname().trim());
        current.setPhone(request.getPhone());
        current.setStatus(request.getStatus());
        current.setPassword(StringUtils.isEmpty(request.getPassword()) ? null
                : SecurityUtils.encryptPassword(request.getPassword()));
        try { return shopStaffMapper.update(current); }
        catch (DuplicateKeyException e) { throw new ServiceException("员工手机号已存在", HttpStatus.CONFLICT); }
    }

    @Override
    @Transactional
    public int delete(Long id)
    {
        selectById(id);
        if (staffShopMapper.countShopsByStaffId(id) > 0)
            throw new ServiceException("请先解除该员工的门店绑定", HttpStatus.CONFLICT);
        return shopStaffMapper.deleteById(id);
    }

    private void requireUnique(Long currentId, String account, String phone)
    {
        ShopStaff accountOwner = shopStaffMapper.selectByAccount(account);
        if (accountOwner != null && !accountOwner.getId().equals(currentId))
            throw new ServiceException("员工账号已存在", HttpStatus.CONFLICT);
        ShopStaff phoneOwner = shopStaffMapper.selectByPhone(phone);
        if (phoneOwner != null && !phoneOwner.getId().equals(currentId))
            throw new ServiceException("员工手机号已存在", HttpStatus.CONFLICT);
    }
}
