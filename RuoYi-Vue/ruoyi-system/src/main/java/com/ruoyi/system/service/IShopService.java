package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Shop;

/**
 * 门店服务层
 */
public interface IShopService
{
    public List<Shop> selectShopList(Shop shop);

    public Shop selectShopById(Long id);

    public boolean checkShopCodeUnique(Shop shop);

    public int insertShop(Shop shop);

    public int updateShop(Shop shop);

    public int updateShopStatus(Long id, Integer status);

    public int deleteShopById(Long id);
}
