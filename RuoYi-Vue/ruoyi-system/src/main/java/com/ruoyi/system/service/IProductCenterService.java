package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.Product;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.StockLedger;
import com.ruoyi.system.domain.dto.ShopProductAssignRequest;
import com.ruoyi.system.domain.dto.StockAdjustRequest;

/**
 * 商品中心服务
 */
public interface IProductCenterService
{
    List<Category> selectCategoryList(Category category);
    int insertCategory(Category category);
    int updateCategory(Category category);
    int deleteCategory(Long id);

    List<SpecTemplate> selectSpecTemplateList(SpecTemplate template);
    int insertSpecTemplate(SpecTemplate template);
    int updateSpecTemplate(SpecTemplate template);
    int deleteSpecTemplate(Long id);

    List<SpecOption> selectSpecOptionList(Long templateId);
    int insertSpecOption(SpecOption option);
    int updateSpecOption(SpecOption option);
    int disableSpecOption(Long id);

    List<Product> selectProductList(Product product);
    Product selectProductById(Long id);
    int insertProduct(Product product);
    int updateProduct(Product product);
    int deleteProduct(Long id);

    List<ShopProduct> selectShopProductList(Long shopId);
    int assignShopProducts(ShopProductAssignRequest request);
    int updateShopProduct(ShopProduct shopProduct);
    StockLedger adjustStock(Long id, StockAdjustRequest request);
    int deductStock(Long id, Integer quantity);
    int deleteShopProducts(List<Long> ids);
}
