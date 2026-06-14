package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.ShopProduct;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.dto.CCartAddRequest;
import com.ruoyi.system.domain.dto.CCartItem;
import com.ruoyi.system.domain.dto.CCartSpecOption;
import com.ruoyi.system.domain.dto.CCartUpdateRequest;
import com.ruoyi.system.domain.dto.CCartView;
import com.ruoyi.system.domain.dto.CProductView;
import com.ruoyi.system.domain.dto.CSpecView;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.ICCartService;
import com.ruoyi.system.service.ICProductBrowseService;

@Service
public class CCartServiceImpl implements ICCartService
{
    private static final int CART_TTL_SECONDS = 7 * 24 * 60 * 60;
    private static final String ITEM_PREFIX = "item:";

    private static final DefaultRedisScript<String> ADD_SCRIPT = script("""
            local cartKey = KEYS[1]
            local shopId = ARGV[1]
            local shopName = ARGV[2]
            local itemField = ARGV[3]
            local itemJson = ARGV[4]
            local maxStock = tonumber(ARGV[5])
            local newItem = cjson.decode(itemJson)
            local existing = redis.call('HGET', cartKey, itemField)
            if existing then
                local oldItem = cjson.decode(existing)
                newItem.quantity = oldItem.quantity + newItem.quantity
            end
            if newItem.quantity > 99 then
                return 'QUANTITY_EXCEEDED'
            end
            if maxStock >= 0 and newItem.quantity > maxStock then
                return 'STOCK_NOT_ENOUGH'
            end
            newItem.amount = newItem.unitPrice * newItem.quantity
            redis.call('HSET', cartKey, 'shopId', shopId)
            redis.call('HSET', cartKey, 'shopName', shopName)
            redis.call('HSET', cartKey, itemField, cjson.encode(newItem))
            redis.call('HINCRBY', cartKey, 'version', 1)
            redis.call('EXPIRE', cartKey, tonumber(ARGV[6]))
            return 'SUCCESS'
            """);

    private static final DefaultRedisScript<String> UPDATE_SCRIPT = script("""
            local cartKey = KEYS[1]
            local itemField = ARGV[1]
            local quantity = tonumber(ARGV[2])
            local maxStock = tonumber(ARGV[3])
            local existing = redis.call('HGET', cartKey, itemField)
            if not existing then
                return 'ITEM_NOT_FOUND'
            end
            if quantity == 0 then
                redis.call('HDEL', cartKey, itemField)
            else
                local item = cjson.decode(existing)
                if maxStock >= 0 and quantity > maxStock then
                    return 'STOCK_NOT_ENOUGH'
                end
                item.stock = maxStock
                item.quantity = quantity
                item.amount = item.unitPrice * quantity
                redis.call('HSET', cartKey, itemField, cjson.encode(item))
            end
            redis.call('HINCRBY', cartKey, 'version', 1)
            if redis.call('HLEN', cartKey) <= 3 then
                redis.call('DEL', cartKey)
            else
                redis.call('EXPIRE', cartKey, tonumber(ARGV[4]))
            end
            return 'SUCCESS'
            """);

    private static final DefaultRedisScript<String> REMOVE_SCRIPT = script("""
            local cartKey = KEYS[1]
            local itemField = ARGV[1]
            if redis.call('HDEL', cartKey, itemField) == 0 then
                return 'ITEM_NOT_FOUND'
            end
            redis.call('HINCRBY', cartKey, 'version', 1)
            if redis.call('HLEN', cartKey) <= 3 then
                redis.call('DEL', cartKey)
            else
                redis.call('EXPIRE', cartKey, tonumber(ARGV[2]))
            end
            return 'SUCCESS'
            """);

    @Autowired
    private StringRedisTemplate redisTemplate;

    @Autowired
    private ICProductBrowseService productBrowseService;

    @Autowired
    private ProductCenterMapper productCenterMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Override
    public CCartView getCart(Long userId, Long shopId)
    {
        Shop shop = requireShop(shopId);
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId, shopId));
        CCartView cart = new CCartView();
        cart.setShopId(shop.getId());
        cart.setShopName(shop.getName());
        if (entries.isEmpty())
        {
            return cart;
        }
        List<CCartItem> items = entries.entrySet().stream()
                .filter(entry -> String.valueOf(entry.getKey()).startsWith(ITEM_PREFIX))
                .map(entry -> JSON.parseObject(String.valueOf(entry.getValue()), CCartItem.class))
                .sorted(Comparator.comparing(CCartItem::getCartItemId))
                .toList();
        cart.setItems(items);
        cart.setTotalAmount(items.stream().mapToInt(item -> StringUtils.nvl(item.getAmount(), 0)).sum());
        cart.setTotalCount(items.stream().mapToInt(item -> StringUtils.nvl(item.getQuantity(), 0)).sum());
        return cart;
    }

    @Override
    public CCartView addItem(Long userId, CCartAddRequest request)
    {
        Shop shop = requireShop(request.getShopId());
        CProductView product = productBrowseService.selectProductDetail(request.getShopId(), request.getProductId());
        CCartItem item = buildCartItem(product, request);
        String result = redisTemplate.execute(
                ADD_SCRIPT,
                Collections.singletonList(cartKey(userId, request.getShopId())),
                String.valueOf(shop.getId()),
                shop.getName(),
                itemField(item.getCartItemId()),
                JSON.toJSONString(item),
                String.valueOf(StringUtils.nvl(product.getStock(), -1)),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(result);
        return getCart(userId, request.getShopId());
    }

    @Override
    public CCartView updateItem(Long userId, CCartUpdateRequest request)
    {
        requireShop(request.getShopId());
        CCartItem item = requireCartItem(userId, request.getShopId(), request.getCartItemId());
        int currentStock = request.getQuantity() == 0 ? -1 : currentStock(request.getShopId(), item);
        String result = redisTemplate.execute(
                UPDATE_SCRIPT,
                Collections.singletonList(cartKey(userId, request.getShopId())),
                itemField(request.getCartItemId()),
                String.valueOf(request.getQuantity()),
                String.valueOf(currentStock),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(result);
        return getCart(userId, request.getShopId());
    }

    @Override
    public CCartView removeItem(Long userId, Long shopId, String cartItemId)
    {
        requireShop(shopId);
        String result = redisTemplate.execute(
                REMOVE_SCRIPT,
                Collections.singletonList(cartKey(userId, shopId)),
                itemField(cartItemId),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(result);
        return getCart(userId, shopId);
    }

    @Override
    public CCartView clearCart(Long userId, Long shopId)
    {
        requireShop(shopId);
        redisTemplate.delete(cartKey(userId, shopId));
        return getCart(userId, shopId);
    }

    private CCartItem buildCartItem(CProductView product, CCartAddRequest request)
    {
        if (product.isSoldOut())
        {
            throw new ServiceException("商品已售罄");
        }
        TreeMap<Long, List<String>> normalizedSpecs = normalizeSpecs(request.getSpecs());
        Map<Long, CSpecView> productSpecs = product.getSpecs().stream()
                .collect(Collectors.toMap(CSpecView::getTemplateId, spec -> spec));
        List<CCartSpecOption> selectedOptions = new ArrayList<>();
        int unitPrice = StringUtils.nvl(product.getPrice(), 0);

        for (CSpecView spec : product.getSpecs())
        {
            List<String> selectedIds = normalizedSpecs.getOrDefault(spec.getTemplateId(), Collections.emptyList());
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
                CCartSpecOption snapshot = new CCartSpecOption();
                snapshot.setTemplateId(spec.getTemplateId());
                snapshot.setTemplateName(spec.getName());
                snapshot.setOptionId(option.getOptionId());
                snapshot.setLabel(option.getLabel());
                snapshot.setPriceAdd(StringUtils.nvl(option.getPriceAdd(), 0));
                selectedOptions.add(snapshot);
                unitPrice += snapshot.getPriceAdd();
            }
        }
        if (normalizedSpecs.keySet().stream().anyMatch(templateId -> !productSpecs.containsKey(templateId)))
        {
            throw new ServiceException("商品不支持所选规格");
        }

        CCartItem item = new CCartItem();
        item.setCartItemId(cartItemId(product.getProductId(), normalizedSpecs));
        item.setShopProductId(product.getShopProductId());
        item.setProductId(product.getProductId());
        item.setProductName(product.getName());
        item.setImage(product.getImage());
        TreeMap<String, List<String>> snapshotSpecs = new TreeMap<>();
        normalizedSpecs.forEach((templateId, optionIds) -> snapshotSpecs.put(String.valueOf(templateId), optionIds));
        item.setSpecs(snapshotSpecs);
        item.setSelectedSpecs(selectedOptions);
        item.setSpecText(selectedOptions.isEmpty() ? "默认规格"
                : selectedOptions.stream().map(CCartSpecOption::getLabel).collect(Collectors.joining("、")));
        item.setUnitPrice(unitPrice);
        item.setQuantity(request.getQuantity());
        item.setAmount(unitPrice * request.getQuantity());
        item.setStock(StringUtils.nvl(product.getStock(), -1));
        return item;
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
            List<String> optionIds = values.stream()
                    .filter(StringUtils::isNotEmpty)
                    .distinct()
                    .sorted()
                    .toList();
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

    private void handleScriptResult(String result)
    {
        if ("SUCCESS".equals(result))
        {
            return;
        }
        if ("STOCK_NOT_ENOUGH".equals(result))
        {
            throw new ServiceException("商品库存不足");
        }
        if ("QUANTITY_EXCEEDED".equals(result))
        {
            throw new ServiceException("单个商品数量不能超过99");
        }
        if ("ITEM_NOT_FOUND".equals(result))
        {
            throw new ServiceException("购物车条目不存在");
        }
        throw new ServiceException("购物车操作失败");
    }

    private CCartItem requireCartItem(Long userId, Long shopId, String cartItemId)
    {
        Object json = redisTemplate.opsForHash().get(cartKey(userId, shopId), itemField(cartItemId));
        if (json == null)
        {
            throw new ServiceException("购物车条目不存在");
        }
        return JSON.parseObject(String.valueOf(json), CCartItem.class);
    }

    private int currentStock(Long shopId, CCartItem item)
    {
        ShopProduct shopProduct = productCenterMapper.selectActiveShopProductForCart(shopId, item.getShopProductId());
        if (shopProduct == null)
        {
            throw new ServiceException("商品已下架或不属于当前门店");
        }
        return StringUtils.nvl(shopProduct.getStock(), -1);
    }

    private String cartItemId(Long productId, TreeMap<Long, List<String>> specs)
    {
        String source = productId + ":" + JSON.toJSONString(specs);
        try
        {
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(source.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest).substring(0, 24);
        }
        catch (NoSuchAlgorithmException e)
        {
            throw new IllegalStateException("SHA-256 unavailable", e);
        }
    }

    private Shop requireShop(Long shopId)
    {
        if (shopId == null)
        {
            throw new ServiceException("门店ID不能为空");
        }
        Shop shop = shopMapper.selectShopById(shopId);
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        return shop;
    }

    private String cartKey(Long userId, Long shopId)
    {
        return "cart:" + userId + ":" + shopId;
    }

    private String itemField(String cartItemId)
    {
        return ITEM_PREFIX + cartItemId;
    }

    private static DefaultRedisScript<String> script(String text)
    {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(text);
        script.setResultType(String.class);
        return script;
    }
}
