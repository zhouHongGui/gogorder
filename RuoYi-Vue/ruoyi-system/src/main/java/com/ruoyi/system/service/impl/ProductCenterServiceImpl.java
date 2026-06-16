package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import org.springframework.beans.factory.annotation.Autowired;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
import com.ruoyi.system.domain.dto.ShopProductUpdateRequest;
import com.ruoyi.system.domain.dto.StockAdjustRequest;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.service.IProductCenterService;

/**
 * 商品中心服务（管理后台 + 交易链路共用）。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li><b>分类 / 规格 / 商品 / 门店商品</b>的 CRUD（管理后台）。</li>
 *   <li><b>库存管理</b>：手工调整 {@link #adjustStock}、下单扣减 {@link #deductStock}、
 *       取消恢复 {@link #restoreStock}，全部带幂等流水 {@code stock_ledger}。</li>
 * </ul>
 *
 * <h3>库存模型（接手必读）</h3>
 * <ul>
 *   <li><b>商品级库存</b>：库存存在 {@code shop_product.stock}（门店×商品维度），不做 SKU/规格库存。</li>
 *   <li><b>-1 = 无限库存</b>：表示不限量，扣减/恢复都跳过实际数值变更。</li>
 *   <li><b>原子扣减</b>：用 {@code UPDATE ... WHERE stock >= qty} 条件更新防超卖，rows=0 即库存不足。</li>
 *   <li><b>幂等流水</b>：每次变更写 {@code stock_ledger}，幂等键
 *       {@code {orderId}:{shopProductId}:{DEDUCT|RESTORE}}（订单库存）或 {@code adjust:{requestId}}（手工调整）。
 *       重复变更会被识别并安全返回原流水。</li>
 *   <li><b>无限库存 + useAffectedRows</b>：数据源开启了 {@code useAffectedRows=true}，
 *       无变化的 UPDATE 返回 0 行。因此 -1→-1 的扣减不能走 UPDATE（会被误判为库存不足），
 *       代码里对 -1 单独跳过，详见 {@link #changeOrderStock}。</li>
 * </ul>
 *
 * <h3>规格不变量</h3>
 * 选项 {@code optionId} 全局唯一且创建后不可变；商品可引用多个规格模板；单选模板最多 1 个默认选项。
 */
@Service
public class ProductCenterServiceImpl implements IProductCenterService
{
    private static final Logger log = LoggerFactory.getLogger(ProductCenterServiceImpl.class);

    @Autowired
    private ProductCenterMapper productCenterMapper;

    // ==================== 分类 ====================

    /** 查询分类列表（管理后台）。 */
    @Override
    public List<Category> selectCategoryList(Category category)
    {
        return productCenterMapper.selectCategoryList(category);
    }

    /**
     * 新增分类。补默认值：排序 0、状态启用(1)。
     */
    @Override
    public int insertCategory(Category category)
    {
        category.setSortOrder(StringUtils.nvl(category.getSortOrder(), 0));
        category.setStatus(StringUtils.nvl(category.getStatus(), 1));
        return productCenterMapper.insertCategory(category);
    }

    /**
     * 修改分类。先校验存在，再补默认值。
     */
    @Override
    public int updateCategory(Category category)
    {
        requireCategory(category.getId());
        category.setSortOrder(StringUtils.nvl(category.getSortOrder(), 0));
        category.setStatus(StringUtils.nvl(category.getStatus(), 1));
        return productCenterMapper.updateCategory(category);
    }

    /**
     * 删除分类。单事务内：先删商品-分类关联，再删分类本身。
     */
    @Override
    @Transactional
    public int deleteCategory(Long id)
    {
        requireCategory(id);
        productCenterMapper.deleteProductCategoriesByCategoryId(id);
        return productCenterMapper.deleteCategoryById(id);
    }

    // ==================== 规格模板 ====================

    /** 查询规格模板列表（管理后台）。 */
    @Override
    public List<SpecTemplate> selectSpecTemplateList(SpecTemplate template)
    {
        return productCenterMapper.selectSpecTemplateList(template);
    }

    /**
     * 新增规格模板。补默认值并校验：minSelect ≤ maxSelect；单选模板 maxSelect 必须为 1；必选模板 minSelect ≥ 1。
     */
    @Override
    public int insertSpecTemplate(SpecTemplate template)
    {
        applySpecTemplateDefaults(template);
        validateSpecTemplate(template);
        return productCenterMapper.insertSpecTemplate(template);
    }

    /**
     * 修改规格模板。加锁读当前值补默认值，并校验「maxSelect 不能小于当前默认选项数」。
     */
    @Override
    @Transactional
    public int updateSpecTemplate(SpecTemplate template)
    {
        SpecTemplate current = requireSpecTemplateForUpdate(template.getId());
        template.setSortOrder(StringUtils.nvl(template.getSortOrder(), current.getSortOrder()));
        template.setStatus(StringUtils.nvl(template.getStatus(), current.getStatus()));
        validateSpecTemplate(template);
        // 收窄最多选择数时，不能小于已有默认选项数量，否则现有默认选项会超额。
        if (current.getDefaultOptionCount() > template.getMaxSelect())
        {
            throw new ServiceException("最多选择数量不能小于当前默认选项数量");
        }
        return productCenterMapper.updateSpecTemplate(template);
    }

    /**
     * 删除规格模板。被商品引用、或选项有历史订单引用时禁止删除（保证历史订单可回溯）。
     */
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

    // ==================== 规格选项 ====================

    /** 查询某模板下的规格选项列表（管理后台）。 */
    @Override
    public List<SpecOption> selectSpecOptionList(Long templateId)
    {
        requireSpecTemplate(templateId);
        return productCenterMapper.selectSpecOptionList(templateId);
    }

    /**
     * 新增规格选项。生成全局唯一 optionId（UUID），补默认值，并校验默认选项数量约束。
     */
    @Override
    @Transactional
    public int insertSpecOption(SpecOption option)
    {
        // optionId 全局唯一且不可变，用 UUID 生成。
        option.setOptionId(IdUtils.fastSimpleUUID());
        option.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), 0));
        option.setIsDefault(StringUtils.nvl(option.getIsDefault(), 0));
        option.setSortOrder(StringUtils.nvl(option.getSortOrder(), 0));
        option.setStatus(StringUtils.nvl(option.getStatus(), 1));
        validateDefaultOption(option, null);
        return productCenterMapper.insertSpecOption(option);
    }

    /**
     * 修改规格选项。模板归属创建后不可改；optionId 不可改；补默认值并校验默认选项约束。
     */
    @Override
    @Transactional
    public int updateSpecOption(SpecOption option)
    {
        SpecOption current = requireSpecOption(option.getId());
        // 禁止把选项挪到别的模板下。
        if (option.getTemplateId() != null && !option.getTemplateId().equals(current.getTemplateId()))
        {
            throw new ServiceException("规格选项所属模板创建后不可修改");
        }
        option.setTemplateId(current.getTemplateId());
        option.setOptionId(current.getOptionId());   // optionId 不可变
        option.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), current.getPriceAdd()));
        option.setIsDefault(StringUtils.nvl(option.getIsDefault(), current.getIsDefault()));
        option.setSortOrder(StringUtils.nvl(option.getSortOrder(), current.getSortOrder()));
        option.setStatus(StringUtils.nvl(option.getStatus(), current.getStatus()));
        validateDefaultOption(option, option.getId());
        return productCenterMapper.updateSpecOption(option);
    }

    /** 禁用规格选项（软禁用，保留历史订单引用）。 */
    @Override
    public int disableSpecOption(Long id)
    {
        requireSpecOption(id);
        return productCenterMapper.disableSpecOption(id);
    }

    // ==================== 商品 ====================

    /** 查询商品列表（管理后台），并反序列化 JSON 字段（images/tags/specTemplateIds）。 */
    @Override
    public List<Product> selectProductList(Product product)
    {
        List<Product> list = productCenterMapper.selectProductList(product);
        list.forEach(this::hydrateProductJson);
        return list;
    }

    /**
     * 查询商品详情：含分类列表、规格模板（按 specTemplateIds 关联）。
     */
    @Override
    public Product selectProductById(Long id)
    {
        Product product = requireProduct(id);
        hydrateProductJson(product);
        product.setCategoryIds(productCenterMapper.selectCategoryIdsByProductId(id));
        product.setCategories(productCenterMapper.selectCategoriesByProductId(id));
        if (StringUtils.isNotEmpty(product.getSpecTemplateIds()))
        {
            product.setSpecTemplates(productCenterMapper.selectSpecTemplatesByIds(product.getSpecTemplateIds()));
        }
        else
        {
            product.setSpecTemplates(Collections.emptyList());
        }
        return product;
    }

    /**
     * 新增商品。补默认值、校验关联（分类/规格模板有效）、序列化 JSON 字段、写商品-分类关联。
     */
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

    /**
     * 修改商品。校验存在后，同新增逻辑处理默认值/关联/JSON/分类关联。
     */
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

    /**
     * 删除商品。已分配到门店的商品禁止删除（避免悬挂的 shop_product）。
     * 单事务内：删商品-分类关联 + 删商品本身。
     */
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

    // ==================== 门店商品 ====================

    /** 查询门店商品列表（管理后台）。 */
    @Override
    public List<ShopProduct> selectShopProductList(Long shopId)
    {
        requireShop(shopId);
        return productCenterMapper.selectShopProductList(shopId);
    }

    /**
     * 批量分配商品到门店。先校验商品 ID 全部有效（数量须等于传入数量）。
     */
    @Override
    public int assignShopProducts(ShopProductAssignRequest request)
    {
        requireShop(request.getShopId());
        List<Long> productIds = distinctIds(request.getProductIds());
        // 商品 ID 去重后，数量必须与查到的有效商品数一致，否则存在无效商品。
        if (productCenterMapper.countProductsByIds(productIds) != productIds.size())
        {
            throw new ServiceException("存在无效商品，无法分配");
        }
        int rows = productCenterMapper.assignShopProducts(request.getShopId(), productIds);
        log.info("门店商品分配完成，shopId={}，productCount={}", request.getShopId(), productIds.size());
        return rows;
    }

    /**
     * 修改门店商品（售价/状态/排序）。互斥规则：使用基础价(useBasePrice) 时不能设门店售价；
     * useBasePrice=true 表示清空门店售价（回落到商品基础价）。
     */
    @Override
    public int updateShopProduct(Long id, ShopProductUpdateRequest request)
    {
        ShopProduct current = requireShopProduct(id);
        if (Boolean.TRUE.equals(request.getUseBasePrice()) && request.getPrice() != null)
        {
            throw new ServiceException("使用基础价时不能同时设置门店售价");
        }
        if (Boolean.TRUE.equals(request.getUseBasePrice()))
        {
            current.setPrice(null);   // 清空门店售价 → 下单时用商品基础价
        }
        else if (request.getPrice() != null)
        {
            current.setPrice(request.getPrice());
        }
        current.setStatus(StringUtils.nvl(request.getStatus(), current.getStatus()));
        current.setSortOrder(StringUtils.nvl(request.getSortOrder(), current.getSortOrder()));
        return productCenterMapper.updateShopProduct(current);
    }

    /**
     * 手工调整门店商品库存（管理后台）。幂等：相同 requestId 返回原调整记录。
     *
     * <p>注意：这里直接「设为目标库存」而非「增减」，changeAmount 记录前后差值。
     * 无限库存(-1)参与时 changeAmount 记 0。
     *
     * @param id      shop_product.id
     * @param request 含目标库存、原因、幂等 requestId
     * @return 库存流水记录
     */
    @Override
    @Transactional
    public StockLedger adjustStock(Long id, StockAdjustRequest request)
    {
        String idempotentKey = "adjust:" + request.getRequestId();
        // 幂等：相同 requestId 直接返回已有记录（并校验参数一致，防止 requestId 复用）。
        StockLedger existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            validateExistingAdjustment(existing, id, request);
            return existing;
        }
        // 加锁门店商品行，读当前库存。
        ShopProduct shopProduct = productCenterMapper.selectShopProductByIdForUpdate(id);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在");
        }
        // 加锁后再查一次幂等键（双重检查，防止加锁前另一事务已写入）。
        existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            validateExistingAdjustment(existing, id, request);
            return existing;
        }
        int beforeStock = shopProduct.getStock();
        int afterStock = request.getStock();
        productCenterMapper.updateShopProductStock(id, afterStock);

        StockLedger ledger = new StockLedger();
        ledger.setShopProductId(id);
        ledger.setChangeType("ADJUST");
        // 涉及无限库存(-1)时差值记 0（-1 与数值混算无意义）。
        ledger.setChangeAmount(beforeStock == -1 || afterStock == -1 ? 0 : afterStock - beforeStock);
        ledger.setBeforeStock(beforeStock);
        ledger.setAfterStock(afterStock);
        ledger.setIdempotentKey(idempotentKey);
        ledger.setReason(request.getReason());
        productCenterMapper.insertStockLedger(ledger);
        return ledger;
    }

    /**
     * 下单扣减库存（由订单服务调用）。幂等键 {@code {orderId}:{shopProductId}:DEDUCT}。
     */
    @Override
    @Transactional
    public StockLedger deductStock(Long orderId, Long shopProductId, Integer quantity)
    {
        return changeOrderStock(orderId, shopProductId, quantity, "DEDUCT");
    }

    /**
     * 取消/退款恢复库存（由取消服务调用）。幂等键 {@code {orderId}:{shopProductId}:RESTORE}。
     * 恢复前会校验「存在对应 DEDUCT 流水且数量一致」，防止凭空恢复。
     */
    @Override
    @Transactional
    public StockLedger restoreStock(Long orderId, Long shopProductId, Integer quantity)
    {
        return changeOrderStock(orderId, shopProductId, quantity, "RESTORE");
    }

    /**
     * 批量下架门店商品。存在「进行中订单」引用的门店商品禁止下架（避免悬挂订单明细）。
     */
    @Override
    @Transactional
    public int deleteShopProducts(List<Long> ids)
    {
        List<Long> shopProductIds = distinctIds(ids);
        if (shopProductIds.isEmpty())
        {
            return 0;
        }
        if (productCenterMapper.countActiveOrderItemsByShopProductIds(shopProductIds) > 0)
        {
            throw new ServiceException("存在进行中订单引用的门店商品，请先处理订单后再下架");
        }
        return productCenterMapper.deleteShopProducts(shopProductIds);
    }

    // ==================== 校验/工具方法 ====================

    /** 校验分类存在，不存在抛异常。 */
    private Category requireCategory(Long id)
    {
        Category category = id == null ? null : productCenterMapper.selectCategoryById(id);
        if (category == null)
        {
            throw new ServiceException("商品分类不存在");
        }
        return category;
    }

    /** 校验规格模板存在（无锁）。 */
    private SpecTemplate requireSpecTemplate(Long id)
    {
        SpecTemplate template = id == null ? null : productCenterMapper.selectSpecTemplateById(id);
        if (template == null)
        {
            throw new ServiceException("规格模板不存在");
        }
        return template;
    }

    /** 加锁校验规格模板存在（用于修改场景）。返回无锁的当前值供读取默认值。 */
    private SpecTemplate requireSpecTemplateForUpdate(Long id)
    {
        SpecTemplate locked = id == null ? null : productCenterMapper.selectSpecTemplateByIdForUpdate(id);
        if (locked == null)
        {
            throw new ServiceException("规格模板不存在");
        }
        return productCenterMapper.selectSpecTemplateById(id);
    }

    /** 校验规格选项存在。 */
    private SpecOption requireSpecOption(Long id)
    {
        SpecOption option = id == null ? null : productCenterMapper.selectSpecOptionById(id);
        if (option == null)
        {
            throw new ServiceException("规格选项不存在");
        }
        return option;
    }

    /** 校验商品存在。 */
    private Product requireProduct(Long id)
    {
        Product product = id == null ? null : productCenterMapper.selectProductById(id);
        if (product == null)
        {
            throw new ServiceException("商品不存在");
        }
        return product;
    }

    /** 校验门店商品存在。 */
    private ShopProduct requireShopProduct(Long id)
    {
        ShopProduct shopProduct = id == null ? null : productCenterMapper.selectShopProductById(id);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在");
        }
        return shopProduct;
    }

    /** 校验门店存在（count 方式，不返回实体）。 */
    private void requireShop(Long shopId)
    {
        if (shopId == null || productCenterMapper.countShopById(shopId) == 0)
        {
            throw new ServiceException("门店不存在或已删除");
        }
    }

    /** 规格模板补默认值：必选/最少/最多选择数、排序、状态。 */
    private void applySpecTemplateDefaults(SpecTemplate template)
    {
        template.setIsRequired(StringUtils.nvl(template.getIsRequired(), 1));
        template.setMinSelect(StringUtils.nvl(template.getMinSelect(), 1));
        template.setMaxSelect(StringUtils.nvl(template.getMaxSelect(), 1));
        template.setSortOrder(StringUtils.nvl(template.getSortOrder(), 0));
        template.setStatus(StringUtils.nvl(template.getStatus(), 1));
    }

    /**
     * 校验规格模板规则：min≤max；单选模板 max 必须=1；必选模板 min≥1。
     */
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

    /**
     * 校验默认选项数量约束：单选模板最多 1 个默认；默认选项总数不超过 maxSelect。
     *
     * @param excludeId 排除自身（修改场景），新增时传 null
     */
    private void validateDefaultOption(SpecOption option, Long excludeId)
    {
        SpecTemplate template = requireSpecTemplateForUpdate(option.getTemplateId());
        // 只有「设为默认且启用」的选项才参与计数。
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

    /** 商品补默认值（图片/描述/基础价/状态/JSON 字段/分类/规格模板去重）。 */
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

    /** 校验商品关联的分类、规格模板 ID 全部有效（数量必须匹配）。 */
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

    /** 重写商品的分类关联：先删后插。 */
    private void replaceProductCategories(Product product)
    {
        productCenterMapper.deleteProductCategoriesByProductId(product.getId());
        if (StringUtils.isNotEmpty(product.getCategoryIds()))
        {
            productCenterMapper.insertProductCategories(product.getId(), product.getCategoryIds());
        }
    }

    /** 把商品的 List 字段序列化为 JSON 字符串入库。 */
    private void serializeProductJson(Product product)
    {
        product.setImagesJson(JSON.toJSONString(product.getImages()));
        product.setTagsJson(JSON.toJSONString(product.getTags()));
        product.setSpecTemplateIdsJson(JSON.toJSONString(product.getSpecTemplateIds()));
    }

    /** 把商品的 JSON 字符串字段反序列化为 List。 */
    private void hydrateProductJson(Product product)
    {
        product.setImages(parseStringList(product.getImagesJson()));
        product.setTags(parseStringList(product.getTagsJson()));
        product.setSpecTemplateIds(parseLongList(product.getSpecTemplateIdsJson()));
    }

    /** JSON 字符串 → List<String>，空串返回空列表。 */
    private List<String> parseStringList(String json)
    {
        return StringUtils.isEmpty(json) ? new ArrayList<>() : JSON.parseArray(json, String.class);
    }

    /** JSON 字符串 → List<Long>，空串返回空列表。 */
    private List<Long> parseLongList(String json)
    {
        return StringUtils.isEmpty(json) ? new ArrayList<>() : JSON.parseArray(json, Long.class);
    }

    /** 列表去重并保留顺序（LinkedHashSet）。 */
    private <T> List<T> distinctIds(List<T> ids)
    {
        if (StringUtils.isEmpty(ids))
        {
            return new ArrayList<>();
        }
        return new ArrayList<>(new LinkedHashSet<>(ids));
    }

    /**
     * 订单库存变更核心方法（DEDUCT 扣减 / RESTORE 恢复）。
     *
     * <p><b>幂等</b>：幂等键 {@code {orderId}:{shopProductId}:{changeType}}。已存在则校验参数一致后返回原流水。
     *
     * <p><b>无限库存(-1)特殊处理</b>：扣减/恢复都不改变数值（-1→-1），因此跳过 UPDATE。
     * 原因：数据源开启了 {@code useAffectedRows=true}，无变化的 UPDATE 返回 0 行，
     * 若不跳过会被误判为「库存不足 / 恢复失败」。changeAmount 记 0。
     *
     * <p><b>恢复前置校验</b>：RESTORE 时要求存在对应的 DEDUCT 流水且数量一致，防止凭空恢复。
     *
     * @param orderId     订单 ID
     * @param shopProductId 门店商品 ID
     * @param quantity    变更数量（必须 >0）
     * @param changeType  DEDUCT 或 RESTORE
     * @return 库存流水
     * @throws ServiceException 商品不存在/库存不足/数量与原扣减不一致/幂等键冲突
     */
    private StockLedger changeOrderStock(Long orderId, Long shopProductId, Integer quantity, String changeType)
    {
        if (orderId == null)
        {
            throw new ServiceException("订单ID不能为空");
        }
        if (shopProductId == null)
        {
            throw new ServiceException("门店商品ID不能为空");
        }
        if (quantity == null || quantity <= 0)
        {
            throw new ServiceException("库存变更数量必须大于0");
        }
        // 恢复前：必须有对应的扣减流水，且恢复数量与扣减数量一致（防凭空恢复/超量恢复）。
        if ("RESTORE".equals(changeType))
        {
            StockLedger deduction = productCenterMapper.selectStockLedgerByKey(
                    orderId + ":" + shopProductId + ":DEDUCT");
            if (deduction == null)
            {
                throw new ServiceException("未找到对应的库存扣减流水");
            }
            if (deduction.getBeforeStock() != -1 && !Objects.equals(deduction.getChangeAmount(), -quantity))
            {
                throw new ServiceException("库存恢复数量与原扣减数量不一致");
            }
        }

        // 幂等：查已有流水，命中则校验参数一致后直接返回。
        String idempotentKey = orderId + ":" + shopProductId + ":" + changeType;
        StockLedger existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            validateExistingOrderStock(existing, orderId, shopProductId, quantity, changeType);
            return existing;
        }

        // 加锁门店商品行。
        ShopProduct shopProduct = productCenterMapper.selectShopProductByIdForUpdate(shopProductId);
        if (shopProduct == null)
        {
            throw new ServiceException("门店商品不存在");
        }
        // 加锁后再次查幂等键（双重检查）。
        existing = productCenterMapper.selectStockLedgerByKey(idempotentKey);
        if (existing != null)
        {
            validateExistingOrderStock(existing, orderId, shopProductId, quantity, changeType);
            return existing;
        }

        int beforeStock = shopProduct.getStock();
        int rows;
        int afterStock;
        if (beforeStock == -1)
        {
            // 无限库存：-1→-1 无变化，跳过 UPDATE（否则 useAffectedRows 下返回 0 行被误判失败）。
            rows = 1;
            afterStock = -1;
        }
        else if ("DEDUCT".equals(changeType))
        {
            // 条件 UPDATE 原子扣减：WHERE stock >= qty，rows=0 即库存不足（并发下可能发生）。
            rows = productCenterMapper.deductShopProductStock(shopProductId, quantity);
            if (rows == 0)
            {
                throw new ServiceException("商品库存不足");
            }
            afterStock = beforeStock - quantity;
        }
        else
        {
            // 恢复：库存 += quantity。
            rows = productCenterMapper.restoreShopProductStock(shopProductId, quantity);
            if (rows == 0)
            {
                throw new ServiceException("恢复商品库存失败");
            }
            afterStock = beforeStock + quantity;
        }

        // 写库存流水。无限库存 changeAmount 记 0。
        StockLedger ledger = new StockLedger();
        ledger.setShopProductId(shopProductId);
        ledger.setChangeType(changeType);
        ledger.setChangeAmount(beforeStock == -1 ? 0 : "DEDUCT".equals(changeType) ? -quantity : quantity);
        ledger.setBeforeStock(beforeStock);
        ledger.setAfterStock(afterStock);
        ledger.setOrderId(orderId);
        ledger.setIdempotentKey(idempotentKey);
        ledger.setReason("");
        productCenterMapper.insertStockLedger(ledger);
        return ledger;
    }

    /** 校验已存在的手工调整流水与本次请求参数一致（防幂等键被不同参数复用）。 */
    private void validateExistingAdjustment(StockLedger existing, Long shopProductId, StockAdjustRequest request)
    {
        if (!Objects.equals(existing.getShopProductId(), shopProductId)
                || !Objects.equals(existing.getAfterStock(), request.getStock())
                || !Objects.equals(existing.getReason(), request.getReason()))
        {
            throw new ServiceException("库存调整请求ID已被其他参数使用");
        }
    }

    /** 校验已存在的订单库存流水与本次参数一致（防幂等键被不同参数复用）。 */
    private void validateExistingOrderStock(StockLedger existing, Long orderId, Long shopProductId,
            Integer quantity, String changeType)
    {
        int expectedAmount = existing.getBeforeStock() == -1 ? 0 : "DEDUCT".equals(changeType) ? -quantity : quantity;
        if (!Objects.equals(existing.getOrderId(), orderId)
                || !Objects.equals(existing.getShopProductId(), shopProductId)
                || !Objects.equals(existing.getChangeType(), changeType)
                || !Objects.equals(existing.getChangeAmount(), expectedAmount))
        {
            throw new ServiceException("库存变更幂等键已被其他参数使用");
        }
    }
}
