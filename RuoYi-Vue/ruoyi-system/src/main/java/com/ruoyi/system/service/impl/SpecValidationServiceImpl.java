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

/**
 * 规格校验与定价服务（下单/加购共用）。
 *
 * <h3>职责（接手必读）</h3>
 * <ul>
 *   <li><b>不信任前端</b>：根据 (shopId, productId) 查商品当前规格，校验前端传入的规格是否合法，
 *       并由后端重新计算单价（基础价 + 各选项加价），杜绝前端篡改价格。</li>
 *   <li><b>归一化规格</b>：把前端传入的 specs 归一化为 {@code {templateId → [optionId]}}（TreeMap 排序），
 *       保证「同商品同规格但顺序不同」得到相同 key，用于购物车合并与幂等。</li>
 *   <li><b>输入大小防护</b>：对规格组数、每组选项数、key/optionId 长度做上限校验，
 *       防止超大 payload 攻击（与 DTO 层 {@code @Size} 互为纵深防御）。</li>
 *   <li><b>快照</b>：返回完整的规格快照（templateId/optionId/label/priceAdd），供订单明细持久化。</li>
 * </ul>
 */
@Service
public class SpecValidationServiceImpl implements ISpecValidationService
{
    @Autowired
    private ICProductBrowseService productBrowseService;

    /**
     * 校验规格并定价。
     *
     * @param shopId    门店 ID
     * @param productId 商品 ID
     * @param specs     前端传入的规格，key=templateId，value=单个 optionId 或 optionId 数组
     * @return 校验结果（含单价、门店商品 ID、商品快照、库存、归一化规格、规格快照）
     * @throws ServiceException 商品售罄/不支持所选规格/选项不存在/选择数不合规
     */
    @Override
    public SpecValidationResult validateAndPrice(Long shopId, Long productId, Map<String, Object> specs)
    {
        // 查门店下的商品视图（含规格、库存、售价）。
        CProductView product = productBrowseService.selectProductDetail(shopId, productId);
        if (product.isSoldOut())
        {
            throw new ServiceException("商品已售罄");
        }

        // 归一化前端传入的规格为 {templateId → [optionId]}（TreeMap 保证顺序稳定）。
        TreeMap<Long, List<String>> normalized = normalizeSpecs(specs);
        // 商品支持的规格模板集合，用于校验前端传入的 templateId 是否合法。
        Map<Long, CSpecView> supportedSpecs = product.getSpecs().stream()
                .collect(Collectors.toMap(CSpecView::getTemplateId, item -> item));
        if (normalized.keySet().stream().anyMatch(templateId -> !supportedSpecs.containsKey(templateId)))
        {
            throw new ServiceException("商品不支持所选规格");
        }

        // 单价 = 商品基础价 + 各选中选项的加价（后端重算，不信任前端）。
        int unitPrice = StringUtils.nvl(product.getPrice(), 0);
        List<SpecSnapshot> snapshots = new ArrayList<>();
        for (CSpecView spec : product.getSpecs())
        {
            List<String> selectedIds = normalized.getOrDefault(spec.getTemplateId(), Collections.emptyList());
            // 校验该规格模板的选择数（必选/最少/最多/单选）。
            validateSelectionCount(spec, selectedIds.size());
            // 该模板下的所有选项，按 optionId 建索引便于查选中的选项。
            Map<String, SpecOption> options = spec.getOptions().stream()
                    .collect(Collectors.toMap(SpecOption::getOptionId, option -> option));
            for (String optionId : selectedIds)
            {
                SpecOption option = options.get(optionId);
                if (option == null)
                {
                    // 选项不存在或已禁用（被过滤掉）。
                    throw new ServiceException("规格选项不存在或已禁用");
                }
                // 构造规格快照（订单明细持久化用，保证日后改价不影响历史订单）。
                SpecSnapshot snapshot = new SpecSnapshot();
                snapshot.setTemplateId(spec.getTemplateId());
                snapshot.setTemplateName(spec.getName());
                snapshot.setOptionId(option.getOptionId());
                snapshot.setLabel(option.getLabel());
                snapshot.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), 0));
                snapshots.add(snapshot);
                unitPrice = Math.addExact(unitPrice, snapshot.getPriceAdd());   // 累加选项加价
            }
        }

        // 组装校验结果。
        SpecValidationResult result = new SpecValidationResult();
        result.setSelectedOptions(snapshots);
        result.setUnitPrice(unitPrice);
        result.setShopProductId(product.getShopProductId());
        result.setProductName(product.getName());
        result.setProductImage(product.getImage());
        result.setStock(StringUtils.nvl(product.getStock(), -1));   // -1 表示无限库存
        // 归一化规格的字符串 key 版本（templateId → optionIds），供购物车 cartItemId 计算等使用。
        TreeMap<String, List<String>> normalizedSnapshot = new TreeMap<>();
        normalized.forEach((templateId, optionIds) -> normalizedSnapshot.put(String.valueOf(templateId), optionIds));
        result.setNormalizedSpecs(normalizedSnapshot);
        return result;
    }

    /**
     * 把前端传入的 specs 归一化为 {@code {templateId → [排序去重的 optionId]}}。
     *
     * <p>处理：key 转 Long（templateId）；value 统一成字符串列表；过滤空值；去重排序。
     * 排序是为了让「同规格集合、不同顺序」产生相同的归一化结果（购物车合并依赖此特性）。
     *
     * <p>同时对各组输入做大小/长度上限校验，防超大 payload。
     *
     * @throws ServiceException key 非数字 / value 类型非法 / 超出大小限制
     */
    private TreeMap<Long, List<String>> normalizeSpecs(Map<String, Object> specs)
    {
        TreeMap<Long, List<String>> normalized = new TreeMap<>();
        if (specs == null)
        {
            return normalized;
        }
        // 防护：规格组数上限 16。
        if (specs.size() > 16)
        {
            throw new ServiceException("商品规格不能超过16组");
        }
        specs.forEach((key, value) -> {
            // 防护：templateId key 长度上限 20（正常是纯数字，远短于此）。
            if (StringUtils.isEmpty(key) || key.length() > 20)
            {
                throw new ServiceException("规格模板ID格式不正确");
            }
            Long templateId;
            try
            {
                templateId = Long.valueOf(key);
            }
            catch (NumberFormatException e)
            {
                throw new ServiceException("规格模板ID格式不正确");
            }
            // value 可能是单个字符串（单选）或字符串数组（多选），统一成集合。
            Collection<String> values;
            if (value instanceof String optionId)
            {
                values = Collections.singletonList(optionId);
            }
            else if (value instanceof Collection<?> collection)
            {
                // 防护：单组选项数上限 16。
                if (collection.size() > 16)
                {
                    throw new ServiceException("单组规格选项不能超过16个");
                }
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
            // 防护：单个 optionId 长度上限 64（UUID 也只有 32）。
            if (values.stream().anyMatch(optionId -> optionId.length() > 64))
            {
                throw new ServiceException("规格选项ID长度不能超过64个字符");
            }
            // 过滤空值、去重、排序——排序保证顺序无关。
            List<String> optionIds = values.stream().filter(StringUtils::isNotEmpty).distinct().sorted().toList();
            if (!optionIds.isEmpty())
            {
                normalized.put(templateId, optionIds);
            }
        });
        return normalized;
    }

    /**
     * 校验某规格模板的选择数量是否符合规则：必选/最少/最多/单选。
     *
     * @param spec  规格模板
     * @param count 实际选择数
     * @throws ServiceException 未达必选/最少、超过最多、单选选多项
     */
    private void validateSelectionCount(CSpecView spec, int count)
    {
        int min = StringUtils.nvl(spec.getMinSelect(), 0);
        int max = Math.max(1, StringUtils.nvl(spec.getMaxSelect(), 1));
        // 必选模板：至少选 min 项。
        if (spec.isRequired() && count < Math.max(1, min))
        {
            throw new ServiceException("请选择" + spec.getName());
        }
        // 选了但不够最少。
        if (count > 0 && count < min)
        {
            throw new ServiceException(spec.getName() + "至少选择" + min + "项");
        }
        // 单选模板（type=1）最多 1 项。
        if (Integer.valueOf(1).equals(spec.getType()) && count > 1)
        {
            throw new ServiceException(spec.getName() + "只能选择一项");
        }
        // 超过最多选择数。
        if (count > max)
        {
            throw new ServiceException(spec.getName() + "最多选择" + max + "项");
        }
    }
}
