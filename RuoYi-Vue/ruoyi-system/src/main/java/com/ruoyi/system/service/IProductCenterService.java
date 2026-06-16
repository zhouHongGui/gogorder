package com.ruoyi.system.service;

import java.util.List;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.Product;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.StockLedger;
import com.ruoyi.system.domain.dto.ShopProductAssignRequest;
import com.ruoyi.system.domain.dto.ShopProductUpdateRequest;
import com.ruoyi.system.domain.dto.StockAdjustRequest;

/**
 * 商品中心服务契约：分类/规格/商品/门店商品的 CRUD + 库存管理。
 * 实现见 {@link com.ruoyi.system.service.impl.ProductCenterServiceImpl}（含库存原子扣减/恢复/幂等流水逻辑）。
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
    int updateShopProduct(Long id, ShopProductUpdateRequest request);
    /** 后台手工调整库存（设目标值，幂等 requestId）。 */
    StockLedger adjustStock(Long id, StockAdjustRequest request);
    /** 下单扣减库存（幂等键 {orderId}:{shopProductId}:DEDUCT）。 */
    StockLedger deductStock(Long orderId, Long shopProductId, Integer quantity);
    /** 取消/退款恢复库存（幂等键 {orderId}:{shopProductId}:RESTORE）。 */
    StockLedger restoreStock(Long orderId, Long shopProductId, Integer quantity);
    /** 批量下架门店商品（存在进行中订单引用时拒绝）。 */
    int deleteShopProducts(List<Long> ids);
}
