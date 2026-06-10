package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Shop;

/**
 * 门店数据层
 */
public interface ShopMapper
{
    public List<Shop> selectShopList(Shop shop);

    public Shop selectShopById(Long id);

    public Shop selectShopByCode(String shopCode);

    public int insertShop(Shop shop);

    public int updateShop(Shop shop);

    public int updateShopStatus(@Param("id") Long id, @Param("status") Integer status);

    public int deleteShopById(Long id);

    public int countActiveOrdersByShopId(Long shopId);
}
