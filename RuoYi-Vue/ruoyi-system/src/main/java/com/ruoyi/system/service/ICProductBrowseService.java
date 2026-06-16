package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.dto.CProductView;

/**
 * C 端商品菜单浏览服务契约。实现见 {@link com.ruoyi.system.service.impl.CProductBrowseServiceImpl}。
 */
public interface ICProductBrowseService
{
    /** 查询门店商品分类。 */
    List<Category> selectCategories(Long shopId);

    /** 查询门店商品列表（菜单），批量回填销量。 */
    List<CProductView> selectProducts(Long shopId, Long categoryId, String keyword);

    /** 查询商品详情（含完整规格）。 */
    CProductView selectProductDetail(Long shopId, Long productId);
}
