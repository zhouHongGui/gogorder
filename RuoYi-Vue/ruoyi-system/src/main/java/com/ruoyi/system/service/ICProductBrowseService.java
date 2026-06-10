package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.dto.CProductView;

/**
 * C端商品菜单浏览服务。
 */
public interface ICProductBrowseService
{
    List<Category> selectCategories(Long shopId);

    List<CProductView> selectProducts(Long shopId, Long categoryId, String keyword);

    CProductView selectProductDetail(Long shopId, Long productId);
}
