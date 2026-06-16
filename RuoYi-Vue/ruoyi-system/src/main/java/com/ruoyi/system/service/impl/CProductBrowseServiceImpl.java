package com.ruoyi.system.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.dto.CProductCategoryView;
import com.ruoyi.system.domain.dto.CProductSalesView;
import com.ruoyi.system.domain.dto.CProductView;
import com.ruoyi.system.domain.dto.CSpecView;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.ICProductBrowseService;

/**
 * C 端商品菜单浏览服务（公开，无需登录）。
 *
 * <h3>职责（接手必读）</h3>
 * <ul>
 *   <li>组装门店菜单视图：商品列表 + 分类 + 规格 + 近 30 天销量 + 「售罄/起售价」等展示字段。</li>
 *   <li>商品详情：含完整规格模板与启用选项，供加购/下单选择规格。</li>
 * </ul>
 *
 * <h3>性能要点</h3>
 * <ul>
 *   <li><b>销量批量查询</b>：{@link #hydrateMonthlySales} 用一条 GROUP BY 查询批量回填所有商品的近 30 天销量，
 *       而非逐商品相关子查询（原性能问题已优化）。</li>
 *   <li><b>规格缓存</b>：列表组装时同 specTemplateIds 的商品共用一份规格构建结果（{@code specsCache}）。</li>
 * </ul>
 *
 * <h3>展示字段</h3>
 * <ul>
 *   <li>{@code soldOut}：库存为 0 视为售罄（-1 无限库存永不售罄）。</li>
 *   <li>{@code displayPrice}：展示起售价 = 基础价 + 必选规格的最小加价（{@link #minimumRequiredPrice}）。</li>
 * </ul>
 */
@Service
public class CProductBrowseServiceImpl implements ICProductBrowseService
{
    @Autowired
    private ProductCenterMapper productCenterMapper;

    @Autowired
    private ShopMapper shopMapper;

    /** 查询门店下的商品分类列表（用于菜单顶部筛选）。 */
    @Override
    public List<Category> selectCategories(Long shopId)
    {
        requireShop(shopId);
        return productCenterMapper.selectCShopCategories(shopId);
    }

    /**
     * 查询门店商品列表（菜单）。组装分类、规格、销量、售罄态、起售价。
     *
     * @param shopId    门店
     * @param categoryId 可选，按分类筛选
     * @param keyword   可选，按名称/描述模糊搜索
     */
    @Override
    public List<CProductView> selectProducts(Long shopId, Long categoryId, String keyword)
    {
        requireShop(shopId);
        // 查门店在售商品（基础信息 + 规格模板ID JSON + 库存 + 价格）。
        List<CProductView> products = productCenterMapper.selectCShopProducts(shopId, categoryId, StringUtils.trim(keyword));
        // 批量回填近 30 天销量（单条 GROUP BY，非逐行子查询）。
        hydrateMonthlySales(shopId, products);
        // 批量查商品分类，按 productId 分组。
        Map<Long, List<Category>> categoriesByProductId = products.isEmpty() ? Collections.emptyMap()
                : productCenterMapper.selectCategoriesByProductIds(
                        products.stream().map(CProductView::getProductId).distinct().toList()).stream()
                        .collect(Collectors.groupingBy(
                                CProductCategoryView::getProductId,
                                Collectors.mapping(CProductCategoryView::toCategory, Collectors.toList())));
        // 同 specTemplateIds 的商品共用规格构建结果，避免重复查库。
        Map<String, List<CSpecView>> specsCache = new HashMap<>();
        return products.stream()
                .map(product -> hydrateProductForList(product, specsCache,
                        categoriesByProductId.getOrDefault(product.getProductId(), Collections.emptyList())))
                .toList();
    }

    /**
     * 商品详情：含完整规格模板与启用选项，供加购/下单选规格。
     *
     * @throws ServiceException 商品不存在/未上架/不属于该门店
     */
    @Override
    public CProductView selectProductDetail(Long shopId, Long productId)
    {
        requireShop(shopId);
        CProductView product = productCenterMapper.selectCShopProductDetail(shopId, productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在、未上架或不属于当前门店");
        }
        hydrateMonthlySales(shopId, List.of(product));
        hydrateProductBasics(product, productCenterMapper.selectCategoriesByProductId(product.getProductId()));
        // 构建完整规格（模板 + 启用选项），详情页用于规格选择。
        product.setSpecs(buildSpecs(parseLongList(product.getSpecTemplateIdsJson())));
        product.setHasSpecs(!product.getSpecs().isEmpty());
        // 展示起售价 = 基础价 + 必选规格最小加价。
        product.setDisplayPrice(product.getPrice() + minimumRequiredPrice(product.getSpecs()));
        return product;
    }

    /**
     * 批量回填近 30 天销量：一条 GROUP BY 查询取出本批商品的销量，缺数据的商品记 0。
     */
    private void hydrateMonthlySales(Long shopId, List<CProductView> products)
    {
        if (products.isEmpty())
        {
            return;
        }
        Map<Long, Long> salesByProductId = productCenterMapper.selectCProductMonthlySales(shopId,
                products.stream().map(CProductView::getProductId).distinct().toList()).stream()
                .collect(Collectors.toMap(CProductSalesView::getProductId, CProductSalesView::getMonthlySales));
        products.forEach(product -> product.setMonthlySales(
                salesByProductId.getOrDefault(product.getProductId(), 0L)));
    }

    /**
     * 组装列表展示用的商品视图。注意：列表页不返回完整规格（数据量大），
     * 仅算 hasSpecs / displayPrice；完整规格走详情接口。
     */
    private CProductView hydrateProductForList(CProductView product, Map<String, List<CSpecView>> specsCache,
            List<Category> categories)
    {
        hydrateProductBasics(product, categories);
        // 复用同规格模板的商品的构建结果。
        List<CSpecView> specs = specsCache.computeIfAbsent(
                StringUtils.defaultString(product.getSpecTemplateIdsJson()),
                key -> buildSpecs(parseLongList(key)));
        product.setHasSpecs(!specs.isEmpty());
        product.setDisplayPrice(product.getPrice() + minimumRequiredPrice(specs));
        product.setSpecs(Collections.emptyList());   // 列表不返回完整规格
        return product;
    }

    /** 回填基础展示字段：标签、分类、售罄态（库存为 0 即售罄）。 */
    private void hydrateProductBasics(CProductView product, List<Category> categories)
    {
        product.setTags(parseStringList(product.getTagsJson()));
        product.setCategories(categories);
        product.setSoldOut(Integer.valueOf(0).equals(product.getStock()));
    }

    /**
     * 按模板 ID 构建规格视图列表，只含启用的模板。
     */
    private List<CSpecView> buildSpecs(List<Long> templateIds)
    {
        if (StringUtils.isEmpty(templateIds))
        {
            return Collections.emptyList();
        }
        return productCenterMapper.selectSpecTemplatesByIds(templateIds).stream()
                .filter(template -> Integer.valueOf(1).equals(template.getStatus()))
                .map(this::buildSpec)
                .toList();
    }

    /** 构建单个规格视图：模板信息 + 启用选项列表（禁用选项过滤掉）。 */
    private CSpecView buildSpec(SpecTemplate template)
    {
        List<SpecOption> options = productCenterMapper.selectSpecOptionList(template.getId()).stream()
                .filter(option -> Integer.valueOf(1).equals(option.getStatus()))
                .toList();
        CSpecView spec = new CSpecView();
        spec.setTemplateId(template.getId());
        spec.setName(template.getName());
        spec.setType(template.getType());
        spec.setRequired(Integer.valueOf(1).equals(template.getIsRequired()));
        spec.setMinSelect(template.getMinSelect());
        spec.setMaxSelect(template.getMaxSelect());
        spec.setOptions(options);
        return spec;
    }

    /**
     * 计算必选规格的最小加价：每个必选规格取「最少选择数」个最便宜的选项的加价之和。
     * 用于商品列表/详情的「起售价」展示。
     */
    private int minimumRequiredPrice(List<CSpecView> specs)
    {
        return specs.stream().filter(CSpecView::isRequired).mapToInt(spec -> {
            int count = Math.max(1, StringUtils.nvl(spec.getMinSelect(), 1));
            return spec.getOptions().stream()
                    .map(option -> StringUtils.nvl(option.getPriceAdd(), 0))
                    .sorted(Comparator.naturalOrder())   // 取最便宜的 count 个
                    .limit(count)
                    .mapToInt(Integer::intValue)
                    .sum();
        }).sum();
    }

    /** JSON 字符串 → List<Long>。 */
    private List<Long> parseLongList(String json)
    {
        return StringUtils.isEmpty(json) ? Collections.emptyList()
                : JSON.parseArray(json, Long.class);
    }

    /** JSON 字符串 → List<String>。 */
    private List<String> parseStringList(String json)
    {
        return StringUtils.isEmpty(json) ? Collections.emptyList()
                : JSON.parseArray(json, String.class);
    }

    /** 校验门店存在（不存在抛异常）。 */
    private void requireShop(Long shopId)
    {
        if (shopId == null || shopMapper.selectShopById(shopId) == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
    }
}
