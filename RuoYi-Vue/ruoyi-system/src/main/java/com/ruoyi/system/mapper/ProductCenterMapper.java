package com.ruoyi.system.mapper;

import java.util.List;
import org.apache.ibatis.annotations.Param;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.Product;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.StockLedger;
import com.ruoyi.system.domain.dto.CProductView;
import com.ruoyi.system.domain.dto.CProductCategoryView;

/**
 * 商品中心数据层
 */
public interface ProductCenterMapper
{
    List<Category> selectCategoryList(Category category);
    Category selectCategoryById(Long id);
    int insertCategory(Category category);
    int updateCategory(Category category);
    int deleteProductCategoriesByCategoryId(Long categoryId);
    int deleteCategoryById(Long id);

    List<SpecTemplate> selectSpecTemplateList(SpecTemplate template);
    SpecTemplate selectSpecTemplateById(Long id);
    SpecTemplate selectSpecTemplateByIdForUpdate(Long id);
    List<SpecTemplate> selectSpecTemplatesByIds(List<Long> ids);
    int insertSpecTemplate(SpecTemplate template);
    int updateSpecTemplate(SpecTemplate template);
    int countProductsByTemplateId(Long templateId);
    int countHistoricalOptionsByTemplateId(Long templateId);
    int deleteSpecOptionsByTemplateId(Long templateId);
    int deleteSpecTemplateById(Long id);

    List<SpecOption> selectSpecOptionList(Long templateId);
    SpecOption selectSpecOptionById(Long id);
    int countDefaultOptions(@Param("templateId") Long templateId, @Param("excludeId") Long excludeId);
    int insertSpecOption(SpecOption option);
    int updateSpecOption(SpecOption option);
    int disableSpecOption(Long id);
    int countHistoricalOptionReferences(String optionId);

    List<Product> selectProductList(Product product);
    Product selectProductById(Long id);
    int countProductsByIds(List<Long> ids);
    int countCategoriesByIds(List<Long> ids);
    int countSpecTemplatesByIds(List<Long> ids);
    int insertProduct(Product product);
    int updateProduct(Product product);
    int deleteProductCategoriesByProductId(Long productId);
    int insertProductCategories(@Param("productId") Long productId, @Param("categoryIds") List<Long> categoryIds);
    List<Long> selectCategoryIdsByProductId(Long productId);
    List<Category> selectCategoriesByProductId(Long productId);
    List<CProductCategoryView> selectCategoriesByProductIds(List<Long> productIds);
    int countShopProductsByProductId(Long productId);
    int deleteProductById(Long id);

    List<ShopProduct> selectShopProductList(Long shopId);
    ShopProduct selectShopProductById(Long id);
    ShopProduct selectActiveShopProductForCart(@Param("shopId") Long shopId, @Param("id") Long id);
    ShopProduct selectShopProductByIdForUpdate(Long id);
    int countShopById(Long shopId);
    List<Long> selectAssignedProductIds(@Param("shopId") Long shopId, @Param("productIds") List<Long> productIds);
    int assignShopProducts(@Param("shopId") Long shopId, @Param("productIds") List<Long> productIds);
    int updateShopProduct(ShopProduct shopProduct);
    int updateShopProductStock(@Param("id") Long id, @Param("stock") Integer stock);
    int deductShopProductStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    int restoreShopProductStock(@Param("id") Long id, @Param("quantity") Integer quantity);
    int deleteShopProducts(List<Long> ids);

    List<Category> selectCShopCategories(Long shopId);
    List<CProductView> selectCShopProducts(@Param("shopId") Long shopId, @Param("categoryId") Long categoryId,
            @Param("keyword") String keyword);
    CProductView selectCShopProductDetail(@Param("shopId") Long shopId, @Param("productId") Long productId);

    StockLedger selectStockLedgerByKey(String idempotentKey);
    int insertStockLedger(StockLedger ledger);
}
