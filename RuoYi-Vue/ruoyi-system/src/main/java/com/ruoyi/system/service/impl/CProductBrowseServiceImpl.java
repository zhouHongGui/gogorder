package com.ruoyi.system.service.impl;

import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.dto.CProductView;
import com.ruoyi.system.domain.dto.CSpecView;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.ICProductBrowseService;

/**
 * C端商品菜单浏览服务实现。
 */
@Service
public class CProductBrowseServiceImpl implements ICProductBrowseService
{
    @Autowired
    private ProductCenterMapper productCenterMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public List<Category> selectCategories(Long shopId)
    {
        requireShop(shopId);
        return productCenterMapper.selectCShopCategories(shopId);
    }

    @Override
    public List<CProductView> selectProducts(Long shopId, Long categoryId, String keyword)
    {
        requireShop(shopId);
        Map<String, List<CSpecView>> specsCache = new HashMap<>();
        return productCenterMapper.selectCShopProducts(shopId, categoryId, StringUtils.trim(keyword)).stream()
                .map(product -> hydrateProductForList(product, specsCache))
                .toList();
    }

    @Override
    public CProductView selectProductDetail(Long shopId, Long productId)
    {
        requireShop(shopId);
        CProductView product = productCenterMapper.selectCShopProductDetail(shopId, productId);
        if (product == null)
        {
            throw new ServiceException("商品不存在、未上架或不属于当前门店");
        }
        hydrateProductBasics(product);
        product.setSpecs(buildSpecs(parseLongList(product.getSpecTemplateIdsJson())));
        product.setDisplayPrice(product.getPrice() + minimumRequiredPrice(product.getSpecs()));
        return product;
    }

    private CProductView hydrateProductForList(CProductView product, Map<String, List<CSpecView>> specsCache)
    {
        hydrateProductBasics(product);
        List<CSpecView> specs = specsCache.computeIfAbsent(
                StringUtils.defaultString(product.getSpecTemplateIdsJson()),
                key -> buildSpecs(parseLongList(key)));
        product.setDisplayPrice(product.getPrice() + minimumRequiredPrice(specs));
        product.setSpecs(Collections.emptyList());
        return product;
    }

    private void hydrateProductBasics(CProductView product)
    {
        product.setTags(parseStringList(product.getTagsJson()));
        product.setCategories(productCenterMapper.selectCategoriesByProductId(product.getProductId()));
        product.setSoldOut(Integer.valueOf(0).equals(product.getStock()));
    }

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

    private int minimumRequiredPrice(List<CSpecView> specs)
    {
        return specs.stream().filter(CSpecView::isRequired).mapToInt(spec -> {
            int count = Math.max(1, StringUtils.nvl(spec.getMinSelect(), 1));
            return spec.getOptions().stream()
                    .map(option -> StringUtils.nvl(option.getPriceAdd(), 0))
                    .sorted(Comparator.naturalOrder())
                    .limit(count)
                    .mapToInt(Integer::intValue)
                    .sum();
        }).sum();
    }

    private List<Long> parseLongList(String json)
    {
        return StringUtils.isEmpty(json) ? Collections.emptyList()
                : JSON.parseArray(json, Long.class);
    }

    private List<String> parseStringList(String json)
    {
        return StringUtils.isEmpty(json) ? Collections.emptyList()
                : JSON.parseArray(json, String.class);
    }

    private void requireShop(Long shopId)
    {
        if (shopId == null || shopMapper.selectShopById(shopId) == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
    }
}
