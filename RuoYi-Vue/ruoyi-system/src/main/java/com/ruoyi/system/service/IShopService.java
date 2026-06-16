package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Shop;

/**
 * 门店管理服务契约（管理后台 CRUD + 状态切换）。门店为逻辑删除。
 */
public interface IShopService
{
    public List<Shop> selectShopList(Shop shop);

    public Shop selectShopById(Long id);

    /** 校验门店编号唯一。 */
    public boolean checkShopCodeUnique(Shop shop);

    public int insertShop(Shop shop);

    public int updateShop(Shop shop);

    /** 切换门店状态：0休息 1营业 2暂停即时。 */
    public int updateShopStatus(Long id, Integer status);

    /** 逻辑删除门店（保留历史订单引用）。 */
    public int deleteShopById(Long id);
}
