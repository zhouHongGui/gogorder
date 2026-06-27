package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.dto.BShopProductPageView;
import com.ruoyi.system.domain.dto.BShopProductQuery;
import com.ruoyi.system.domain.dto.BShopProductSoldOutRequest;

/** 门店员工端商品管理服务。 */
public interface IBProductService
{
    List<Category> listCategories(Long shopId);

    BShopProductPageView listProducts(Long shopId, BShopProductQuery query);

    void updateStatus(Long shopId, Long shopProductId, Integer status, Long operatorId);

    void setSoldOut(Long shopId, Long shopProductId, BShopProductSoldOutRequest request, Long operatorId);
}
