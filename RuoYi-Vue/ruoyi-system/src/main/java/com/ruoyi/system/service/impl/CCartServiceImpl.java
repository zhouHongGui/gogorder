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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

/**
 * C 端购物车服务（基于 Redis Hash）。
 *
 * <h3>存储模型（接手必读）</h3>
 * <ul>
 *   <li><b>每个门店独立一车</b>：Redis Key 为 {@code cart:{userId}:{shopId}}，
 *       用户在不同门店的购物车互不影响。</li>
 *   <li><b>Hash 结构</b>：固定字段 {@code shopId}、{@code shopName}、{@code version}（版本号），
 *       每个商品条目占一个 Field，名为 {@code item:{cartItemId}}，值是该条目的 JSON。</li>
 *   <li><b>cartItemId</b>：由 {@code productId + 规格归一化串} 的 SHA-256 前 24 位生成，
 *       同商品同规格 → 同 cartItemId，从而「再次加购」会合并数量而非新增条目。</li>
 *   <li><b>空车自动清理</b>：当 Hash 只剩 3 个固定字段（无商品条目）时直接 DEL 整个 Key。</li>
 *   <li><b>TTL</b>：7 天，add/update/remove/get 任一操作都会续期，避免活跃用户的车被清空。</li>
 * </ul>
 *
 * <h3>并发安全</h3>
 * 所有写操作（加/改/删）都走 <b>Lua 脚本</b>，在 Redis 单线程内原子执行「读-改-写」，
 * 避免并发请求互相覆盖（经典「读后写」丢失更新问题）。脚本返回约定状态码（SUCCESS /
 * STOCK_NOT_ENOUGH / QUANTITY_EXCEEDED / ITEM_NOT_FOUND），由 {@link #handleScriptResult} 翻译成异常。
 *
 * <p><b>注意</b>：购物车里的库存/价格只是「加购时快照」，最终以下单时后端重新校验为准。
 */
@Service
public class CCartServiceImpl implements ICCartService
{
    private static final Logger log = LoggerFactory.getLogger(CCartServiceImpl.class);

    /** 购物车存活时间：7 天（秒）。 */
    private static final int CART_TTL_SECONDS = 7 * 24 * 60 * 60;
    /** 商品条目在 Hash 中的字段前缀，用于与 shopId/shopName/version 等固定字段区分。 */
    private static final String ITEM_PREFIX = "item:";

    /**
     * 加购 Lua 脚本。原子地完成「读取已存在条目 → 合并数量 → 库存/数量校验 → 写回」。
     * <p>入参(KEYS=[cartKey], ARGV=[shopId, shopName, itemField, itemJson, maxStock, ttlSeconds])。
     * <p>返回：SUCCESS / QUANTITY_EXCEEDED(>99) / STOCK_NOT_ENOUGH。
     */
    private static final DefaultRedisScript<String> ADD_SCRIPT = script("""
            local cartKey = KEYS[1]
            local shopId = ARGV[1]
            local shopName = ARGV[2]
            local itemField = ARGV[3]
            local itemJson = ARGV[4]
            local maxStock = tonumber(ARGV[5])   -- -1 表示无限库存，不做上限校验
            local newItem = cjson.decode(itemJson)
            -- 若已有同 cartItemId 条目，则合并数量（再次加购）
            local existing = redis.call('HGET', cartKey, itemField)
            if existing then
                local oldItem = cjson.decode(existing)
                newItem.quantity = oldItem.quantity + newItem.quantity
            end
            -- 数量上限 99（单商品）
            if newItem.quantity > 99 then
                return 'QUANTITY_EXCEEDED'
            end
            -- 有限库存时校验：合计数量不得超过库存
            if maxStock >= 0 and newItem.quantity > maxStock then
                return 'STOCK_NOT_ENOUGH'
            end
            -- 重算金额并写回
            newItem.amount = newItem.unitPrice * newItem.quantity
            redis.call('HSET', cartKey, 'shopId', shopId)
            redis.call('HSET', cartKey, 'shopName', shopName)
            redis.call('HSET', cartKey, itemField, cjson.encode(newItem))
            redis.call('HINCRBY', cartKey, 'version', 1)   -- 版本号自增，供乐观校验/调试
            redis.call('EXPIRE', cartKey, tonumber(ARGV[6]))
            return 'SUCCESS'
            """);

    /**
     * 修改数量 Lua 脚本。quantity=0 表示删除该条目；否则覆盖为新数量。
     * <p>入参(KEYS=[cartKey], ARGV=[itemField, quantity, maxStock, ttlSeconds])。
     * <p>返回：SUCCESS / ITEM_NOT_FOUND / STOCK_NOT_ENOUGH。
     */
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
                redis.call('HDEL', cartKey, itemField)   -- 数量置 0 = 删除条目
            else
                local item = cjson.decode(existing)
                if maxStock >= 0 and quantity > maxStock then
                    return 'STOCK_NOT_ENOUGH'
                end
                item.stock = maxStock                    -- 顺带刷新库存快照
                item.quantity = quantity
                item.amount = item.unitPrice * quantity
                redis.call('HSET', cartKey, itemField, cjson.encode(item))
            end
            redis.call('HINCRBY', cartKey, 'version', 1)
            -- 若条目删空（只剩 shopId/shopName/version 三个固定字段）则清理整个 Key
            if redis.call('HLEN', cartKey) <= 3 then
                redis.call('DEL', cartKey)
            else
                redis.call('EXPIRE', cartKey, tonumber(ARGV[4]))
            end
            return 'SUCCESS'
            """);

    /**
     * 删除条目 Lua 脚本。
     * <p>入参(KEYS=[cartKey], ARGV=[itemField, ttlSeconds])。
     * <p>返回：SUCCESS / ITEM_NOT_FOUND。
     */
    private static final DefaultRedisScript<String> REMOVE_SCRIPT = script("""
            local cartKey = KEYS[1]
            local itemField = ARGV[1]
            if redis.call('HDEL', cartKey, itemField) == 0 then
                return 'ITEM_NOT_FOUND'   -- 条目本就不存在
            end
            redis.call('HINCRBY', cartKey, 'version', 1)
            -- 删后若空车则清理 Key，否则续期
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

    /**
     * 查询购物车。非空购物车会续期 TTL（活跃用户的车不会因只读而过期）。
     *
     * @return 购物车视图，含商品条目（按 cartItemId 排序）、总数量、总金额
     */
    @Override
    public CCartView getCart(Long userId, Long shopId)
    {
        Shop shop = requireShop(shopId);
        // 一次性读出整个 Hash（门店购物车条目有限，全量读取可接受）。
        Map<Object, Object> entries = redisTemplate.opsForHash().entries(cartKey(userId, shopId));
        CCartView cart = new CCartView();
        cart.setShopId(shop.getId());
        cart.setShopName(shop.getName());
        if (entries.isEmpty())
        {
            return cart;
        }
        // 非空购物车续期 7 天，避免用户长期只浏览不下单导致车被清空。
        redisTemplate.expire(cartKey(userId, shopId), CART_TTL_SECONDS, java.util.concurrent.TimeUnit.SECONDS);
        // 只取 item:* 字段，反序列化并按 cartItemId 排序（保证列表顺序稳定）。
        List<CCartItem> items = entries.entrySet().stream()
                .filter(entry -> String.valueOf(entry.getKey()).startsWith(ITEM_PREFIX))
                .map(entry -> JSON.parseObject(String.valueOf(entry.getValue()), CCartItem.class))
                .sorted(Comparator.comparing(CCartItem::getCartItemId))
                .toList();
        cart.setItems(items);
        // 汇总金额与数量（兜底处理 null）。
        cart.setTotalAmount(items.stream().mapToInt(item -> StringUtils.nvl(item.getAmount(), 0)).sum());
        cart.setTotalCount(items.stream().mapToInt(item -> StringUtils.nvl(item.getQuantity(), 0)).sum());
        return cart;
    }

    /**
     * 加入购物车（同商品同规格会合并数量）。
     *
     * <p>先用 {@link #buildCartItem} 重新校验规格+定价（不信任前端价格），再通过 ADD_SCRIPT 原子写入。
     *
     * @return 操作后的完整购物车视图
     */
    @Override
    public CCartView addItem(Long userId, CCartAddRequest request)
    {
        Shop shop = requireShop(request.getShopId());
        // 构造条目：含规格校验、后端重算单价、生成 cartItemId。
        CCartItem item = buildCartItem(request);
        // 执行加购脚本（原子）。stock=-1 表示无限库存，脚本内会跳过库存上限校验。
        String result = redisTemplate.execute(
                ADD_SCRIPT,
                Collections.singletonList(cartKey(userId, request.getShopId())),
                String.valueOf(shop.getId()),
                shop.getName(),
                itemField(item.getCartItemId()),
                JSON.toJSONString(item),
                String.valueOf(StringUtils.nvl(item.getStock(), -1)),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(userId, request.getShopId(), result);
        return getCart(userId, request.getShopId());
    }

    /**
     * 修改购物车条目数量。quantity=0 表示删除该条目。
     *
     * <p>修改前会重新查一次商品当前库存（商品可能在加购后被改库存/下架），作为数量上限校验依据。
     *
     * @return 操作后的完整购物车视图
     */
    @Override
    public CCartView updateItem(Long userId, CCartUpdateRequest request)
    {
        requireShop(request.getShopId());
        // 先确认条目存在（不存在直接抛异常）。
        CCartItem item = requireCartItem(userId, request.getShopId(), request.getCartItemId());
        // quantity=0（删除）无需查库存；否则查实时库存作为上限。-1 表示无限库存。
        int currentStock = request.getQuantity() == 0 ? -1 : currentStock(request.getShopId(), item);
        String result = redisTemplate.execute(
                UPDATE_SCRIPT,
                Collections.singletonList(cartKey(userId, request.getShopId())),
                itemField(request.getCartItemId()),
                String.valueOf(request.getQuantity()),
                String.valueOf(currentStock),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(userId, request.getShopId(), result);
        return getCart(userId, request.getShopId());
    }

    /**
     * 删除购物车条目。
     *
     * @return 操作后的完整购物车视图
     */
    @Override
    public CCartView removeItem(Long userId, Long shopId, String cartItemId)
    {
        requireShop(shopId);
        String result = redisTemplate.execute(
                REMOVE_SCRIPT,
                Collections.singletonList(cartKey(userId, shopId)),
                itemField(cartItemId),
                String.valueOf(CART_TTL_SECONDS));
        handleScriptResult(userId, shopId, result);
        return getCart(userId, shopId);
    }

    /**
     * 清空指定门店的购物车。
     *
     * @return 空购物车视图
     */
    @Override
    public CCartView clearCart(Long userId, Long shopId)
    {
        requireShop(shopId);
        redisTemplate.delete(cartKey(userId, shopId));
        return getCart(userId, shopId);
    }

    /**
     * 构造购物车条目：调用规格校验服务重新定价（不信任前端价格），并组装展示信息。
     */
    private CCartItem buildCartItem(CCartAddRequest request)
    {
        var validation = specValidationService.validateAndPrice(request.getShopId(), request.getProductId(), request.getSpecs());
        // 把规格快照转成购物车展示用的规格选项。
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
        // cartItemId 决定「同商品同规格」是否合并；用归一化后的规格计算，保证顺序无关。
        item.setCartItemId(cartItemId(request.getProductId(), validation.getNormalizedSpecs()));
        item.setShopProductId(validation.getShopProductId());
        item.setProductId(request.getProductId());
        item.setProductName(validation.getProductName());
        item.setImage(validation.getProductImage());
        item.setSpecs(validation.getNormalizedSpecs());
        item.setSelectedSpecs(selectedOptions);
        // 规格展示文本（如「大杯、去冰」），无规格显示「默认规格」。
        item.setSpecText(selectedOptions.isEmpty() ? "默认规格"
                : selectedOptions.stream().map(CCartSpecOption::getLabel).collect(Collectors.joining("、")));
        item.setUnitPrice(validation.getUnitPrice());
        item.setQuantity(request.getQuantity());
        item.setAmount(Math.multiplyExact(validation.getUnitPrice(), request.getQuantity()));
        item.setStock(validation.getStock());
        return item;
    }

    /**
     * 把 Lua 脚本的返回码翻译成业务异常。
     *
     * @param result 脚本返回的状态字符串
     * @throws ServiceException 非 SUCCESS 的各种业务异常
     */
    private void handleScriptResult(Long userId, Long shopId, String result)
    {
        if ("SUCCESS".equals(result))
        {
            return;
        }
        // 非 SUCCESS 一律记 warn（含 shopId 便于定位是哪个门店的车），再翻译成业务异常。
        log.warn("购物车操作失败 userId={} shopId={} result={}", userId, shopId, result);
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
        // 未知的返回码（脚本异常等），统一兜底。
        throw new ServiceException("购物车操作失败");
    }

    /** 从购物车读取单个条目，不存在则抛异常。 */
    private CCartItem requireCartItem(Long userId, Long shopId, String cartItemId)
    {
        Object json = redisTemplate.opsForHash().get(cartKey(userId, shopId), itemField(cartItemId));
        if (json == null)
        {
            throw new ServiceException("购物车条目不存在");
        }
        return JSON.parseObject(String.valueOf(json), CCartItem.class);
    }

    /**
     * 查询商品当前库存（用于改数量时的上限校验）。
     *
     * @throws ServiceException 商品已下架或不属于该门店
     */
    private int currentStock(Long shopId, CCartItem item)
    {
        ShopProduct shopProduct = productCenterMapper.selectActiveShopProductForCart(shopId, item.getShopProductId());
        if (shopProduct == null)
        {
            throw new ServiceException("商品已下架或不属于当前门店");
        }
        // 库存可能为 null（未配置），兜底为 -1（无限库存）。
        return StringUtils.nvl(shopProduct.getStock(), -1);
    }

    /**
     * 生成购物车条目 ID：{@code SHA-256(productId + 规格归一化JSON)} 的前 24 位十六进制。
     *
     * <p>用归一化（排序后的规格）参与哈希，保证「同商品、同规格集合但顺序不同」也得到相同 ID，
     * 从而正确合并数量。
     */
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

    /**
     * 校验门店存在并返回。所有购物车操作都要求 shopId 非空且门店未删除。
     *
     * @throws ServiceException 门店 ID 为空或门店不存在
     */
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

    /** 购物车 Redis Key：{@code cart:{userId}:{shopId}}（每门店一车）。 */
    private String cartKey(Long userId, Long shopId)
    {
        return "cart:" + userId + ":" + shopId;
    }

    /** 商品条目在 Hash 中的字段名：{@code item:{cartItemId}}。 */
    private String itemField(String cartItemId)
    {
        return ITEM_PREFIX + cartItemId;
    }

    /**
     * 构造 {@link DefaultRedisScript}（Lua 文本 → String 返回类型）。
     * Spring 会缓存编译后的脚本 SHA，重复执行时用 EVALSHA 提升性能。
     */
    private static DefaultRedisScript<String> script(String text)
    {
        DefaultRedisScript<String> script = new DefaultRedisScript<>();
        script.setScriptText(text);
        script.setResultType(String.class);
        return script;
    }
}
