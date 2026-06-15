package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
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
import com.ruoyi.system.domain.dto.CCartAddRequest;
import com.ruoyi.system.domain.dto.CCartItem;
import com.ruoyi.system.domain.dto.CCartSpecOption;
import com.ruoyi.system.domain.dto.CCartUpdateRequest;
import com.ruoyi.system.domain.dto.CCartView;
import com.ruoyi.system.mapper.ProductCenterMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.ICCartService;
import com.ruoyi.system.service.ISpecValidationService;

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
    private ProductCenterMapper productCenterMapper;

    @Autowired
    private ShopMapper shopMapper;

    @Autowired
    private ISpecValidationService specValidationService;

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
        redisTemplate.expire(cartKey(userId, shopId), CART_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
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
        CCartItem item = buildCartItem(request);
        String result = redisTemplate.execute(
                ADD_SCRIPT,
                Collections.singletonList(cartKey(userId, request.getShopId())),
                String.valueOf(shop.getId()),
                shop.getName(),
                itemField(item.getCartItemId()),
                JSON.toJSONString(item),
                String.valueOf(StringUtils.nvl(item.getStock(), -1)),
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

    private CCartItem buildCartItem(CCartAddRequest request)
    {
        var validation = specValidationService.validateAndPrice(request.getShopId(), request.getProductId(), request.getSpecs());
        List<CCartSpecOption> selectedOptions = validation.getSelectedOptions().stream().map(snapshot -> {
            CCartSpecOption option = new CCartSpecOption();
            option.setTemplateId(snapshot.getTemplateId());
            option.setTemplateName(snapshot.getTemplateName());
            option.setOptionId(snapshot.getOptionId());
            option.setLabel(snapshot.getLabel());
            option.setPriceAdd(snapshot.getPriceAdd());
            return option;
        }).toList();

        CCartItem item = new CCartItem();
        item.setCartItemId(cartItemId(request.getProductId(), validation.getNormalizedSpecs()));
        item.setShopProductId(validation.getShopProductId());
        item.setProductId(request.getProductId());
        item.setProductName(validation.getProductName());
        item.setImage(validation.getProductImage());
        item.setSpecs(validation.getNormalizedSpecs());
        item.setSelectedSpecs(selectedOptions);
        item.setSpecText(selectedOptions.isEmpty() ? "默认规格"
                : selectedOptions.stream().map(CCartSpecOption::getLabel).collect(Collectors.joining("、")));
        item.setUnitPrice(validation.getUnitPrice());
        item.setQuantity(request.getQuantity());
        item.setAmount(Math.multiplyExact(validation.getUnitPrice(), request.getQuantity()));
        item.setStock(validation.getStock());
        return item;
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

    private String cartItemId(Long productId, Map<String, List<String>> specs)
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
