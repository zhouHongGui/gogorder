package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.Product;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.StockLedger;
import com.ruoyi.system.domain.dto.ShopProductAssignRequest;
import com.ruoyi.system.domain.dto.StockAdjustRequest;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.service.IProductCenterService;

/**
 * 商品中心服务实现
 */
@Service
public class ProductCenterServiceImpl implements IProductCenterService
{
    @Autowired
    private ProductCenterMapper productCenterMapper;

    @Override
    public List<Category> selectCategoryList(Category category)
    {
        return productCenterMapper.selectCategoryList(category);
    }

    @Override
    public int insertCategory(Category category)
    {
        category.setSortOrder(StringUtils.nvl(category.getSortOrder(), 0));
        category.setStatus(StringUtils.nvl(category.getStatus(), 1));
        return productCenterMapper.insertCategory(category);
    }

    @Override
    public int updateCategory(Category category)
    {
        requireCategory(category.getId());
        category.setSortOrder(StringUtils.nvl(category.getSortOrder(), 0));
        category.setStatus(StringUtils.nvl(category.getStatus(), 1));
        return productCenterMapper.updateCategory(category);
    }

    @Override
    @Transactional
    public int deleteCategory(Long id)
    {
        requireCategory(id);
        productCenterMapper.deleteProductCategoriesByCategoryId(id);
        return productCenterMapper.deleteCategoryById(id);
    }

    @Override
    public List<SpecTemplate> selectSpecTemplateList(SpecTemplate template)
    {
        return productCenterMapper.selectSpecTemplateList(template);
    }

    @Override
    public int insertSpecTemplate(SpecTemplate template)
    {
        applySpecTemplateDefaults(template);
        validateSpecTemplate(template);
        return productCenterMapper.insertSpecTemplate(template);
    }

    @Override
    public int updateSpecTemplate(SpecTemplate template)
    {
        SpecTemplate current = requireSpecTemplate(template.getId());
        template.setSortOrder(StringUtils.nvl(template.getSortOrder(), current.getSortOrder()));
        template.setStatus(StringUtils.nvl(template.getStatus(), current.getStatus()));
        validateSpecTemplate(template);
        if (current.getDefaultOptionCount() > template.getMaxSelect())
        {
            throw new ServiceException("最多选择数量不能小于当前默认选项数量");
        }
        return productCenterMapper.updateSpecTemplate(template);
    }

    @Override
    @Transactional
    public int deleteSpecTemplate(Long id)
    {
        requireSpecTemplate(id);
        if (productCenterMapper.countProductsByTemplateId(id) > 0)
        {
            throw new ServiceException("规格模板已被商品引用，不能删除");
        }
        if (productCenterMapper.countHistoricalOptionsByTemplateId(id) > 0)
        {
            throw new ServiceException("规格选项已有历史订单引用，不能删除模板");
        }
        productCenterMapper.deleteSpecOptionsByTemplateId(id);
        return productCenterMapper.deleteSpecTemplateById(id);
    }

    @Override
    public List<SpecOption> selectSpecOptionList(Long templateId)
    {
        requireSpecTemplate(templateId);
        return productCenterMapper.selectSpecOptionList(templateId);
    }

    @Override
    public int insertSpecOption(SpecOption option)
    {
        option.setOptionId(IdUtils.fastSimpleUUID());
        option.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), 0));
        option.setIsDefault(StringUtils.nvl(option.getIsDefault(), 0));
        option.setSortOrder(StringUtils.nvl(option.getSortOrder(), 0));
        option.setStatus(StringUtils.nvl(option.getStatus(), 1));
        validateDefaultOption(option, null);
        return productCenterMapper.insertSpecOption(option);
    }

    @Override
    public int updateSpecOption(SpecOption option)
    {
        SpecOption current = requireSpecOption(option.getId());
        if (option.getTemplateId() != null && !option.getTemplateId().equals(current.getTemplateId()))
        {
            throw new ServiceException("规格选项所属模板创建后不可修改");
        }
        option.setTemplateId(current.getTemplateId());
        option.setOptionId(current.getOptionId());
        option.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), current.getPriceAdd()));
        option.setIsDefault(StringUtils.nvl(option.getIsDefault(), current.getIsDefault()));
        option.setSortOrder(StringUtils.nvl(option.getSortOrder(), current.getSortOrder()));
        option.setStatus(StringUtils.nvl(option.getStatus(), current.getStatus()));
        validateDefaultOption(option, option.getId());
        return productCenterMapper.updateSpecOption(option);
    }

    @Override
    public int disableSpecOption(Long id)
    {
        requireSpecOption(id);
        return productCenterMapper.disableSpecOption(id);
    }

    @Override
    public List<Product> selectProductList(Product product)
    {
        List<Product> list = productCenterMapper.selectProductList(product);
        list.forEach(this::hydrateProductJson);
        return list;
    }

    @Override
    public Product selectProductById(Long id)
    {
        Product product = requireProduct(id);
        hydrateProductJson(product);
        product.setCategoryIds(productCenterMapper.selectCategoryIdsByProductId(id));
        product.setCategories(productCenterMapper.selectCategoriesByProductId(id));
        if (StringUtils.isNotEmpty(product.getSpecTemplateIds()))
        {
            List<SpecTemplate> templates = productCenterMapper.selectSpecTemplateList(new SpecTemplate());
            templates.removeIf(item -> !product.getSpecTemplateIds().contains(item.getId()));
            product.setSpecTemplates(templates);
        }
        else
        {
            product.setSpecTemplates(Collections.emptyList());
        }
        return product;
    }

    @Override
    @Transactional
    public int insertProduct(Product product)
    {
        applyProductDefaults(product);
        validateProductRelations(product);
        serializeProductJson(product);
        int rows = productCenterMapper.insertProduct(product);
        replaceProductCategories(product);
        return rows;
    }

    @Override
    @Transactional
    public int updateProduct(Product product)
    {
        requireProduct(product.getId());
        applyProductDefaults(product);
        validateProductRelations(product);
        serializeProductJson(product);
        int rows = productCenterMapper.updateProduct(product);
        replaceProductCategories(product);
        return rows;
    }

    @Override
    @Transactional
    public int deleteProduct(Long id)
    {
        Product product = requireProduct(id);
        if (productCenterMapper.countShopProductsByProductId(id) > 0)
        {
            throw new ServiceException("商品已分配至门店，不能删除");
        }
        productCenterMapper.deleteProductCategoriesByProductId(id);
        int rows = productCenterMapper.deleteProductById(id);
        if (rows == 0)
        {
            throw new ServiceException("商品'" + product.getName() + "'删除失败");
        }
        return rows;
    }

    @Override
    public List<ShopProduct> selectShopProductList(Long shopId)
    {
        requireShop(shopId);
        return productCenterMapper.selectShopProductList(shopId);
    }

    @Override
    public int assignShopProducts(ShopProductAssignRequest request)
    {
        requireShop(request.getShopId());
        List<Long> productIds = distinctIds(request.getProductIds());
        if (productCenterMapper.countProductsByIds(productIds) != productIds.size())
        {
            throw new ServiceException("存在无效商品，无法分配");
        }
        return productCenterMapper.assignShopProducts(request.getShopId(), productIds);
    }

    @Override
    public int updateShopProduct(ShopProduct shopProduct)
    {
        ShopProduct current = requireShopProduct(shopProduct.getId());
        shopProduct.setPrice(shopProduct.getPrice());
        shopProduct.setStatus(StringUtils.nvl(shopProduct.getStatus(), current.getStatus()));
        shopProduct.setSortOrder(StringUtils.nvl(shopProduct.getSortOrder(), current.getSortOrder()));
        return productCenterMapper.updateShopProduct(shopProduct);
    }

    @Override
    @Transactional
    public StockLedger adjustStock(Long id, StockAdjustRequest request)
    {
        String idempotentKey = "adjust:" + request.getRequestId();
        StockLedger existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            return existing;
        }
        ShopProduct shopProduct = productCenterMapper.selectShopProductByIdForUpdate(id);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在");
        }
        existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            return existing;
        }
        int beforeStock = shopProduct.getStock();
        int afterStock = request.getStock();
        productCenterMapper.updateShopProductStock(id, afterStock);

        StockLedger ledger = new StockLedger();
        ledger.setShopProductId(id);
        ledger.setChangeType("ADJUST");
        ledger.setChangeAmount(beforeStock == -1 || afterStock == -1 ? 0 : afterStock - beforeStock);
        ledger.setBeforeStock(beforeStock);
        ledger.setAfterStock(afterStock);
        ledger.setIdempotentKey(idempotentKey);
        ledger.setReason(request.getReason());
        productCenterMapper.insertStockLedger(ledger);
        return ledger;
    }

    @Override
    public int deductStock(Long id, Integer quantity)
    {
        if (quantity == null || quantity <= 0)
        {
            throw new ServiceException("扣减数量必须大于0");
        }
        return productCenterMapper.deductShopProductStock(id, quantity);
    }

    @Override
    public int deleteShopProducts(List<Long> ids)
    {
        return productCenterMapper.deleteShopProducts(distinctIds(ids));
    }

    private Category requireCategory(Long id)
    {
        Category category = id == null ? null : productCenterMapper.selectCategoryById(id);
        if (category == null)
        {
            throw new ServiceException("商品分类不存在");
        }
        return category;
    }

    private SpecTemplate requireSpecTemplate(Long id)
    {
        SpecTemplate template = id == null ? null : productCenterMapper.selectSpecTemplateById(id);
        if (template == null)
        {
            throw new ServiceException("规格模板不存在");
        }
        return template;
    }

    private SpecOption requireSpecOption(Long id)
    {
        SpecOption option = id == null ? null : productCenterMapper.selectSpecOptionById(id);
        if (option == null)
        {
            throw new ServiceException("规格选项不存在");
        }
        return option;
    }

    private Product requireProduct(Long id)
    {
        Product product = id == null ? null : productCenterMapper.selectProductById(id);
        if (product == null)
        {
            throw new ServiceException("商品不存在");
        }
        return product;
    }

    private ShopProduct requireShopProduct(Long id)
    {
        ShopProduct shopProduct = id == null ? null : productCenterMapper.selectShopProductById(id);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在");
        }
        return shopProduct;
    }

    private void requireShop(Long shopId)
    {
        if (shopId == null || productCenterMapper.countShopById(shopId) == 0)
        {
            throw new ServiceException("门店不存在或已删除");
        }
    }

    private void applySpecTemplateDefaults(SpecTemplate template)
    {
        template.setIsRequired(StringUtils.nvl(template.getIsRequired(), 1));
        template.setMinSelect(StringUtils.nvl(template.getMinSelect(), 1));
        template.setMaxSelect(StringUtils.nvl(template.getMaxSelect(), 1));
        template.setSortOrder(StringUtils.nvl(template.getSortOrder(), 0));
        template.setStatus(StringUtils.nvl(template.getStatus(), 1));
    }

    private void validateSpecTemplate(SpecTemplate template)
    {
        if (template.getMinSelect() > template.getMaxSelect())
        {
            throw new ServiceException("最少选择数量不能大于最多选择数量");
        }
        if (Integer.valueOf(1).equals(template.getType()) && template.getMaxSelect() != 1)
        {
            throw new ServiceException("单选规格最多选择数量必须为1");
        }
        if (Integer.valueOf(1).equals(template.getIsRequired()) && template.getMinSelect() < 1)
        {
            throw new ServiceException("必选规格最少选择数量必须大于0");
        }
    }

    private void validateDefaultOption(SpecOption option, Long excludeId)
    {
        SpecTemplate template = requireSpecTemplate(option.getTemplateId());
        if (!Integer.valueOf(1).equals(option.getIsDefault()) || !Integer.valueOf(1).equals(option.getStatus()))
        {
            return;
        }
        int defaults = productCenterMapper.countDefaultOptions(option.getTemplateId(), excludeId) + 1;
        if (Integer.valueOf(1).equals(template.getType()) && defaults > 1)
        {
            throw new ServiceException("单选规格最多只能有一个默认选项");
        }
        if (defaults > template.getMaxSelect())
        {
            throw new ServiceException("默认选项数量不能超过最多选择数量");
        }
    }

    private void applyProductDefaults(Product product)
    {
        product.setImage(StringUtils.nvl(product.getImage(), ""));
        product.setDescription(StringUtils.nvl(product.getDescription(), ""));
        product.setBasePrice(StringUtils.nvl(product.getBasePrice(), 0));
        product.setStatus(StringUtils.nvl(product.getStatus(), 1));
        product.setImages(StringUtils.nvl(product.getImages(), new ArrayList<>()));
        product.setTags(StringUtils.nvl(product.getTags(), new ArrayList<>()));
        product.setSpecTemplateIds(distinctIds(StringUtils.nvl(product.getSpecTemplateIds(), new ArrayList<>())));
        product.setCategoryIds(distinctIds(StringUtils.nvl(product.getCategoryIds(), new ArrayList<>())));
    }

    private void validateProductRelations(Product product)
    {
        if (StringUtils.isNotEmpty(product.getCategoryIds())
                && productCenterMapper.countCategoriesByIds(product.getCategoryIds()) != product.getCategoryIds().size())
        {
            throw new ServiceException("存在无效商品分类");
        }
        if (StringUtils.isNotEmpty(product.getSpecTemplateIds())
                && productCenterMapper.countSpecTemplatesByIds(product.getSpecTemplateIds()) != product.getSpecTemplateIds().size())
        {
            throw new ServiceException("存在无效规格模板");
        }
    }

    private void replaceProductCategories(Product product)
    {
        productCenterMapper.deleteProductCategoriesByProductId(product.getId());
        if (StringUtils.isNotEmpty(product.getCategoryIds()))
        {
            productCenterMapper.insertProductCategories(product.getId(), product.getCategoryIds());
        }
    }

    private void serializeProductJson(Product product)
    {
        product.setImagesJson(JSON.toJSONString(product.getImages()));
        product.setTagsJson(JSON.toJSONString(product.getTags()));
        product.setSpecTemplateIdsJson(JSON.toJSONString(product.getSpecTemplateIds()));
    }

    private void hydrateProductJson(Product product)
    {
        product.setImages(parseStringList(product.getImagesJson()));
        product.setTags(parseStringList(product.getTagsJson()));
        product.setSpecTemplateIds(parseLongList(product.getSpecTemplateIdsJson()));
    }

    private List<String> parseStringList(String json)
    {
        return StringUtils.isEmpty(json) ? new ArrayList<>() : JSON.parseArray(json, String.class);
    }

    private List<Long> parseLongList(String json)
    {
        return StringUtils.isEmpty(json) ? new ArrayList<>() : JSON.parseArray(json, Long.class);
    }

    private <T> List<T> distinctIds(List<T> ids)
    {
        if (StringUtils.isEmpty(ids))
        {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }
}
