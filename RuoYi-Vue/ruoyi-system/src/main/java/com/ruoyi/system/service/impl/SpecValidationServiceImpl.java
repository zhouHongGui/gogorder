package com.ruoyi.system.service.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.dto.CProductView;
import com.ruoyi.system.domain.dto.CSpecView;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.domain.dto.SpecValidationResult;
import com.ruoyi.system.service.ICProductBrowseService;
import com.ruoyi.system.service.ISpecValidationService;

@Service
public class SpecValidationServiceImpl implements ISpecValidationService
{
    @Autowired
    private ICProductBrowseService productBrowseService;

    @Override
    public SpecValidationResult validateAndPrice(Long shopId, Long productId, Map<String, Object> specs)
    {
        CProductView product = productBrowseService.selectProductDetail(shopId, productId);
        if (product.isSoldOut())
        {
            throw new ServiceException("商品已售罄");
        }

        TreeMap<Long, List<String>> normalized = normalizeSpecs(specs);
        Map<Long, CSpecView> supportedSpecs = product.getSpecs().stream()
                .collect(Collectors.toMap(CSpecView::getTemplateId, item -> item));
        if (normalized.keySet().stream().anyMatch(templateId -> !supportedSpecs.containsKey(templateId)))
        {
            throw new ServiceException("商品不支持所选规格");
        }

        int unitPrice = StringUtils.nvl(product.getPrice(), 0);
        List<SpecSnapshot> snapshots = new ArrayList<>();
        for (CSpecView spec : product.getSpecs())
        {
            List<String> selectedIds = normalized.getOrDefault(spec.getTemplateId(), Collections.emptyList());
            validateSelectionCount(spec, selectedIds.size());
            Map<String, SpecOption> options = spec.getOptions().stream()
                    .collect(Collectors.toMap(SpecOption::getOptionId, option -> option));
            for (String optionId : selectedIds)
            {
                SpecOption option = options.get(optionId);
                if (option == null)
                {
                    throw new ServiceException("规格选项不存在或已禁用");
                }
                SpecSnapshot snapshot = new SpecSnapshot();
                snapshot.setTemplateId(spec.getTemplateId());
                snapshot.setTemplateName(spec.getName());
                snapshot.setOptionId(option.getOptionId());
                snapshot.setLabel(option.getLabel());
                snapshot.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), 0));
                snapshots.add(snapshot);
                unitPrice = Math.addExact(unitPrice, snapshot.getPriceAdd());
            }
        }

        SpecValidationResult result = new SpecValidationResult();
        result.setSelectedOptions(snapshots);
        result.setUnitPrice(unitPrice);
        result.setShopProductId(product.getShopProductId());
        result.setProductName(product.getName());
        result.setProductImage(product.getImage());
        result.setStock(StringUtils.nvl(product.getStock(), -1));
        TreeMap<String, List<String>> normalizedSnapshot = new TreeMap<>();
        normalized.forEach((templateId, optionIds) -> normalizedSnapshot.put(String.valueOf(templateId), optionIds));
        result.setNormalizedSpecs(normalizedSnapshot);
        return result;
    }

    private TreeMap<Long, List<String>> normalizeSpecs(Map<String, Object> specs)
    {
        TreeMap<Long, List<String>> normalized = new TreeMap<>();
        if (specs == null)
        {
            return normalized;
        }
        specs.forEach((key, value) -> {
            Long templateId;
            try
            {
                templateId = Long.valueOf(key);
            }
            catch (NumberFormatException e)
            {
                throw new ServiceException("规格模板ID格式不正确");
            }
            Collection<String> values;
            if (value instanceof String optionId)
            {
                values = Collections.singletonList(optionId);
            }
            else if (value instanceof Collection<?> collection)
            {
                if (collection.stream().anyMatch(item -> !(item instanceof String)))
                {
                    throw new ServiceException("规格选项ID必须为字符串");
                }
                values = collection.stream().map(String.class::cast).toList();
            }
            else
            {
                throw new ServiceException("规格选项必须为字符串或字符串数组");
            }
            List<String> optionIds = values.stream().filter(StringUtils::isNotEmpty).distinct().sorted().toList();
            if (!optionIds.isEmpty())
            {
                normalized.put(templateId, optionIds);
            }
        });
        return normalized;
    }

    private void validateSelectionCount(CSpecView spec, int count)
    {
        int min = StringUtils.nvl(spec.getMinSelect(), 0);
        int max = Math.max(1, StringUtils.nvl(spec.getMaxSelect(), 1));
        if (spec.isRequired() && count < Math.max(1, min))
        {
            throw new ServiceException("请选择" + spec.getName());
        }
        if (count > 0 && count < min)
        {
            throw new ServiceException(spec.getName() + "至少选择" + min + "项");
        }
        if (Integer.valueOf(1).equals(spec.getType()) && count > 1)
        {
            throw new ServiceException(spec.getName() + "只能选择一项");
        }
        if (count > max)
        {
            throw new ServiceException(spec.getName() + "最多选择" + max + "项");
        }
    }
}
