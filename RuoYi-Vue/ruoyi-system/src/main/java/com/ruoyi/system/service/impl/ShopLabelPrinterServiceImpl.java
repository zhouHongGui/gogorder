package com.ruoyi.system.service.impl; // 门店标签打印机业务实现所在包

import java.time.LocalDateTime; // 引入日期时间，用于记录最近状态查询时间
import java.util.ArrayList;
import java.util.List; // 引入集合类型
import java.util.Objects; // 引入对象比较工具，用于判断字段是否变化
import java.util.TreeSet;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Service; // 引入 Service 注解
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException; // 引入业务异常
import com.ruoyi.common.utils.StringUtils; // 引入字符串工具
import com.ruoyi.system.domain.Shop; // 引入门店实体，用于校验门店存在性
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入打印机实体
import com.ruoyi.system.domain.dto.FeiePrinterBindResult;
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅响应
import com.ruoyi.system.mapper.ShopLabelPrinterMapper; // 引入打印机 Mapper
import com.ruoyi.system.mapper.ShopMapper; // 引入门店 Mapper
import com.ruoyi.system.service.IFeiePrintService; // 引入飞鹅打印服务
import com.ruoyi.system.service.IShopLabelPrinterService; // 引入本服务接口

@Service // 声明为业务 Bean
public class ShopLabelPrinterServiceImpl implements IShopLabelPrinterService // 门店标签打印机业务实现：CRUD + 飞鹅联动 + 标签测试
{
    private static final Logger log = LoggerFactory.getLogger(ShopLabelPrinterServiceImpl.class);
    private static final String SN_OPERATION_LOCK_PREFIX = "printer:sn:operation:";
    private static final int SN_OPERATION_LOCK_TTL_MINUTES = 2;

    private final ShopLabelPrinterMapper printerMapper; // 打印机数据访问
    private final ShopMapper shopMapper; // 门店数据访问（校验门店）
    private final IFeiePrintService feiePrintService; // 飞鹅打印服务
    private final RedisCache redisCache;

    public ShopLabelPrinterServiceImpl(ShopLabelPrinterMapper printerMapper, ShopMapper shopMapper, // 构造器注入依赖
            IFeiePrintService feiePrintService, RedisCache redisCache)
    {
        this.printerMapper = printerMapper; // 保存打印机 Mapper
        this.shopMapper = shopMapper; // 保存门店 Mapper
        this.feiePrintService = feiePrintService; // 保存飞鹅服务
        this.redisCache = redisCache;
    }

    @Override
    public List<ShopLabelPrinter> selectShopLabelPrinterList(ShopLabelPrinter printer) // 列表查询
    {
        return printerMapper.selectShopLabelPrinterList(printer); // 直接委托 Mapper 按条件分页查询
    }

    @Override
    public ShopLabelPrinter selectShopLabelPrinterById(Long id) // 详情查询
    {
        return printerMapper.selectShopLabelPrinterById(id); // 委托 Mapper 查询（不含密钥）
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 飞鹅绑定不参与数据库事务，本地回滚时通过同步回调执行补偿解绑
    public int insertShopLabelPrinter(ShopLabelPrinter printer) // 新增打印机
    {
        normalizeAndValidate(printer, true); // 规范化并校验字段（新增时密钥必填）
        ensureShop(printer); // 校验门店存在并回填门店名
        acquireSnOperationLocks(printer.getSn());
        ensureSnAvailable(printer.getSn(), null); // 校验 SN 未被其他未删除设备占用
        FeiePrinterBindResult bindResult = feiePrintService.addLabelPrinter(printer); // 同步绑定到飞鹅平台
        printer.setFeieStatus(bindResult.response().getMsg()); // 保存飞鹅返回的状态描述
        printer.setLastStatusTime(LocalDateTime.now()); // 记录最近状态时间
        printer.setDelFlag(0); // 新增记录标记为未删除
        registerInsertRollbackCleanup(printer.getSn(), bindResult.newlyBound());
        int rows = printerMapper.insertShopLabelPrinter(printer); // 落库并返回影响行数
        if (rows != 1)
        {
            throw new ServiceException("标签打印机新增失败");
        }
        log.info("标签打印机新增成功 printerId={} shopId={} sn={}", printer.getId(), printer.getShopId(), printer.getSn());
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 新 SN 先绑定，本地提交后解绑旧 SN；回滚时补偿解绑本次新增绑定
    public int updateShopLabelPrinter(ShopLabelPrinter printer) // 修改打印机
    {
        ShopLabelPrinter current = printerMapper.selectShopLabelPrinterByIdWithKeyForUpdate(printer.getId()); // 锁定当前记录，避免并发修改产生远端孤儿绑定
        if (current == null) // 记录不存在
        {
            throw new ServiceException("标签打印机不存在"); // 抛出业务异常
        }
        normalizeAndValidate(printer, false); // 规范化并校验字段（修改时密钥可空）
        ensureShop(printer); // 校验门店存在并回填门店名
        acquireSnOperationLocks(current.getSn(), printer.getSn());
        ensureSnAvailable(printer.getSn(), printer.getId()); // 校验 SN 未被其他记录占用（排除自身）
        boolean snChanged = !Objects.equals(current.getSn(), printer.getSn()); // SN 是否发生变化
        if (snChanged && StringUtils.isBlank(printer.getPrinterKey()))
        {
            throw new ServiceException("修改打印机编号时必须填写新设备对应的密钥");
        }
        if (StringUtils.isBlank(printer.getPrinterKey())) // 未提交新密钥
        {
            printer.setPrinterKey(current.getPrinterKey()); // 沿用原密钥，避免覆盖为空
        }
        boolean keyProvided = StringUtils.isNotBlank(printer.getPrinterKey()) // 是否提供了新的密钥
                && !Objects.equals(current.getPrinterKey(), printer.getPrinterKey());
        if (snChanged || keyProvided) // SN 或密钥变化，飞鹅侧需要重新绑定
        {
            FeiePrinterBindResult bindResult = feiePrintService.addLabelPrinter(printer); // 重新绑定（已绑定则自动降级编辑）
            printer.setFeieStatus(bindResult.response().getMsg()); // 更新飞鹅状态描述
            printer.setLastStatusTime(LocalDateTime.now()); // 更新状态时间
            if (snChanged)
            {
                registerSnChangeSynchronization(printer.getId(), current.getSn(), printer.getSn(), bindResult.newlyBound());
            }
        }
        else if (!Objects.equals(current.getPrinterName(), printer.getPrinterName())) // 仅备注名变化
        {
            FeieResponse response = feiePrintService.editPrinter(printer.getSn(), displayName(printer)); // 同步更新飞鹅侧备注名
            printer.setFeieStatus(response.getMsg()); // 更新飞鹅状态描述
            printer.setLastStatusTime(LocalDateTime.now()); // 更新状态时间
        }
        int rows = printerMapper.updateShopLabelPrinter(printer); // 落库并返回影响行数
        if (rows != 1)
        {
            throw new ServiceException("标签打印机修改失败");
        }
        log.info("标签打印机修改成功 printerId={} shopId={} oldSn={} newSn={} snChanged={}",
                printer.getId(), printer.getShopId(), current.getSn(), printer.getSn(), snChanged);
        return rows;
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 飞鹅先解绑；本地删除回滚时尝试重新绑定原设备
    public int deleteShopLabelPrinterById(Long id) // 逻辑删除打印机
    {
        ShopLabelPrinter printer = printerMapper.selectShopLabelPrinterByIdWithKeyForUpdate(id); // 加行锁查询，串行化同一设备的并发删除
        if (printer == null) // 记录不存在或已删除
        {
            return 0; // 视为已删除，直接返回 0
        }
        acquireSnOperationLocks(printer.getSn());
        feiePrintService.deletePrinter(printer.getSn()); // 先在飞鹅平台解绑
        registerDeleteRollbackCompensation(printer);
        int rows = printerMapper.deleteShopLabelPrinterById(id); // 再逻辑删除本地记录（del_flag=1）
        if (rows != 1)
        {
            throw new ServiceException("标签打印机删除失败");
        }
        log.info("标签打印机删除成功 printerId={} shopId={} sn={}", id, printer.getShopId(), printer.getSn());
        return rows;
    }

    @Override
    public String queryPrinterStatus(Long id) // 查询并回写飞鹅状态
    {
        ShopLabelPrinter printer = requirePrinter(id); // 校验打印机存在
        FeieResponse response = feiePrintService.queryPrinterStatus(printer.getSn()); // 调用飞鹅状态查询
        String status = response.getData() == null ? response.getMsg() : String.valueOf(response.getData()); // 优先取 data，没有则取 msg
        printerMapper.updateFeieStatus(id, status); // 回写状态到本地
        log.info("飞鹅打印机状态查询完成 printerId={} shopId={} sn={} status={}",
                id, printer.getShopId(), printer.getSn(), status);
        return status; // 返回状态文本给前端展示
    }

    @Override
    public String printTestLabel(Long id) // 发送测试标签
    {
        ShopLabelPrinter printer = requirePrinter(id); // 校验打印机存在
        if (!Integer.valueOf(1).equals(printer.getStatus())) // 打印机未启用
        {
            throw new ServiceException("标签打印机已停用"); // 拒绝测试
        }
        Integer width = StringUtils.nvl(printer.getPrintWidth(), 40); // 标签宽度，缺省 40mm
        Integer height = StringUtils.nvl(printer.getPrintHeight(), 50); // 标签高度，缺省 50mm
        int qrX = 10; // 测试二维码与实际标签一致，固定在左下角
        int qrY = Math.max(24, height * 8 - 124); // 二维码贴近底部并为底部留白
        String content = "<DIRECTION>1</DIRECTION><SIZE>" + width + "," + height + "</SIZE>" // 标签方向与纸张尺寸
                + "<TEXT x=\"10\" y=\"40\">GOGORDER 标签测试</TEXT>" // 标题文本
                + "<TEXT x=\"10\" y=\"72\">纸张 " + width + "x" + height + "mm</TEXT>" // 显示纸张尺寸
                + "<TEXT x=\"10\" y=\"104\">" + escapeXml(displayName(printer)) + "</TEXT>" // 显示设备名（XML 转义）
                + "<TEXT x=\"10\" y=\"136\">SN:" + escapeXml(printer.getSn()) + "</TEXT>" // 显示 SN（XML 转义）
                + "<TEXT x=\"10\" y=\"168\">请检查方向、边距及二维码</TEXT>" // 提示文本
                + "<QR x=\"" + qrX + "\" y=\"" + qrY + "\" e=\"L\" w=\"3\">GOGORDER-QR-TEST</QR>"; // 小尺寸测试二维码
        FeieResponse response = feiePrintService.printLabel(printer.getSn(), content, 1); // 发送 1 份测试标签
        log.info("标签测试打印任务已提交 printerId={} shopId={} sn={} printSize={}x{}mm feieOrderId={} feieMsg={}",
                id, printer.getShopId(), printer.getSn(), width, height, response.getData(), response.getMsg());
        return response.getData() == null ? response.getMsg() : String.valueOf(response.getData()); // 返回飞鹅处理结果
    }

    private ShopLabelPrinter requirePrinter(Long id) // 按主键查询并校验打印机存在
    {
        ShopLabelPrinter printer = printerMapper.selectShopLabelPrinterById(id); // 查询记录（不含密钥）
        if (printer == null) // 不存在
        {
            throw new ServiceException("标签打印机不存在"); // 抛出业务异常
        }
        return printer; // 返回打印机
    }

    private void normalizeAndValidate(ShopLabelPrinter printer, boolean requireKey) // 规范化（去空格）并校验字段；requireKey 控制密钥是否必填
    {
        printer.setSn(StringUtils.trim(printer.getSn())); // SN 去首尾空格
        printer.setPrinterKey(StringUtils.trim(printer.getPrinterKey())); // 密钥去首尾空格
        printer.setPrinterName(StringUtils.trim(printer.getPrinterName())); // 备注名去首尾空格
        if (printer.getShopId() == null) // 未选门店
        {
            throw new ServiceException("请选择绑定门店"); // 提示选择门店
        }
        if (StringUtils.isBlank(printer.getSn()) || !printer.getSn().matches("\\d{6,32}")) // SN 为空或非 6-32 位数字
        {
            throw new ServiceException("打印机编号必须为6-32位数字"); // 提示 SN 格式
        }
        if (requireKey && StringUtils.isBlank(printer.getPrinterKey())) // 新增时密钥必填
        {
            throw new ServiceException("打印机设备密钥不能为空"); // 提示填写密钥
        }
        if (StringUtils.isNotBlank(printer.getPrinterKey()) && !printer.getPrinterKey().matches("[A-Za-z0-9_]{4,128}")) // 密钥格式校验
        {
            throw new ServiceException("打印机设备密钥格式不正确"); // 提示密钥格式
        }
        if (StringUtils.isNotBlank(printer.getPrinterName()))
        {
            if (printer.getPrinterName().length() > 100)
            {
                throw new ServiceException("设备名称不能超过100个字符");
            }
            if (printer.getPrinterName().contains("#") || printer.getPrinterName().contains("\r")
                    || printer.getPrinterName().contains("\n"))
            {
                throw new ServiceException("设备名称不能包含#或换行符");
            }
        }
        if (StringUtils.isNotBlank(printer.getRemark()) && printer.getRemark().length() > 200)
        {
            throw new ServiceException("备注不能超过200个字符");
        }
        printer.setPrintWidth(printer.getPrintWidth() == null ? 40 : printer.getPrintWidth()); // 宽度缺省 40mm
        printer.setPrintHeight(printer.getPrintHeight() == null ? 50 : printer.getPrintHeight()); // 高度缺省 50mm
        printer.setStatus(printer.getStatus() == null ? 1 : printer.getStatus()); // 状态缺省启用
        if (printer.getPrintWidth() < 20 || printer.getPrintWidth() > 120 // 宽度越界
                || printer.getPrintHeight() < 10 || printer.getPrintHeight() > 120) // 或高度越界
        {
            throw new ServiceException("标签纸尺寸范围为宽20-120mm、高10-120mm"); // 提示尺寸范围
        }
        if (!Integer.valueOf(0).equals(printer.getStatus()) && !Integer.valueOf(1).equals(printer.getStatus())) // 状态非 0/1
        {
            throw new ServiceException("打印机状态不正确"); // 提示状态取值
        }
    }

    private void ensureShop(ShopLabelPrinter printer) // 校验门店存在并回填门店名
    {
        Shop shop = shopMapper.selectShopById(printer.getShopId()); // 查询门店
        if (shop == null) // 门店不存在或已删除
        {
            throw new ServiceException("绑定门店不存在或已删除"); // 抛出业务异常
        }
        printer.setShopName(shop.getName()); // 回填门店名，供飞鹅备注名兜底使用
    }

    private void ensureSnAvailable(String sn, Long currentId) // 校验 SN 唯一性（currentId 为更新时的自身主键，需排除）
    {
        ShopLabelPrinter existing = printerMapper.selectActiveBySn(sn); // 查询是否已有未删除设备占用该 SN
        if (existing != null && !Objects.equals(existing.getId(), currentId)) // 被其他记录占用
        {
            throw new ServiceException("该打印机编号已绑定其他门店"); // 提示 SN 冲突
        }
    }

    private void registerInsertRollbackCleanup(String sn, boolean newlyBound)
    {
        if (!newlyBound)
        {
            return;
        }
        registerSynchronization("新增打印机", new TransactionSynchronization()
        {
            @Override
            public int getOrder()
            {
                return Ordered.LOWEST_PRECEDENCE - 100;
            }

            @Override
            public void afterCompletion(int status)
            {
                if (status != STATUS_COMMITTED)
                {
                    compensateUnbind(sn, "新增打印机本地事务回滚");
                }
            }
        });
    }

    private void registerSnChangeSynchronization(Long printerId, String oldSn, String newSn, boolean newlyBound)
    {
        registerSynchronization("修改打印机SN", new TransactionSynchronization()
        {
            @Override
            public int getOrder()
            {
                return Ordered.LOWEST_PRECEDENCE - 100;
            }

            @Override
            public void afterCommit()
            {
                try
                {
                    feiePrintService.deletePrinter(oldSn);
                    log.info("打印机SN变更后旧设备解绑成功 printerId={} oldSn={} newSn={}", printerId, oldSn, newSn);
                }
                catch (Exception e)
                {
                    log.error("打印机SN已更新但旧设备解绑失败，需人工核对 printerId={} oldSn={} newSn={}",
                            printerId, oldSn, newSn, e);
                }
            }

            @Override
            public void afterCompletion(int status)
            {
                if (status != STATUS_COMMITTED && newlyBound)
                {
                    compensateUnbind(newSn, "修改打印机SN本地事务回滚");
                }
            }
        });
    }

    private void registerDeleteRollbackCompensation(ShopLabelPrinter printer)
    {
        registerSynchronization("删除打印机", new TransactionSynchronization()
        {
            @Override
            public int getOrder()
            {
                return Ordered.LOWEST_PRECEDENCE - 100;
            }

            @Override
            public void afterCompletion(int status)
            {
                if (status == STATUS_COMMITTED)
                {
                    return;
                }
                try
                {
                    feiePrintService.addLabelPrinter(printer);
                    log.warn("标签打印机本地删除回滚，已重新绑定飞鹅设备 printerId={} sn={}", printer.getId(), printer.getSn());
                }
                catch (Exception e)
                {
                    log.error("标签打印机本地删除回滚且飞鹅设备恢复失败，需人工核对 printerId={} sn={}",
                            printer.getId(), printer.getSn(), e);
                }
            }
        });
    }

    private void registerSynchronization(String operation, TransactionSynchronization synchronization)
    {
        if (!TransactionSynchronizationManager.isSynchronizationActive())
        {
            throw new IllegalStateException(operation + "必须在事务中执行");
        }
        TransactionSynchronizationManager.registerSynchronization(synchronization);
    }

    private void acquireSnOperationLocks(String... sns)
    {
        TreeSet<String> sortedSnSet = new TreeSet<>();
        for (String sn : sns)
        {
            if (StringUtils.isNotBlank(sn))
            {
                sortedSnSet.add(sn);
            }
        }
        List<SnOperationLock> acquiredLocks = new ArrayList<>();
        try
        {
            for (String sn : sortedSnSet)
            {
                String key = SN_OPERATION_LOCK_PREFIX + sn;
                String owner = UUID.randomUUID().toString();
                if (!redisCache.setCacheObjectIfAbsent(key, owner, SN_OPERATION_LOCK_TTL_MINUTES, TimeUnit.MINUTES))
                {
                    throw new ServiceException("打印机正在执行绑定或解绑操作，请稍后重试");
                }
                acquiredLocks.add(new SnOperationLock(key, owner));
            }
            registerSynchronization("打印机SN操作锁", new TransactionSynchronization()
            {
                @Override
                public int getOrder()
                {
                    return Ordered.LOWEST_PRECEDENCE;
                }

                @Override
                public void afterCompletion(int status)
                {
                    releaseSnOperationLocks(acquiredLocks);
                }
            });
        }
        catch (Exception e)
        {
            releaseSnOperationLocks(acquiredLocks);
            throw e;
        }
    }

    private void releaseSnOperationLocks(List<SnOperationLock> locks)
    {
        for (SnOperationLock lock : locks)
        {
            try
            {
                redisCache.releaseLock(lock.key(), lock.owner());
            }
            catch (Exception e)
            {
                log.error("打印机SN操作锁释放失败 key={}", lock.key(), e);
            }
        }
    }

    private record SnOperationLock(String key, String owner)
    {
    }

    private void compensateUnbind(String sn, String reason)
    {
        try
        {
            feiePrintService.deletePrinter(sn);
            log.warn("飞鹅设备补偿解绑成功 reason={} sn={}", reason, sn);
        }
        catch (Exception e)
        {
            log.error("飞鹅设备补偿解绑失败，需人工核对 reason={} sn={}", reason, sn, e);
        }
    }

    private String displayName(ShopLabelPrinter printer) // 取展示名：设备名 > 门店名 > SN
    {
        if (StringUtils.isNotBlank(printer.getPrinterName())) // 有设备名
        {
            return printer.getPrinterName(); // 返回设备名
        }
        if (StringUtils.isNotBlank(printer.getShopName())) // 有门店名
        {
            return printer.getShopName(); // 返回门店名
        }
        return printer.getSn(); // 都没有则返回 SN
    }

    private String escapeXml(String value) // 对标签文本做 XML 转义，避免破坏飞鹅标签指令
    {
        return StringUtils.nvl(value, "") // null 视为空串
                .replace("&", "&amp;") // & 必须先转义
                .replace("<", "&lt;") // 小于号转义
                .replace(">", "&gt;") // 大于号转义
                .replace("\"", "&quot;"); // 双引号转义
    }
} // ShopLabelPrinterServiceImpl 类定义结束
