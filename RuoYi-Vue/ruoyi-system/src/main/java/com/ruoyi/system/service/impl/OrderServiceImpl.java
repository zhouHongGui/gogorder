package com.ruoyi.system.service.impl;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.config.GogorderOrderProperties;
import com.ruoyi.common.enums.OrderStatusEnum;
import com.ruoyi.common.enums.OrderTypeEnum;
import com.ruoyi.common.enums.PayStatusEnum;
import com.ruoyi.common.enums.RefundStatusEnum;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.BizOrder;
import com.ruoyi.system.domain.BizOrderItem;
import com.ruoyi.system.domain.CUserBalance;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.dto.OrderDetailView;
import com.ruoyi.system.domain.dto.OrderItemRequest;
import com.ruoyi.system.domain.dto.OrderItemView;
import com.ruoyi.system.domain.dto.OrderListItemView;
import com.ruoyi.system.domain.dto.OrderPageView;
import com.ruoyi.system.domain.dto.OrderSubmitRequest;
import com.ruoyi.system.domain.dto.OrderSubmitResponse;
import com.ruoyi.system.domain.dto.SpecSnapshot;
import com.ruoyi.system.domain.dto.SpecValidationResult;
import com.ruoyi.system.mapper.BizOrderItemMapper;
import com.ruoyi.system.mapper.BizOrderMapper;
import com.ruoyi.system.mapper.CUserBalanceMapper;
import com.ruoyi.system.mapper.CUserMapper;
import com.ruoyi.system.mapper.ShopMapper;
import com.ruoyi.system.service.IOrderCancelService;
import com.ruoyi.system.service.IOrderService;
import com.ruoyi.system.service.IProductCenterService;
import com.ruoyi.system.service.ISpecValidationService;

/**
 * 订单核心服务（C 端下单 / 取消 / 查询）。
 *
 * <h3>职责</h3>
 * <ul>
 *   <li>下单 {@link #submitOrder}：校验 → 计价 → 落库 → 扣库存（单事务）。</li>
 *   <li>取消 {@link #cancelOrder}：委托 {@link IOrderCancelService}，本类只做转发。</li>
 *   <li>查询 {@link #getOrderList} / {@link #getOrderDetail}：组装 C 端视图。</li>
 * </ul>
 *
 * <h3>并发与幂等模型（接手必读）</h3>
 * <ul>
 *   <li><b>下单幂等键</b>：客户端传入 {@code submitToken}，落库到 {@code biz_order.submit_key}，
 *       配合唯一索引 {@code uk_user_submit_key(user_id, submit_key)} 防重复下单。
 *       同一 token 多次提交只会产生一笔订单（网络重试 / 用户连点均安全）。</li>
 *   <li><b>并发下单</b>：先查幂等键做快速返回，再 {@code selectByIdForUpdate} 锁用户行做二次校验，
 *       最后靠唯一索引 + {@link DuplicateKeyException} 重试兜底，三层防护。</li>
 *   <li><b>订单号</b>：{@code yyyyMMddHHmmss + 6位随机}，秒内仅 10^6 候选，碰撞靠
 *       {@code uk_order_no} 唯一索引 + 3 次重试解决。</li>
 *   <li><b>库存扣减顺序</b>：订单先 INSERT 成功拿到 orderId，再按 shopProductId 升序逐个扣减，
 *       升序是为了降低多订单并发扣减时的死锁概率。整个 {@code doSubmitOrder} 在同一事务内，
 *       扣库存失败会连同订单一起回滚。</li>
 *   <li><b>金额单位</b>：全链路用「分」（int），计算一律走 {@link Math#addExact}/{@link Math#multiplyExact}
 *       防溢出，溢出转成业务异常「金额超出限制」。</li>
 * </ul>
 *
 * <h3>金额口径</h3>
 * {@code totalAmount = productAmount + packFee}，其中 {@code packFee = shop.packFee(分/杯) × 总杯数}。
 *
 * @see PaymentTransactionService 支付（余额扣款 / 取餐号分配 / 自动接单）
 * @see OrderCancelServiceImpl 取消与退款
 */
@Service
public class OrderServiceImpl implements IOrderService
{
    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    /** 订单号时间前缀格式：精确到秒。 */
    private static final DateTimeFormatter ORDER_NO_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmmss");

    @Autowired private BizOrderMapper bizOrderMapper;
    @Autowired private BizOrderItemMapper bizOrderItemMapper;
    @Autowired private ShopMapper shopMapper;
    @Autowired private CUserBalanceMapper cUserBalanceMapper;
    @Autowired private CUserMapper cUserMapper;
    @Autowired private ISpecValidationService specValidationService;
    @Autowired private IProductCenterService productCenterService;
    @Autowired private IOrderCancelService orderCancelService;
    /** 订单相关可调阈值（支付超时、单用户待支付上限、批量大小等），见 {@link GogorderOrderProperties}。 */
    @Autowired private GogorderOrderProperties orderProperties;

    /**
     * 提交订单（C 端下单入口）。
     *
     * <p>幂等：同一 {@code (userId, submitToken)} 只产生一笔订单，重复提交返回原订单。
     *
     * @param userId  当前登录用户 ID（由 {@code CAuthTokenFilter} 注入）
     * @param request 下单请求，含门店、订单类型、商品明细、预约时间等
     * @return 下单结果（订单号、应付金额、当前余额），供前端跳转支付页
     * @throws ServiceException 门店不存在 / 账号禁用 / 待支付订单超限 / 库存不足 / 金额超限 等
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public OrderSubmitResponse submitOrder(Long userId, OrderSubmitRequest request)
    {
        // 【幂等第 1 道】快速通道：无锁查询，命中则直接返回已存在的订单，避免重复下单。
        // 绝大多数重试请求（网络抖动 / 用户连点）会在这里直接返回，不走后续重逻辑。
        BizOrder existing = bizOrderMapper.selectBySubmitKey(userId, request.getSubmitToken());
        if (existing != null)
        {
            log.debug("下单幂等命中(快速通道) userId={} orderId={} orderNo={} shopId={}",
                    userId, existing.getId(), existing.getOrderNo(), existing.getShopId());
            return buildSubmitResponse(existing);
        }
        // 【用户可用性校验 + 加锁】锁住用户行，既校验账号是否被禁用，也为后面的「待支付订单数」
        // 计数提供一致性快照。注意：CAuthTokenFilter 已在请求入口校验过状态，这里是纵深防御，
        // 确保即使 filter 被绕过 / 账号在 token 有效期内被禁用，下单也会被拦截。
        CUser user = cUserMapper.selectByIdForUpdate(userId);
        if (user == null || !Integer.valueOf(1).equals(user.getStatus()))
        {
            throw new ServiceException("账号不可用");
        }
        // 【幂等第 2 道】拿到用户锁后再查一次，处理「第 1 道查询与加锁之间，另一并发请求已建单」的窗口。
        existing = bizOrderMapper.selectBySubmitKey(userId, request.getSubmitToken());
        if (existing != null)
        {
            log.debug("下单幂等命中(加锁后) userId={} orderId={} orderNo={} shopId={}",
                    userId, existing.getId(), existing.getOrderNo(), existing.getShopId());
            return buildSubmitResponse(existing);
        }
        // 【防占库存】限制单用户「待支付」订单数。下单即扣库存，未支付订单要等 15 分钟超时才归还，
        // 若不限制，恶意用户可反复下单锁死限量商品库存。阈值由 orderProperties 配置。
        if (bizOrderMapper.countPendingOrdersByUserId(userId) >= orderProperties.getMaxPendingOrdersPerUser())
        {
            throw new ServiceException("待支付订单过多，请先支付或取消已有订单");
        }
        return doSubmitOrder(userId, request);
    }

    /**
     * 实际下单逻辑：校验门店/类型 → 逐项计价 → 落库订单 → 批量插入明细 → 扣库存。
     *
     * <p>整个方法在 {@link #submitOrder} 的事务内执行；任何一步抛异常，订单与已扣库存全部回滚。
     *
     * @return 下单结果；若并发期间该 submitToken 已被建单，则返回那笔已有订单
     */
    private OrderSubmitResponse doSubmitOrder(Long userId, OrderSubmitRequest request)
    {
        // 校验门店存在（门店为逻辑删除，已删除门店查不到）。
        Shop shop = shopMapper.selectShopById(request.getShopId());
        if (shop == null)
        {
            throw new ServiceException("门店不存在或已删除");
        }
        // 校验订单类型与营业时间：即时单要求门店「营业中」，预订单校验取餐时间窗口。
        validateOrderType(shop, request);

        List<BizOrderItem> orderItems = new ArrayList<>();
        // key=shopProductId，value=该门店商品在本单中的合计数量。用 LinkedHashMap 保留插入顺序，
        // 后续排序扣减时用作「按 id 升序」的来源（降低并发死锁概率）。
        Map<Long, Integer> stockDeductions = new LinkedHashMap<>();
        int productAmount = 0;   // 商品总金额（分），累加各明细小计
        int totalQuantity = 0;   // 总杯数，用于计算包装费
        try
        {
            for (OrderItemRequest itemRequest : request.getItems())
            {
                // 规格 + 价格后端重算：不信任前端传的价格，按 (shopId, productId, specs) 重新校验并定价，
                // 同时返回当前库存（-1 表示无限库存）和商品快照信息。
                SpecValidationResult validation = specValidationService.validateAndPrice(
                        request.getShopId(), itemRequest.getProductId(), itemRequest.getSpecs());
                // 库存校验（仅有限库存才校验，-1 无限库存跳过）。
                // 注意：这里只是「预检」，真正的原子扣减在后面 deductStock 的条件 UPDATE 里，
                // 因此即便此处通过，并发下也可能扣减失败（会抛异常回滚整单）。
                if (validation.getStock() != -1 && validation.getStock() < itemRequest.getQuantity())
                {
                    throw new ServiceException("商品「" + validation.getProductName() + "」库存不足");
                }
                // 小计 = 单价 × 数量；multiplyExact 防整数溢出。
                int subtotal = Math.multiplyExact(validation.getUnitPrice(), itemRequest.getQuantity());
                productAmount = Math.addExact(productAmount, subtotal);
                totalQuantity = Math.addExact(totalQuantity, itemRequest.getQuantity());
                // 同一门店商品合并数量（一单里可能含同一商品的不同规格，扣库存按门店商品维度聚合）。
                stockDeductions.merge(validation.getShopProductId(), itemRequest.getQuantity(), Math::addExact);

                // 组装订单明细（含规格快照），此时还没 orderId，落库前再回填。
                BizOrderItem item = new BizOrderItem();
                item.setShopProductId(validation.getShopProductId());
                item.setProductId(itemRequest.getProductId());
                item.setProductName(validation.getProductName());
                item.setProductImage(validation.getProductImage());
                // 规格快照序列化为 JSON 存明细，保证日后商品/规格改价不影响历史订单展示。
                item.setSpecs(JSON.toJSONString(validation.getSelectedOptions()));
                item.setUnitPrice(validation.getUnitPrice());
                item.setQuantity(itemRequest.getQuantity());
                item.setSubtotal(subtotal);
                orderItems.add(item);
            }
        }
        catch (ArithmeticException e)
        {
            // 金额/数量溢出（极端大单），转成用户可读的业务异常。
            throw new ServiceException("订单金额或数量超出系统限制");
        }

        int packFee;
        int totalAmount;
        try
        {
            // 包装费 = 每杯包装费 × 总杯数（packFee 字段单位：分/杯）。
            packFee = Math.multiplyExact(StringUtils.nvl(shop.getPackFee(), 0), totalQuantity);
            // 应付合计 = 商品金额 + 包装费（V1.0 无配送费/优惠）。
            totalAmount = Math.addExact(productAmount, packFee);
        }
        catch (ArithmeticException e)
        {
            throw new ServiceException("订单金额超出系统限制");
        }

        // 组装订单主记录，初始状态：order_status=0(待支付) pay_status=0(待支付) refund_status=0(无退款)。
        BizOrder order = new BizOrder();
        order.setSubmitKey(request.getSubmitToken());
        order.setUserId(userId);
        order.setShopId(request.getShopId());
        order.setOrderType(request.getOrderType());
        order.setScheduledPickupTime(request.getScheduledPickupTime());
        order.setProductAmount(productAmount);
        order.setPackFee(packFee);
        order.setTotalAmount(totalAmount);
        order.setRemark(StringUtils.trim(request.getRemark()));
        order.setCreateTime(LocalDateTime.now());
        // 落库（带订单号碰撞重试）。返回非空表示并发期间该 submitToken 已被建单，直接幂等返回。
        BizOrder concurrentExisting = insertOrderWithRetry(order);
        if (concurrentExisting != null)
        {
            return buildSubmitResponse(concurrentExisting);
        }

        // 订单已落库拿到自增 id，回填到每条明细。
        for (BizOrderItem item : orderItems)
        {
            item.setOrderId(order.getId());
        }
        bizOrderItemMapper.batchInsert(orderItems);

        // 按 shopProductId 升序扣减库存（固定加锁顺序，避免不同订单交叉扣减时死锁）。
        // deductStock 内部用「WHERE stock >= qty」条件 UPDATE 原子扣减 + stock_ledger 幂等流水。
        List<Long> sortedIds = new ArrayList<>(stockDeductions.keySet());
        Collections.sort(sortedIds);
        for (Long shopProductId : sortedIds)
        {
            productCenterService.deductStock(order.getId(), shopProductId, stockDeductions.get(shopProductId));
        }
        log.info("下单成功 orderId={} orderNo={} userId={} shopId={} amount={} orderType={}",
                order.getId(), order.getOrderNo(), userId, order.getShopId(),
                order.getTotalAmount(), order.getOrderType());
        return buildSubmitResponse(order);
    }

    /**
     * 插入订单，处理两类唯一约束冲突：
     * <ul>
     *   <li>{@code uk_user_submit_key}：同 token 重复下单 → 查出已有订单幂等返回；</li>
     *   <li>{@code uk_order_no}：订单号随机碰撞 → 换号重试，最多 3 次。</li>
     * </ul>
     * MySQL 下唯一约束冲突不会中止事务，因此捕获后可继续在同一事务内操作。
     *
     * @return 已存在的订单（命中 submit_key 幂等时）；{@code null} 表示本次插入成功
     */
    private BizOrder insertOrderWithRetry(BizOrder order)
    {
        for (int retry = 0; retry < 3; retry++)
        {
            order.setOrderNo(generateOrderNo());   // 每次重试都重新生成订单号
            try
            {
                bizOrderMapper.insertOrder(order);
                return null;   // 插入成功
            }
            catch (DuplicateKeyException e)
            {
                // 命中「用户+提交键」唯一约束：说明并发请求已用同 token 建单，加锁查出该单返回。
                if (containsConstraint(e, "uk_user_submit_key"))
                {
                    BizOrder existing = bizOrderMapper.selectBySubmitKeyForUpdate(order.getUserId(), order.getSubmitKey());
                    if (existing != null)
                    {
                        log.debug("下单幂等命中(submit_key冲突) userId={} orderId={} orderNo={} shopId={}",
                                order.getUserId(), existing.getId(), existing.getOrderNo(), existing.getShopId());
                        return existing;
                    }
                    // 极端情况：约束报错却查不到订单，继续往下走重试逻辑。
                }
                // 非 submit_key 冲突，或已是 uk_order_no 但已达最大重试次数 → 直接抛出，交给上层回滚。
                if (!containsConstraint(e, "uk_order_no") || retry == 2)
                {
                    throw e;
                }
                // 否则是 uk_order_no 碰撞且仍有重试机会，循环换号重试。
                log.warn("订单号碰撞重试 userId={} collidedOrderNo={} retry={}",
                        order.getUserId(), order.getOrderNo(), retry + 1);
            }
        }
        throw new ServiceException("订单创建失败，请重试");
    }

    /**
     * 校验订单类型与取餐时间是否符合门店规则。
     * <ul>
     *   <li>即时单(NORMAL)：门店必须「营业中」(status=1 且当前时间在营业时段)；清空预约时间。</li>
     *   <li>预订单(PREORDER)：取餐时间必须在 [现在+最早分钟, 现在+最大天数] 窗口内，且落在营业时段。</li>
     * </ul>
     * 营业时间支持跨午夜（open_time > close_time 表示跨日），由 {@link Shop#isOpenAt} 判定。
     */
    private void validateOrderType(Shop shop, OrderSubmitRequest request)
    {
        LocalDateTime now = LocalDateTime.now();
        if (OrderTypeEnum.NORMAL.getCode().equals(request.getOrderType()))
        {
            // 即时单：门店需营业中。休息/暂停门店只能下预订单。
            if (!Integer.valueOf(1).equals(shop.getStatus()) || !shop.isOpenAt(now.toLocalTime()))
            {
                throw new ServiceException("当前门店暂不支持即时单，请选择预约单");
            }
            request.setScheduledPickupTime(null);   // 即时单无预约时间
            return;
        }
        if (!OrderTypeEnum.PREORDER.getCode().equals(request.getOrderType()))
        {
            throw new ServiceException("无效的订单类型");
        }
        // 预订单必须带取餐时间。
        LocalDateTime pickupTime = request.getScheduledPickupTime();
        if (pickupTime == null)
        {
            throw new ServiceException("预订单必须选择取餐时间");
        }
        // 取餐时间窗口：不早于「现在+最早预约分钟」(默认30)，不晚于「现在+最大预约天数」(默认7)。
        LocalDateTime earliest = now.plusMinutes(StringUtils.nvl(shop.getPreorderMinMinutes(), 30));
        LocalDateTime latest = now.plusDays(StringUtils.nvl(shop.getPreorderMaxDays(), 7));
        if (pickupTime.isBefore(earliest) || pickupTime.isAfter(latest) || !shop.isOpenAt(pickupTime.toLocalTime()))
        {
            throw new ServiceException("预约取餐时间不可用，请重新选择");
        }
    }

    /**
     * 取消订单（用户主动取消）。仅允许取消「未支付」订单，已支付订单的取消走管理端退款流程。
     * 实际逻辑在 {@link IOrderCancelService#cancelUnpaidOrder}。
     */
    @Override
    public void cancelOrder(Long userId, Long orderId)
    {
        orderCancelService.cancelUnpaidOrder(userId, orderId);
    }

    /**
     * 分页查询当前用户的订单列表（按下单时间）。
     *
     * @param dateScope "TODAY"=今日订单 / "HISTORY"=今日之前的历史订单
     * @param pageNum   页码，从 1 开始，默认 1
     * @param pageSize  每页条数，1~20，默认 10
     * @return 订单列表分页视图（含门店名、商品明细、状态描述等已组装好的展示字段）
     */
    @Override
    public OrderPageView getOrderList(Long userId, String dateScope, Integer pageNum, Integer pageSize)
    {
        // 规范化日期范围参数，仅支持 TODAY / HISTORY。
        String scope = StringUtils.isEmpty(dateScope) ? "TODAY" : dateScope.trim().toUpperCase();
        if (!"TODAY".equals(scope) && !"HISTORY".equals(scope))
        {
            throw new ServiceException("无效的订单日期范围");
        }
        // 计算时间区间：TODAY=[今日0点, 明日0点)；HISTORY=(-∞, 今日0点)。
        LocalDateTime todayStart = LocalDateTime.now().toLocalDate().atStartOfDay();
        LocalDateTime startTime = "TODAY".equals(scope) ? todayStart : null;
        LocalDateTime endTime = "TODAY".equals(scope) ? todayStart.plusDays(1) : todayStart;
        // 分页参数兜底：页码≥1，每页 1~20（限制单次返回量）。
        int currentPage = Math.max(StringUtils.nvl(pageNum, 1), 1);
        int currentPageSize = Math.min(Math.max(StringUtils.nvl(pageSize, 10), 1), 20);
        int offset = (currentPage - 1) * currentPageSize;
        long total = bizOrderMapper.countUserOrders(userId, startTime, endTime);
        // 总数为 0 或翻页越界：直接返回空页，避免无意义查询。
        if (total == 0 || offset >= total)
        {
            return new OrderPageView(Collections.emptyList(), total, currentPage, currentPageSize);
        }
        List<BizOrder> orders = bizOrderMapper.selectUserOrderList(
                userId, startTime, endTime, offset, currentPageSize);
        // 批量查询本页订单涉及的门店、明细，避免 N+1 查询。
        Set<Long> shopIds = orders.stream().map(BizOrder::getShopId).collect(Collectors.toSet());
        Map<Long, Shop> shops = shopMapper.selectShopByIds(new ArrayList<>(shopIds)).stream()
                .collect(Collectors.toMap(Shop::getId, shop -> shop));
        List<Long> orderIds = orders.stream().map(BizOrder::getId).toList();
        Map<Long, List<BizOrderItem>> itemsByOrder = bizOrderItemMapper.selectByOrderIds(orderIds).stream()
                .collect(Collectors.groupingBy(BizOrderItem::getOrderId));
        // 组装列表项视图（门店可能已逻辑删除，用「历史门店」兜底名称）。
        List<OrderListItemView> rows = orders.stream()
                .map(order -> buildListItemView(order, shops.get(order.getShopId()),
                        itemsByOrder.getOrDefault(order.getId(), Collections.emptyList())))
                .toList();
        return new OrderPageView(rows, total, currentPage, currentPageSize);
    }

    /**
     * 查询订单详情。仅返回属于当前用户的订单（越权访问返回「订单不存在」）。
     *
     * @return 含完整时间线（下单/支付/接单/制作/核销/取消各时间点）与商品明细的详情视图
     */
    @Override
    public OrderDetailView getOrderDetail(Long userId, Long orderId)
    {
        BizOrder order = bizOrderMapper.selectById(orderId);
        // 越权或不存在统一返回「订单不存在」，避免泄露订单是否存在的信息。
        if (order == null || !Objects.equals(order.getUserId(), userId))
        {
            throw new ServiceException("订单不存在");
        }
        Shop shop = shopMapper.selectShopById(order.getShopId());
        CUser user = cUserMapper.selectById(userId);
        // 逐字段组装详情视图（门店/用户可能为空，用空串兜底避免 NPE）。
        OrderDetailView view = new OrderDetailView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shop == null ? "" : shop.getName());
        view.setShopPhone(shop == null ? "" : shop.getPhone());
        view.setReservedPhone(user == null ? "" : user.getPhone());
        view.setOrderType(order.getOrderType());
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), order.getOrderType()));
        view.setScheduledPickupTime(order.getScheduledPickupTime());
        view.setOrderStatus(order.getOrderStatus());
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), order.getOrderStatus()));
        view.setPayStatus(order.getPayStatus());
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), order.getPayStatus()));
        view.setRefundStatus(order.getRefundStatus());
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), order.getRefundStatus()));
        view.setProductAmount(order.getProductAmount());
        view.setPackFee(order.getPackFee());
        view.setTotalAmount(order.getTotalAmount());
        view.setRemark(order.getRemark());
        view.setPickupDisplay(order.getPickupDisplay());
        view.setPickupToken(order.getPickupToken());
        view.setPickupDate(order.getPickupDate());
        view.setPayTime(order.getPayTime());
        view.setAcceptTime(order.getAcceptTime());
        view.setMakeStartTime(order.getMakeStartTime());
        view.setCompleteMakeTime(order.getCompleteMakeTime());
        view.setVerifyTime(order.getVerifyTime());
        view.setCancelTime(order.getCancelTime());
        view.setCancelReason(order.getCancelReason());
        view.setCreateTime(order.getCreateTime());
        view.setItems(bizOrderItemMapper.selectByOrderId(orderId).stream().map(this::buildItemView).toList());
        return view;
    }

    /**
     * 把订单明细实体转成前端展示视图：解析规格快照 JSON，拼出「规格文本」(如「大杯、去冰」)。
     */
    private OrderItemView buildItemView(BizOrderItem item)
    {
        // 规格快照反序列化；空 specs 视为「默认规格」。
        List<SpecSnapshot> specs = StringUtils.isEmpty(item.getSpecs())
                ? Collections.emptyList() : JSON.parseArray(item.getSpecs(), SpecSnapshot.class);
        OrderItemView view = new OrderItemView();
        view.setProductId(item.getProductId());
        view.setProductName(item.getProductName());
        view.setProductImage(item.getProductImage());
        view.setSpecs(specs);
        // 拼接规格展示文本，无规格显示「默认规格」。
        view.setSpecText(specs.isEmpty() ? "默认规格"
                : specs.stream().map(SpecSnapshot::getLabel).collect(Collectors.joining("、")));
        view.setUnitPrice(item.getUnitPrice());
        view.setQuantity(item.getQuantity());
        view.setSubtotal(item.getSubtotal());
        return view;
    }

    /**
     * 组装订单列表项视图。门店可能已逻辑删除，名称用「历史门店」兜底，保证列表不报错。
     */
    private OrderListItemView buildListItemView(BizOrder order, Shop shop, List<BizOrderItem> orderItems)
    {
        List<OrderItemView> items = orderItems.stream().map(this::buildItemView).toList();
        OrderListItemView view = new OrderListItemView();
        view.setOrderId(order.getId());
        view.setOrderNo(order.getOrderNo());
        view.setShopId(order.getShopId());
        view.setShopName(shop == null ? "历史门店" : shop.getName());
        view.setOrderType(order.getOrderType());
        view.setOrderTypeDesc(enumDesc(OrderTypeEnum.values(), order.getOrderType()));
        view.setScheduledPickupTime(order.getScheduledPickupTime());
        view.setOrderStatus(order.getOrderStatus());
        view.setOrderStatusDesc(enumDesc(OrderStatusEnum.values(), order.getOrderStatus()));
        view.setPayStatus(order.getPayStatus());
        view.setPayStatusDesc(enumDesc(PayStatusEnum.values(), order.getPayStatus()));
        view.setRefundStatus(order.getRefundStatus());
        view.setRefundStatusDesc(enumDesc(RefundStatusEnum.values(), order.getRefundStatus()));
        view.setTotalAmount(order.getTotalAmount());
        view.setItemCount(items.stream().mapToInt(item -> StringUtils.nvl(item.getQuantity(), 0)).sum());
        view.setPickupDisplay(order.getPickupDisplay());
        view.setCreateTime(order.getCreateTime());
        view.setItems(items);
        return view;
    }

    /**
     * 构建下单响应，附带用户当前余额（供前端支付页展示「余额是否足够」）。
     */
    private OrderSubmitResponse buildSubmitResponse(BizOrder order)
    {
        CUserBalance balance = cUserBalanceMapper.selectByUserId(order.getUserId());
        return new OrderSubmitResponse(order.getId(), order.getOrderNo(), order.getTotalAmount(),
                balance == null ? 0 : balance.getBalance(), order.getCreateTime());
    }

    /**
     * 生成订单号：{@code yyyyMMddHHmmss(14位) + 6位随机数}。
     * 秒内仅 10^6 候选值，碰撞由 {@code uk_order_no} 唯一索引 + 重试兜底，当前业务量级足够。
     */
    private String generateOrderNo()
    {
        return LocalDateTime.now().format(ORDER_NO_TIME)
                + String.format("%06d", ThreadLocalRandom.current().nextInt(1_000_000));
    }

    /**
     * 判断 {@link DuplicateKeyException} 是否由指定唯一约束触发。
     * 通过匹配异常 cause message 中是否包含约束名（大小写不敏感）。
     */
    private boolean containsConstraint(DuplicateKeyException e, String constraint)
    {
        return String.valueOf(e.getMostSpecificCause().getMessage()).toLowerCase()
                .contains(constraint.toLowerCase());
    }

    /** 枚举码 → 描述文本（订单类型），找不到返回空串。 */
    private String enumDesc(OrderTypeEnum[] values, String code)
    {
        for (OrderTypeEnum value : values) if (value.getCode().equals(code)) return value.getDesc();
        return "";
    }

    /** 枚举码 → 描述文本（订单状态），找不到返回空串。 */
    private String enumDesc(OrderStatusEnum[] values, Integer code)
    {
        for (OrderStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    /** 枚举码 → 描述文本（支付状态），找不到返回空串。 */
    private String enumDesc(PayStatusEnum[] values, Integer code)
    {
        for (PayStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }

    /** 枚举码 → 描述文本（退款状态），找不到返回空串。 */
    private String enumDesc(RefundStatusEnum[] values, Integer code)
    {
        for (RefundStatusEnum value : values) if (value.getCode() == code) return value.getDesc();
        return "";
    }
}
