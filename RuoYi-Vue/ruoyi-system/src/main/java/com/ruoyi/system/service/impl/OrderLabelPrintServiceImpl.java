package com.ruoyi.system.service.impl; // 订单标签打印业务实现所在包

import java.time.format.DateTimeFormatter; // 引入日期格式化器，用于标签上的时间显示
import java.util.ArrayList; // 引入动态数组，用于收集换行后的文本行
import java.util.Collections; // 引入集合工具，用于返回空列表
import java.util.List; // 引入集合类型
import java.util.UUID; // 引入 UUID，用作 Redis 处理中锁 owner
import java.util.concurrent.TimeUnit; // 引入时间单位，用于设置 Redis 键过期
import org.slf4j.Logger; // 引入日志接口
import org.slf4j.LoggerFactory; // 引入日志工厂
import org.springframework.beans.factory.annotation.Qualifier; // 引入限定符注解，指定注入的线程池 Bean
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor; // 引入线程池任务执行器，用于异步打印
import org.springframework.stereotype.Service; // 引入 Service 注解
import com.alibaba.fastjson2.JSON; // 引入 fastjson2，用于解析订单明细的规格快照 JSON
import com.ruoyi.common.core.redis.RedisCache; // 引入 Redis 缓存工具，用于打印幂等控制
import com.ruoyi.common.enums.OrderTypeEnum; // 引入订单类型枚举（即时单/预订单）
import com.ruoyi.common.enums.PayStatusEnum; // 引入支付状态枚举
import com.ruoyi.common.utils.StringUtils; // 引入字符串工具
import com.ruoyi.system.domain.BizOrder; // 引入订单实体
import com.ruoyi.system.domain.BizOrderItem; // 引入订单明细实体
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入打印机实体
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅响应，用于记录云端打印任务 ID
import com.ruoyi.system.domain.dto.SpecSnapshot; // 引入规格快照 DTO
import com.ruoyi.system.mapper.BizOrderItemMapper; // 引入订单明细 Mapper
import com.ruoyi.system.mapper.BizOrderMapper; // 引入订单 Mapper
import com.ruoyi.system.mapper.ShopLabelPrinterMapper; // 引入打印机 Mapper
import com.ruoyi.system.service.IFeiePrintService; // 引入飞鹅打印服务
import com.ruoyi.system.service.IOrderLabelPrintService; // 引入本服务接口

@Service // 声明为业务 Bean
public class OrderLabelPrintServiceImpl implements IOrderLabelPrintService // 订单标签打印实现：支付后异步为门店打印机逐商品打印标签
{
    private static final Logger log = LoggerFactory.getLogger(OrderLabelPrintServiceImpl.class); // 日志记录器
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("MM-dd HH:mm"); // 标签时间显示格式：月-日 时:分
    private static final int LABEL_MARGIN = 20; // 标签内容起始 Y 坐标（点），缩短取餐号与商品名的整体顶部留白
    private static final int LABEL_X = 10; // 标签文本固定 X 坐标（点），即左边距
    private static final int LABEL_SECTION_GAP = 6; // 标签各区块之间的纵向间距（点）
    private static final int LABEL_QR_MODULE_WIDTH = 3; // 飞鹅二维码宽度参数，默认 5 偏大，杯贴使用 3
    private static final int LABEL_QR_RESERVED_SIZE = 88; // w=3 二维码的预留宽高（点，包含扫码留白）
    private static final int LABEL_REMARK_MAX_LINES = 2; // 主标签最多显示两行备注，剩余内容打印续页
    private static final int LABEL_NORMAL_LINE_HEIGHT = 26; // 普通文本紧凑行高
    private static final int LABEL_PRINT_ONCE_TTL_DAYS = 30; // 「整单成功标记」过期天数：全部标签任务成功后才写入，期间内自动打印不重复执行
    private static final String LABEL_PRINT_ONCE_KEY_PREFIX = "order:label:printed:"; // 「成功标记」键前缀，后接订单 ID
    private static final String LABEL_PRINT_ITEM_KEY_PREFIX = "order:label:printed:item:"; // 「单项成功标记」键前缀，后接订单/打印机/明细 ID
    private static final int LABEL_PRINT_LOCK_TTL_MINUTES = 5; // 「处理中锁」过期分钟数：仅防止同一订单并发重复执行，进程崩溃后自动过期
    private static final String LABEL_PRINT_LOCK_KEY_PREFIX = "order:label:printing:"; // 「处理中锁」键前缀，后接订单 ID

    private final BizOrderMapper bizOrderMapper; // 订单数据访问
    private final BizOrderItemMapper bizOrderItemMapper; // 订单明细数据访问
    private final ShopLabelPrinterMapper shopLabelPrinterMapper; // 打印机数据访问
    private final IFeiePrintService feiePrintService; // 飞鹅打印服务
    private final ThreadPoolTaskExecutor taskExecutor; // 异步线程池，避免阻塞支付主流程
    private final RedisCache redisCache; // Redis 缓存，用于打印幂等

    public OrderLabelPrintServiceImpl(BizOrderMapper bizOrderMapper, BizOrderItemMapper bizOrderItemMapper, // 构造器注入依赖
            ShopLabelPrinterMapper shopLabelPrinterMapper, IFeiePrintService feiePrintService,
            @Qualifier("threadPoolTaskExecutor") ThreadPoolTaskExecutor taskExecutor, RedisCache redisCache)
    {
        this.bizOrderMapper = bizOrderMapper; // 保存订单 Mapper
        this.bizOrderItemMapper = bizOrderItemMapper; // 保存订单明细 Mapper
        this.shopLabelPrinterMapper = shopLabelPrinterMapper; // 保存打印机 Mapper
        this.feiePrintService = feiePrintService; // 保存飞鹅服务
        this.taskExecutor = taskExecutor; // 保存线程池
        this.redisCache = redisCache; // 保存 Redis 缓存
    }

    @Override
    public void printPaidOrderAsync(Long orderId) // 支付成功后异步触发标签打印（由支付流程调用）
    {
        if (orderId == null)
        {
            log.warn("支付后标签打印未调度：orderId 为空");
            return;
        }
        try
        {
            taskExecutor.execute(() -> { // 提交到线程池异步执行，立即返回不阻塞调用方
                try
                {
                    printPaidOrder(orderId); // 执行实际打印逻辑
                }
                catch (Exception e) // 异步任务抛出异常
                {
                    log.error("支付后标签打印任务异常结束 orderId={}", orderId, e); // 支付已成功，仅记录打印异常
                }
            });
            log.debug("支付后标签打印已调度 orderId={}", orderId);
        }
        catch (RuntimeException e) // 线程池关闭或队列已满时 execute 会同步抛异常
        {
            log.error("支付后标签打印调度失败 orderId={}", orderId, e);
        }
    }

    private void printPaidOrder(Long orderId) // 实际打印逻辑：抢锁 → 校验订单 → 遍历打印 → 成功才写标记
    {
        String lockOwner = acquireProcessingLock(orderId); // 抢「处理中锁」，返回 owner 用于安全释放
        if (lockOwner == null) // 抢锁失败，说明另一线程正在处理同一订单
        {
            log.info("支付后打印跳过：订单正在打印中 orderId={}", orderId); // 自动打印只记录跳过原因
            return; // 直接返回，避免并发重复打印
        }
        try
        {
            long startedAt = System.currentTimeMillis();
            log.info("开始处理支付后标签打印 orderId={}", orderId);
            if (isAlreadyPrinted(orderId)) // 自动打印已有「整单成功标记」，说明此前已成功打印过
            {
                log.info("支付后打印跳过：订单标签任务已完整提交过 orderId={}", orderId); // 云端受理不等于设备实际出纸
                return; // 直接返回，避免重复打印
            }
            BizOrder order = bizOrderMapper.selectById(orderId); // 查询订单
            if (order == null) // 订单不存在（事务尚未可见或已被清理）
            {
                log.warn("支付后打印跳过：订单不存在 orderId={}", orderId);
                return;
            }
            if (!Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(order.getPayStatus())) // 订单未支付成功
            {
                log.warn("支付后打印跳过：订单未支付成功 orderId={} payStatus={}", orderId, order.getPayStatus());
                return;
            }
            if (StringUtils.isBlank(order.getPickupToken()))
            {
                log.warn("支付成功订单缺少核销码，主标签将不打印二维码 orderId={} orderNo={} shopId={}",
                        orderId, order.getOrderNo(), order.getShopId());
            }
            List<ShopLabelPrinter> printers = shopLabelPrinterMapper.selectEnabledByShopId(order.getShopId()); // 查询门店启用的打印机
            if (printers.isEmpty()) // 门店未配置启用的打印机
            {
                log.info("支付后打印跳过：门店未配置启用中的标签打印机 orderId={} shopId={}", orderId, order.getShopId());
                return;
            }
            List<BizOrderItem> items = bizOrderItemMapper.selectByOrderId(orderId); // 查询订单明细
            if (items.isEmpty()) // 订单没有明细
            {
                log.warn("支付后打印跳过：订单明细为空 orderId={}", orderId);
                return;
            }
            log.info("支付后标签打印数据就绪 orderId={} orderNo={} shopId={} pickupDisplay={} printerCount={} itemCount={} hasPickupToken={}",
                    orderId, order.getOrderNo(), order.getShopId(), order.getPickupDisplay(), printers.size(), items.size(),
                    StringUtils.isNotBlank(order.getPickupToken()));

            int totalCount = 0; // 实际打印任务数，包含主标签和可能存在的备注续页
            int coveredCount = 0; // 云端已受理或已有幂等标记覆盖的任务数
            int failedCount = 0; // 发送失败任务数
            int skippedCount = 0; // 因单项幂等标记跳过的任务数
            for (ShopLabelPrinter printer : printers) // 遍历门店的每台启用打印机
            {
                int printWidth = StringUtils.nvl(printer.getPrintWidth(), 40);
                int printHeight = StringUtils.nvl(printer.getPrintHeight(), 50);
                if (printWidth < 40 || printHeight < 40)
                {
                    log.warn("标签纸尺寸小于当前订单模板建议值，内容可能被截断 printerId={} sn={} printSize={}x{}mm recommendedMin=40x40mm",
                            printer.getId(), printer.getSn(), printWidth, printHeight);
                }
                for (BizOrderItem item : items) // 遍历订单的每个商品明细
                {
                    int times = Math.max(StringUtils.nvl(item.getQuantity(), 1), 1); // 该商品的打印份数 = 购买数量，至少 1
                    List<LabelPrintTask> labelTasks;
                    try
                    {
                        labelTasks = buildLabelTasks(printer, order, item); // 主标签 + 备注超长时的续页标签
                    }
                    catch (Exception e) // 单个商品内容异常不能阻断同订单的其他商品
                    {
                        totalCount++;
                        failedCount++;
                        log.error("支付后标签内容构建失败 orderId={} printerId={} sn={} itemId={} product={} times={} errorType={}",
                                orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), times,
                                e.getClass().getSimpleName(), e);
                        continue;
                    }
                    log.info("支付后标签任务规划完成 orderId={} printerId={} sn={} itemId={} product={} times={} taskCount={} continuationPages={} printSize={}x{}mm remarkLength={}",
                            orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), times,
                            labelTasks.size(), Math.max(labelTasks.size() - 1, 0), printer.getPrintWidth(), printer.getPrintHeight(),
                            StringUtils.nvl(order.getRemark(), "").length());
                    for (LabelPrintTask labelTask : labelTasks)
                    {
                        totalCount++;
                        String itemKey = labelPrintItemKey(orderId, printer.getId(), item.getId()) + labelTask.keySuffix();
                        if (isItemPrinted(itemKey)) // 主标签和每张续页分别幂等，部分失败时只重试失败页
                        {
                            coveredCount++;
                            skippedCount++;
                            log.info("支付后打印单项跳过：已成功提交过 orderId={} printerId={} itemId={} labelPart={}",
                                    orderId, printer.getId(), item.getId(), labelTask.partName());
                            continue;
                        }
                        try
                        {
                            FeieResponse response = feiePrintService.printLabel(printer.getSn(), labelTask.content(), times); // 按商品数量提交当前标签页
                            coveredCount++; // 累加云端受理计数
                            markItemPrinted(itemKey); // 当前页单独写成功标记
                            log.info("支付后打印任务已提交 orderId={} printerId={} sn={} itemId={} product={} labelPart={} times={} feieOrderId={} feieMsg={}",
                                    orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), labelTask.partName(),
                                    times, response.getData(), response.getMsg()); // ret=0 仅表示云端受理，不能代表设备已经实际出纸
                        }
                        catch (Exception e) // 单页打印失败
                        {
                            failedCount++; // 累加失败计数
                            log.warn("支付后打印单项失败 orderId={} printerId={} sn={} itemId={} product={} labelPart={} times={}",
                                    orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), labelTask.partName(), times, e);
                        }
                    }
                }
            }
            boolean allCovered = totalCount > 0 && failedCount == 0 && coveredCount == totalCount;
            if (allCovered) // 全部任务均被云端受理或已有单项成功标记覆盖
            {
                markPrinted(orderId); // 写入「整单成功标记」，后续自动重复触发将跳过
            }
            else if (coveredCount == 0) // 全部失败
            {
                log.warn("支付后标签任务全部提交失败，未写入整单覆盖标记，后续再次触发可重试 orderId={}", orderId); // 不写整单标记以便重试
            }
            else // 部分成功
            {
                log.warn("支付后标签任务部分提交成功，未写入整单覆盖标记 orderId={} covered={} failed={} skipped={}",
                        orderId, coveredCount, failedCount, skippedCount); // 保留未成功项的重试空间
            }
            log.info("支付后标签打印处理完成 orderId={} totalTasks={} covered={} failed={} skipped={} allCovered={} elapsedMs={}",
                    orderId, totalCount, coveredCount, failedCount, skippedCount, allCovered,
                    System.currentTimeMillis() - startedAt);
        }
        finally
        {
            releaseProcessingLock(orderId, lockOwner); // owner 校验释放「处理中锁」，避免误删其他线程的新锁
        }
    }

    private String acquireProcessingLock(Long orderId) // 抢占「处理中锁」：setIfAbsent 成功表示获得锁，用于阻止并发重复执行
    {
        String owner = UUID.randomUUID().toString(); // 每次抢锁生成唯一 owner，释放时必须匹配
        boolean locked = redisCache.setCacheObjectIfAbsent(LABEL_PRINT_LOCK_KEY_PREFIX + orderId, owner,
                LABEL_PRINT_LOCK_TTL_MINUTES, TimeUnit.MINUTES); // 短期过期，进程崩溃后也会自动释放
        return locked ? owner : null;
    }

    private void releaseProcessingLock(Long orderId, String lockOwner) // 释放「处理中锁」，使后续触发可立即重试
    {
        redisCache.releaseLock(LABEL_PRINT_LOCK_KEY_PREFIX + orderId, lockOwner); // owner 校验释放，防止误删别人持有的新锁
    }

    private boolean isAlreadyPrinted(Long orderId) // 判断订单是否已有「成功标记」（即此前已成功打印过）
    {
        return Boolean.TRUE.equals(redisCache.hasKey(LABEL_PRINT_ONCE_KEY_PREFIX + orderId)); // 防御 hasKey 偶发返回 null
    }

    private void markPrinted(Long orderId) // 写入「成功标记」，保证一段时间内同一订单不重复打印
    {
        redisCache.setCacheObjectIfAbsent(LABEL_PRINT_ONCE_KEY_PREFIX + orderId, "1", // 键为前缀+订单ID，值无意义
                LABEL_PRINT_ONCE_TTL_DAYS, TimeUnit.DAYS); // 过期 30 天
    }

    private boolean isItemPrinted(String itemKey) // 判断某个「打印机+订单明细」标签是否已成功打印过
    {
        return Boolean.TRUE.equals(redisCache.hasKey(itemKey)); // 防御 hasKey 偶发返回 null
    }

    private void markItemPrinted(String itemKey) // 写入单项成功标记，避免自动重试时重复打印已成功项
    {
        redisCache.setCacheObjectIfAbsent(itemKey, "1", LABEL_PRINT_ONCE_TTL_DAYS, TimeUnit.DAYS); // 与整单标记保持相同保留期
    }

    private String labelPrintItemKey(Long orderId, Long printerId, Long itemId) // 构造单项幂等键
    {
        return LABEL_PRINT_ITEM_KEY_PREFIX + orderId + ":" + printerId + ":" + itemId; // 订单 + 打印机 + 明细 唯一定位一张标签任务
    }

    private List<LabelPrintTask> buildLabelTasks(ShopLabelPrinter printer, BizOrder order, BizOrderItem item) // 构造主标签和备注续页任务
    {
        List<String> remarkLines = wrapTextAll(order.getRemark(), 17); // 预留“备注 ”前缀宽度，主标签最多放两行
        int mainRemarkCount = Math.min(remarkLines.size(), LABEL_REMARK_MAX_LINES);
        List<LabelPrintTask> tasks = new ArrayList<>();
        tasks.add(new LabelPrintTask("", "主标签", buildLabelContent(printer, order, item,
                new ArrayList<>(remarkLines.subList(0, mainRemarkCount))))); // 主标签沿用旧幂等键，兼容已打印记录

        if (remarkLines.size() <= LABEL_REMARK_MAX_LINES)
        {
            return tasks;
        }
        List<String> overflowLines = new ArrayList<>(remarkLines.subList(LABEL_REMARK_MAX_LINES, remarkLines.size()));
        int linesPerPage = remarkContinuationLinesPerPage(printer.getPrintHeight());
        int pageCount = (overflowLines.size() + linesPerPage - 1) / linesPerPage;
        for (int page = 0; page < pageCount; page++)
        {
            int from = page * linesPerPage;
            int to = Math.min(from + linesPerPage, overflowLines.size());
            int pageNumber = page + 1;
            tasks.add(new LabelPrintTask(":REMARK:" + pageNumber, "备注续页" + pageNumber,
                    buildRemarkContinuationContent(printer, order, item,
                            new ArrayList<>(overflowLines.subList(from, to)), pageNumber, pageCount)));
        }
        return tasks;
    }

    private String buildLabelContent(ShopLabelPrinter printer, BizOrder order, BizOrderItem item,
            List<String> mainRemarkLines) // 构造单个商品的主标签内容
    {
        Integer printHeight = StringUtils.nvl(printer.getPrintHeight(), 50); // 标签高度，缺省 50mm
        LabelBuilder builder = new LabelBuilder(printHeight); // 订单打印沿用设备已配置尺寸，不重复发送 SIZE 指令

        builder.addFixedPickupQr(order.getPickupToken()); // 二维码固定在备注区域下方，先预留空间再绘制正文
        builder.addPickupLine(StringUtils.nvl(order.getPickupDisplay(), "--")); // 大字号显示取餐展示号（叫号码）
        builder.addGap(4); // 取餐号与商品名紧邻
        builder.addProductLines(wrapText(StringUtils.nvl(item.getProductName(), "未命名商品"), 12, 2)); // 商品名最多 2 行

        String specText = formatSpecText(item); // 格式化规格文本
        if (StringUtils.isNotBlank(specText)) // 有规格信息
        {
            builder.addGap(LABEL_SECTION_GAP); // 区块间距
            List<String> specLines = wrapText(specText, 17, 1); // 主标签空间有限，规格紧凑显示一行
            if (!specLines.isEmpty())
            {
                builder.addLine("规格 " + specLines.get(0), LABEL_NORMAL_LINE_HEIGHT);
            }
        }
        if (!mainRemarkLines.isEmpty()) // 有订单备注
        {
            builder.addGap(LABEL_SECTION_GAP); // 区块间距
            builder.addLine("备注 " + mainRemarkLines.get(0), LABEL_NORMAL_LINE_HEIGHT); // 第一行带备注标题
            if (mainRemarkLines.size() > 1)
            {
                builder.addLine(mainRemarkLines.get(1), LABEL_NORMAL_LINE_HEIGHT); // 主标签最多再放一行
            }
        }

        builder.addBottomLine(formatLabelBottom(order)); // 即时单显示下单时间，预订单优先显示预约时间
        if (builder.getOmittedLineCount() > 0)
        {
            log.warn("订单主标签内容超出可打印区域，部分文本已省略 orderId={} itemId={} printHeight={}mm omittedLines={}",
                    order.getId(), item.getId(), printHeight, builder.getOmittedLineCount());
        }
        return builder.build(); // 返回拼装好的标签指令字符串
    }

    private String formatLabelBottom(BizOrder order) // 构造主标签底部固定行
    {
        if (OrderTypeEnum.PREORDER.getCode().equals(order.getOrderType()) && order.getScheduledPickupTime() != null)
        {
            return "预约 " + order.getScheduledPickupTime().format(DATE_TIME);
        }
        return "下单 " + formatOrderTime(order);
    }

    private String buildRemarkContinuationContent(ShopLabelPrinter printer, BizOrder order, BizOrderItem item,
            List<String> remarkLines, int pageNumber, int pageCount) // 构造超长备注的续页标签
    {
        LabelBuilder builder = new LabelBuilder(StringUtils.nvl(printer.getPrintHeight(), 50));
        builder.addPickupLine(StringUtils.nvl(order.getPickupDisplay(), "--"));
        builder.addGap(4);
        builder.addLine(StringUtils.nvl(item.getProductName(), "未命名商品"), LABEL_NORMAL_LINE_HEIGHT);
        builder.addGap(10);
        builder.addLine("备注续页 " + pageNumber + "/" + pageCount, LABEL_NORMAL_LINE_HEIGHT);
        builder.addLines(remarkLines, LABEL_NORMAL_LINE_HEIGHT);
        builder.addBottomLine("订单 " + StringUtils.nvl(order.getOrderNo(), "--"));
        if (builder.getOmittedLineCount() > 0)
        {
            log.warn("订单备注续页内容超出可打印区域，部分文本已省略 orderId={} itemId={} page={}/{} printHeight={}mm omittedLines={}",
                    order.getId(), item.getId(), pageNumber, pageCount, printer.getPrintHeight(), builder.getOmittedLineCount());
        }
        return builder.build();
    }

    private int remarkContinuationLinesPerPage(Integer printHeight) // 按标签高度计算每张续页可容纳的备注行数
    {
        int contentBottom = mmToDots(StringUtils.nvl(printHeight, 50)) - 54;
        int firstRemarkY = LABEL_MARGIN + 48 + 4 + LABEL_NORMAL_LINE_HEIGHT + 10 + LABEL_NORMAL_LINE_HEIGHT;
        return Math.max(1, (contentBottom - firstRemarkY) / LABEL_NORMAL_LINE_HEIGHT + 1);
    }

    private String formatSpecText(BizOrderItem item) // 把订单明细的规格快照 JSON 格式化为展示文本
    {
        String specsJson = item.getSpecs();
        List<SpecSnapshot> specs;
        try
        {
            specs = StringUtils.isEmpty(specsJson) // 规格为空
                    ? Collections.emptyList() : JSON.parseArray(specsJson, SpecSnapshot.class); // 否则解析为规格快照列表
        }
        catch (Exception e)
        {
            log.warn("订单标签规格快照解析失败，使用异常提示占位 itemId={} specsLength={} errorType={}",
                    item.getId(), specsJson == null ? 0 : specsJson.length(), e.getClass().getSimpleName());
            return "规格信息异常";
        }
        if (specs.isEmpty()) // 没有任何规格
        {
            return "默认规格"; // 显示默认规格
        }
        List<String> labels = new ArrayList<>(); // 收集各规格的选项标签
        for (SpecSnapshot spec : specs) // 遍历规格
        {
            if (spec != null && StringUtils.isNotBlank(spec.getLabel())) // 规格对象和标签非空
            {
                labels.add(spec.getLabel()); // 加入标签列表
            }
        }
        return labels.isEmpty() ? "默认规格" : String.join(" / ", labels); // 用斜杠拼接，无标签则显示默认规格
    }

    private String formatOrderTime(BizOrder order) // 取订单的下单时间文本：优先支付时间，其次创建时间
    {
        if (order.getPayTime() != null) // 有支付时间
        {
            return order.getPayTime().format(DATE_TIME); // 用支付时间
        }
        if (order.getCreateTime() != null) // 有创建时间
        {
            return order.getCreateTime().format(DATE_TIME); // 用创建时间
        }
        return "--"; // 都没有则显示占位
    }

    private List<String> wrapText(String text, int maxUnits, int maxLines) // 按显示宽度对文本换行，控制最多行数
    {
        List<String> allLines = wrapTextAll(text, maxUnits);
        return allLines.size() <= maxLines ? allLines : new ArrayList<>(allLines.subList(0, maxLines));
    }

    private List<String> wrapTextAll(String text, int maxUnits) // 按显示宽度完整换行，不截断内容
    {
        if (StringUtils.isBlank(text)) // 空文本
        {
            return Collections.emptyList(); // 返回空列表
        }
        List<String> result = new ArrayList<>(); // 换行结果
        StringBuilder line = new StringBuilder(); // 当前行
        int units = 0; // 当前行已占的显示宽度单位
        for (int i = 0; i < text.length(); i++) // 逐字符遍历
        {
            char ch = text.charAt(i); // 当前字符
            if (ch == '\r')
            {
                continue;
            }
            if (ch == '\n')
            {
                if (line.length() > 0)
                {
                    result.add(line.toString());
                    line.setLength(0);
                    units = 0;
                }
                continue;
            }
            int charUnits = ch <= 127 ? 1 : 2; // ASCII 字符算 1 单位，中文等宽字符算 2 单位（近似显示宽度）
            if (units + charUnits > maxUnits) // 加上当前字符会超出行宽
            {
                result.add(line.toString()); // 当前行结束，加入结果
                line.setLength(0); // 重置当前行
                units = 0; // 重置宽度计数
            }
            line.append(ch); // 追加字符到当前行
            units += charUnits; // 累加宽度
        }
        if (line.length() > 0) // 还有未结束的行
        {
            result.add(line.toString()); // 加入最后一行
        }
        return result; // 返回换行结果
    }

    private String escapeXml(String value) // 对标签文本做 XML 转义，避免破坏飞鹅标签指令
    {
        return StringUtils.nvl(value, "") // null 视为空串
                .replace("&", "&amp;") // & 先转义
                .replace("<", "&lt;") // 小于号转义
                .replace(">", "&gt;") // 大于号转义
                .replace("\"", "&quot;"); // 双引号转义
    }

    private int mmToDots(Integer mm) // 毫米转打印点数（飞鹅标签机按 8 点/mm 换算）
    {
        return StringUtils.nvl(mm, 50) * 8; // 缺省 50mm，乘以 8 得点数
    }

    private final class LabelBuilder // 标签内容构建器：以 Y 坐标自增的方式拼装飞鹅标签 TEXT 指令
    {
        private final StringBuilder content = new StringBuilder(); // 标签指令字符串缓冲
        private int contentMaxY; // 内容区域允许的最大 Y 坐标（二维码会进一步收缩此区域）
        private final int bottomY; // 底部固定行的 Y 坐标
        private int y = LABEL_MARGIN; // 当前绘制 Y 坐标，从顶部留白开始
        private int omittedLineCount; // 因标签高度不足而省略的正文行数

        private LabelBuilder(Integer printHeight) // 构造器：沿用设备纸张尺寸，仅初始化方向与内容边界
        {
            int height = StringUtils.nvl(printHeight, 50);
            content.append("<DIRECTION>1</DIRECTION>"); // 设置出纸方向；订单任务不重复更新标签纸尺寸
            bottomY = mmToDots(height) - 36; // 底部行 Y = 总高度 - 36 点（留底部边距）
            contentMaxY = bottomY - 18; // 内容区上界比底部行再往上 18 点，避免与底部行重叠
        }

        private void addFixedPickupQr(String pickupToken) // 在备注下方的固定区域添加小尺寸核销二维码
        {
            if (StringUtils.isBlank(pickupToken)) // 历史订单或异常订单可能没有核销令牌
            {
                return; // 不输出空二维码
            }
            int qrX = LABEL_X; // 二维码固定在左下角，与正文左边距对齐
            int qrY = Math.max(LABEL_MARGIN, bottomY - LABEL_QR_RESERVED_SIZE - 8); // 固定在底部时间行上方
            content.append("<QR x=\"").append(qrX).append("\" y=\"").append(qrY)
                    .append("\" e=\"L\" w=\"").append(LABEL_QR_MODULE_WIDTH).append("\">")
                    .append(escapeXml(pickupToken)) // 二维码只编码原始 pickup_token，扫码结果可直接提交核销接口
                    .append("</QR>");
            contentMaxY = Math.min(contentMaxY, qrY - LABEL_NORMAL_LINE_HEIGHT); // 保证最后一行正文不会伸入二维码区域
        }

        private void addPickupLine(String text) // 添加取餐展示号
        {
            addText(text, 48, 12, 2, 2);
        }

        private void addProductLine(String text) // 添加一行商品名
        {
            addText(text, 44, 12, 2, 2);
        }

        private void addProductLines(List<String> lines) // 添加多行商品名
        {
            for (String line : lines) // 遍历每行
            {
                addProductLine(line); // 逐行添加
            }
        }

        private void addLine(String text, int lineHeight) // 添加一行普通文本
        {
            addText(text, lineHeight, 12, 1, 1); // 行高参数化，字号 12，缩放 1 倍
        }

        private void addLines(List<String> lines, int lineHeight) // 添加多行普通文本
        {
            for (String line : lines) // 遍历每行
            {
                addLine(line, lineHeight); // 逐行添加
            }
        }

        private void addBottomLine(String text) // 在底部固定位置添加一行（如下单时间）
        {
            if (StringUtils.isBlank(text)) // 文本为空
            {
                return; // 不绘制
            }
            appendText(text, bottomY, 12, 1, 1); // 固定在 bottomY 位置绘制
        }

        private void addGap(int gap) // 增加纵向间距
        {
            y += gap; // 当前 Y 下移指定点数
        }

        private void addText(String text, int lineHeight, int font, int widthScale, int heightScale) // 在当前 Y 绘制一行文本并下移行高
        {
            if (StringUtils.isBlank(text))
            {
                return;
            }
            if (y > contentMaxY) // 已超出内容区
            {
                omittedLineCount++;
                return; // 不绘制（防止溢出标签底部）
            }
            appendText(text, y, font, widthScale, heightScale); // 在当前 Y 绘制
            y += lineHeight; // Y 下移一个行高
        }

        private void appendText(String text, int textY, int font, int widthScale, int heightScale) // 拼接一条飞鹅 TEXT 指令
        {
            content.append("<TEXT x=\"").append(LABEL_X).append("\" y=\"").append(textY) // X 固定左边距，Y 为参数
                    .append("\" font=\"").append(font) // 字号
                    .append("\" w=\"").append(widthScale) // 宽度缩放
                    .append("\" h=\"").append(heightScale) // 高度缩放
                    .append("\" r=\"0\">") // 旋转角度 0
                    .append(escapeXml(text)) // 文本内容（已转义）
                    .append("</TEXT>"); // 指令闭合
        }

        private String build() // 返回拼装完成的标签指令字符串
        {
            return content.toString(); // 输出最终内容
        }

        private int getOmittedLineCount()
        {
            return omittedLineCount;
        }
    }

    private record LabelPrintTask(String keySuffix, String partName, String content) // 一次独立的飞鹅标签提交任务
    {
    }
} // OrderLabelPrintServiceImpl 类定义结束
