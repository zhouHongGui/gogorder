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
    private static final int LABEL_MARGIN = 40; // 标签内容起始 Y 坐标（点），约等于顶部留白
    private static final int LABEL_X = 10; // 标签文本固定 X 坐标（点），即左边距
    private static final int LABEL_SECTION_GAP = 18; // 标签各区块之间的纵向间距（点）
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
        taskExecutor.execute(() -> { // 提交到线程池异步执行，立即返回不阻塞调用方
            try
            {
                printPaidOrder(orderId); // 执行实际打印逻辑
            }
            catch (Exception e) // 异步任务抛出异常
            {
                log.warn("支付后打印失败 orderId={}", orderId, e); // 仅记录警告，不影响支付结果
            }
        });
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
            if (isAlreadyPrinted(orderId)) // 自动打印已有「整单成功标记」，说明此前已成功打印过
            {
                log.info("支付后打印跳过：订单标签已打印成功 orderId={}", orderId); // 记录跳过原因
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

            int totalCount = printers.size() * items.size(); // 任务数按「打印机 x 商品明细」计算，数量份数由 times 传给飞鹅
            int successCount = 0; // 成功发送或已成功标记覆盖的任务数
            int failedCount = 0; // 发送失败任务数
            int skippedCount = 0; // 因单项幂等标记跳过的任务数
            for (ShopLabelPrinter printer : printers) // 遍历门店的每台启用打印机
            {
                for (BizOrderItem item : items) // 遍历订单的每个商品明细
                {
                    int times = Math.max(StringUtils.nvl(item.getQuantity(), 1), 1); // 该商品的打印份数 = 购买数量，至少 1
                    String itemKey = labelPrintItemKey(orderId, printer.getId(), item.getId()); // 单项幂等键：订单+打印机+明细
                    if (isItemPrinted(itemKey)) // 自动打印遇到单项成功标记时跳过该任务
                    {
                        successCount++;
                        skippedCount++;
                        log.info("支付后打印单项跳过：已成功打印过 orderId={} printerId={} itemId={}",
                                orderId, printer.getId(), item.getId());
                        continue;
                    }
                    try
                    {
                        feiePrintService.printLabel(printer.getSn(), buildLabelContent(printer, order, item), times); // 按数量打印标签
                        successCount++; // 累加成功计数
                        markItemPrinted(itemKey); // 写入单项成功标记，后续自动重试不会重复打印已成功项
                        log.info("支付后打印成功 orderId={} printerId={} sn={} itemId={} product={} times={}", // 记录成功日志
                                orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), times);
                    }
                    catch (Exception e) // 单项打印失败
                    {
                        failedCount++; // 累加失败计数
                        log.warn("支付后打印单项失败 orderId={} printerId={} sn={} itemId={} product={} times={}", // 记录失败日志，不中断后续项
                                orderId, printer.getId(), printer.getSn(), item.getId(), item.getProductName(), times, e);
                    }
                }
            }
            if (totalCount > 0 && failedCount == 0 && successCount == totalCount) // 全部任务均成功或已有单项成功标记覆盖
            {
                markPrinted(orderId); // 写入「整单成功标记」，后续自动重复触发将跳过
            }
            else if (successCount == 0) // 全部失败
            {
                log.warn("支付后打印全部失败，未写入整单成功标记，后续再次触发可重试 orderId={}", orderId); // 不写整单标记以便重试
            }
            else // 部分成功
            {
                log.warn("支付后打印部分成功，未写入整单成功标记 orderId={} success={} failed={} skipped={}",
                        orderId, successCount, failedCount, skippedCount); // 保留未成功项的重试空间
            }
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

    private String buildLabelContent(ShopLabelPrinter printer, BizOrder order, BizOrderItem item) // 构造单个商品的标签内容
    {
        Integer printWidth = StringUtils.nvl(printer.getPrintWidth(), 40); // 标签宽度，缺省 40mm
        Integer printHeight = StringUtils.nvl(printer.getPrintHeight(), 50); // 标签高度，缺省 50mm
        LabelBuilder builder = new LabelBuilder(printWidth, printHeight); // 创建标签构建器，初始化方向与边界

        builder.addH1Line(StringUtils.nvl(order.getPickupDisplay(), "--")); // 大字号显示取餐展示号（叫号码）
        builder.addGap(22); // 与下一区块留白
        builder.addH1Lines(wrapText(StringUtils.nvl(item.getProductName(), "未命名商品"), 12, 2)); // 大字号显示商品名（最多 2 行）

        String specText = formatSpecText(item.getSpecs()); // 格式化规格文本
        if (StringUtils.isNotBlank(specText)) // 有规格信息
        {
            builder.addGap(LABEL_SECTION_GAP); // 区块间距
            builder.addLine("规格", 28); // "规格"小标题
            builder.addLines(wrapText(specText, 22, 4), 30); // 规格正文（最多 4 行）
        }
        if (StringUtils.isNotBlank(order.getRemark())) // 有订单备注
        {
            builder.addGap(LABEL_SECTION_GAP); // 区块间距
            builder.addLine("备注", 28); // "备注"小标题
            builder.addLines(wrapText(order.getRemark(), 22, 3), 30); // 备注正文（最多 3 行）
        }

        if (OrderTypeEnum.PREORDER.getCode().equals(order.getOrderType()) && order.getScheduledPickupTime() != null) // 预订单且有预约时间
        {
            builder.addGap(LABEL_SECTION_GAP); // 区块间距
            builder.addLine("预约 " + order.getScheduledPickupTime().format(DATE_TIME), 30); // 显示预约取餐时间
        }

        builder.addBottomLine("下单 " + formatOrderTime(order)); // 底部固定位置显示下单时间
        return builder.build(); // 返回拼装好的标签指令字符串
    }

    private String formatSpecText(String specsJson) // 把订单明细的规格快照 JSON 格式化为展示文本
    {
        List<SpecSnapshot> specs = StringUtils.isEmpty(specsJson) // 规格为空
                ? Collections.emptyList() : JSON.parseArray(specsJson, SpecSnapshot.class); // 否则解析为规格快照列表
        if (specs.isEmpty()) // 没有任何规格
        {
            return "默认规格"; // 显示默认规格
        }
        List<String> labels = new ArrayList<>(); // 收集各规格的选项标签
        for (SpecSnapshot spec : specs) // 遍历规格
        {
            if (StringUtils.isNotBlank(spec.getLabel())) // 标签非空
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
            int charUnits = ch <= 127 ? 1 : 2; // ASCII 字符算 1 单位，中文等宽字符算 2 单位（近似显示宽度）
            if (units + charUnits > maxUnits) // 加上当前字符会超出行宽
            {
                result.add(line.toString()); // 当前行结束，加入结果
                if (result.size() >= maxLines) // 已达最大行数
                {
                    return result; // 提前返回，超出部分截断
                }
                line.setLength(0); // 重置当前行
                units = 0; // 重置宽度计数
            }
            line.append(ch); // 追加字符到当前行
            units += charUnits; // 累加宽度
        }
        if (line.length() > 0 && result.size() < maxLines) // 还有未结束的行且未超最大行数
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
        private final int contentMaxY; // 内容区域允许的最大 Y 坐标（超过则不再绘制）
        private final int bottomY; // 底部固定行的 Y 坐标
        private int y = LABEL_MARGIN; // 当前绘制 Y 坐标，从顶部留白开始

        private LabelBuilder(Integer printWidth, Integer printHeight) // 构造器：初始化标签方向与底部坐标
        {
            int width = StringUtils.nvl(printWidth, 40);
            int height = StringUtils.nvl(printHeight, 50);
            content.append("<DIRECTION>1</DIRECTION>") // 设置出纸方向（1 为默认正向）
                    .append("<SIZE>").append(width).append(",").append(height).append("</SIZE>"); // 按设备配置设置标签纸尺寸
            bottomY = mmToDots(height) - 36; // 底部行 Y = 总高度 - 36 点（留底部边距）
            contentMaxY = bottomY - 18; // 内容区上界比底部行再往上 18 点，避免与底部行重叠
        }

        private void addH1Line(String text) // 添加一行大标题（商品名/取餐号）
        {
            addText(text, 58, 12, 2, 2); // 行高 58，字号 12，宽高缩放各 2 倍
        }

        private void addH1Lines(List<String> lines) // 添加多行大标题
        {
            for (String line : lines) // 遍历每行
            {
                addH1Line(line); // 逐行添加
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
            if (StringUtils.isBlank(text) || y > contentMaxY) // 文本为空或已超出内容区
            {
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
    }
} // OrderLabelPrintServiceImpl 类定义结束
