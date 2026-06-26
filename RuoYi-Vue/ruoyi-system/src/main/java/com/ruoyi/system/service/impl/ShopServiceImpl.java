package com.ruoyi.system.service.impl;

import java.time.LocalTime;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.ruoyi.common.constant.UserConstants;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.IShopService;

/**
 * 门店服务层处理
 */
@Service
public class ShopServiceImpl implements IShopService
{
    @Autowired
    private ShopMapper shopMapper;

    @Override
    public List<Shop> selectShopList(Shop shop)
    {
        return shopMapper.selectShopList(shop);
    }

    @Override
    public Shop selectShopById(Long id)
    {
        return shopMapper.selectShopById(id);
    }

    @Override
    public boolean checkShopCodeUnique(Shop shop)
    {
        Long shopId = StringUtils.isNull(shop.getId()) ? -1L : shop.getId();
        Shop info = shopMapper.selectShopByCode(shop.getShopCode());
        if (StringUtils.isNotNull(info) && info.getId().longValue() != shopId.longValue())
        {
            return UserConstants.NOT_UNIQUE;
        }
        return UserConstants.UNIQUE;
    }

    @Override
    public int insertShop(Shop shop)
    {
        if (StringUtils.isBlank(shop.getShopCode()))
        {
            throw new ServiceException("门店编号不能为空");
        }
        if (!checkShopCodeUnique(shop))
        {
            throw new ServiceException("门店编号已存在");
        }
        applyDefaults(shop);
        return shopMapper.insertShop(shop);
    }

    @Override
    public int updateShop(Shop shop)
    {
        Shop current = requireShop(shop.getId());
        if (StringUtils.isNotEmpty(shop.getShopCode()) && !StringUtils.equals(shop.getShopCode(), current.getShopCode()))
        {
            throw new ServiceException("门店编号创建后不可修改");
        }
        shop.setShopCode(current.getShopCode());
        shop.setStatus(null);
        return shopMapper.updateShop(shop);
    }

    @Override
    public int updateShopStatus(Long id, Integer status)
    {
        requireShop(id);
        validateStatus(status);
        return shopMapper.updateShopStatus(id, status);
    }

    @Override
    @Transactional
    public int deleteShopById(Long id)
    {
        Shop shop = requireShop(id);
        if (shopMapper.countActiveOrdersByShopId(id) > 0)
        {
            throw new ServiceException("门店存在进行中订单，不能删除");
        }
        int rows = shopMapper.deleteShopById(id);
        if (rows == 0)
        {
            throw new ServiceException("门店'" + shop.getName() + "'删除失败");
        }
        return rows;
    }

    private Shop requireShop(Long id)
    {
        if (id == null)
        {
            throw new ServiceException("门店ID不能为空");
        }
        Shop shop = shopMapper.selectShopById(id);
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        return shop;
    }

    private void applyDefaults(Shop shop)
    {
        shop.setOpenTime(StringUtils.nvl(shop.getOpenTime(), LocalTime.of(10, 0)));
        shop.setCloseTime(StringUtils.nvl(shop.getCloseTime(), LocalTime.of(22, 0)));
        shop.setStatus(StringUtils.nvl(shop.getStatus(), 1));
        shop.setPreorderMinMinutes(StringUtils.nvl(shop.getPreorderMinMinutes(), 30));
        shop.setPreorderMaxDays(StringUtils.nvl(shop.getPreorderMaxDays(), 7));
        shop.setMakeLeadMinutes(StringUtils.nvl(shop.getMakeLeadMinutes(), 30));
        shop.setMinutesPerCup(StringUtils.nvl(shop.getMinutesPerCup(), 3));
        shop.setPackFee(StringUtils.nvl(shop.getPackFee(), 100));
        shop.setSortOrder(StringUtils.nvl(shop.getSortOrder(), 0));
        shop.setDelFlag(0);
        validateStatus(shop.getStatus());
    }

    private void validateStatus(Integer status)
    {
        if (status == null || status < 0 || status > 2)
        {
            throw new ServiceException("门店状态不正确");
        }
    }
}
