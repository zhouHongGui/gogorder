package com.ruoyi.system.service.impl; // 门店标签打印机业务实现所在包

import java.time.LocalDateTime; // 引入日期时间，用于记录最近状态查询时间
import java.util.List; // 引入集合类型
import java.util.Objects; // 引入对象比较工具，用于判断字段是否变化
import org.springframework.stereotype.Service; // 引入 Service 注解
import org.springframework.transaction.annotation.Transactional; // 引入事务注解，保证飞鹅操作与本地落库的一致性
import com.ruoyi.common.exception.ServiceException; // 引入业务异常
import com.ruoyi.common.utils.StringUtils; // 引入字符串工具
import com.ruoyi.system.domain.Shop; // 引入门店实体，用于校验门店存在性
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入打印机实体
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅响应
import com.ruoyi.system.mapper.ShopLabelPrinterMapper; // 引入打印机 Mapper
import com.ruoyi.system.mapper.ShopMapper; // 引入门店 Mapper
import com.ruoyi.system.service.IFeiePrintService; // 引入飞鹅打印服务
import com.ruoyi.system.service.IShopLabelPrinterService; // 引入本服务接口

@Service // 声明为业务 Bean
public class ShopLabelPrinterServiceImpl implements IShopLabelPrinterService // 门店标签打印机业务实现：CRUD + 飞鹅联动 + 标签测试
{
    private final ShopLabelPrinterMapper printerMapper; // 打印机数据访问
    private final ShopMapper shopMapper; // 门店数据访问（校验门店）
    private final IFeiePrintService feiePrintService; // 飞鹅打印服务

    public ShopLabelPrinterServiceImpl(ShopLabelPrinterMapper printerMapper, ShopMapper shopMapper, // 构造器注入依赖
            IFeiePrintService feiePrintService)
    {
        this.printerMapper = printerMapper; // 保存打印机 Mapper
        this.shopMapper = shopMapper; // 保存门店 Mapper
        this.feiePrintService = feiePrintService; // 保存飞鹅服务
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
    @Transactional(rollbackFor = Exception.class) // 本地落库事务化：飞鹅绑定在插入前调用，失败抛异常可阻止插入；注意飞鹅远程调用不在事务内，无法回滚
    public int insertShopLabelPrinter(ShopLabelPrinter printer) // 新增打印机
    {
        normalizeAndValidate(printer, true); // 规范化并校验字段（新增时密钥必填）
        ensureShop(printer); // 校验门店存在并回填门店名
        ensureSnAvailable(printer.getSn(), null); // 校验 SN 未被其他未删除设备占用
        FeieResponse response = feiePrintService.addLabelPrinter(printer); // 同步绑定到飞鹅平台
        printer.setFeieStatus(response.getMsg()); // 保存飞鹅返回的状态描述
        printer.setLastStatusTime(LocalDateTime.now()); // 记录最近状态时间
        printer.setDelFlag(0); // 新增记录标记为未删除
        return printerMapper.insertShopLabelPrinter(printer); // 落库并返回影响行数
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 本地落库事务化：飞鹅变更失败抛异常可阻止更新；注意飞鹅远程调用不在事务内，无法回滚
    public int updateShopLabelPrinter(ShopLabelPrinter printer) // 修改打印机
    {
        ShopLabelPrinter current = printerMapper.selectShopLabelPrinterByIdWithKey(printer.getId()); // 查询当前记录（含密钥）
        if (current == null) // 记录不存在
        {
            throw new ServiceException("标签打印机不存在"); // 抛出业务异常
        }
        normalizeAndValidate(printer, false); // 规范化并校验字段（修改时密钥可空）
        ensureShop(printer); // 校验门店存在并回填门店名
        ensureSnAvailable(printer.getSn(), printer.getId()); // 校验 SN 未被其他记录占用（排除自身）
        if (StringUtils.isBlank(printer.getPrinterKey())) // 未提交新密钥
        {
            printer.setPrinterKey(current.getPrinterKey()); // 沿用原密钥，避免覆盖为空
        }
        boolean snChanged = !Objects.equals(current.getSn(), printer.getSn()); // SN 是否发生变化
        boolean keyProvided = StringUtils.isNotBlank(printer.getPrinterKey()) // 是否提供了新的密钥
                && !Objects.equals(current.getPrinterKey(), printer.getPrinterKey());
        if (snChanged || keyProvided) // SN 或密钥变化，飞鹅侧需要重新绑定
        {
            FeieResponse response = feiePrintService.addLabelPrinter(printer); // 重新绑定（已绑定则自动降级编辑）
            printer.setFeieStatus(response.getMsg()); // 更新飞鹅状态描述
            printer.setLastStatusTime(LocalDateTime.now()); // 更新状态时间
        }
        else if (!Objects.equals(current.getPrinterName(), printer.getPrinterName())) // 仅备注名变化
        {
            FeieResponse response = feiePrintService.editPrinter(printer.getSn(), displayName(printer)); // 同步更新飞鹅侧备注名
            printer.setFeieStatus(response.getMsg()); // 更新飞鹅状态描述
            printer.setLastStatusTime(LocalDateTime.now()); // 更新状态时间
        }
        return printerMapper.updateShopLabelPrinter(printer); // 落库并返回影响行数
    }

    @Override
    @Transactional(rollbackFor = Exception.class) // 本地落库事务化：飞鹅解绑在删除前调用，失败抛异常可阻止删除；注意飞鹅远程调用不在事务内，无法回滚
    public int deleteShopLabelPrinterById(Long id) // 逻辑删除打印机
    {
        ShopLabelPrinter printer = printerMapper.selectShopLabelPrinterByIdWithKey(id); // 查询待删除记录（含 SN）
        if (printer == null) // 记录不存在或已删除
        {
            return 0; // 视为已删除，直接返回 0
        }
        feiePrintService.deletePrinter(printer.getSn()); // 先在飞鹅平台解绑
        return printerMapper.deleteShopLabelPrinterById(id); // 再逻辑删除本地记录（del_flag=1）
    }

    @Override
    public String queryPrinterStatus(Long id) // 查询并回写飞鹅状态
    {
        ShopLabelPrinter printer = requirePrinter(id); // 校验打印机存在
        FeieResponse response = feiePrintService.queryPrinterStatus(printer.getSn()); // 调用飞鹅状态查询
        String status = response.getData() == null ? response.getMsg() : String.valueOf(response.getData()); // 优先取 data，没有则取 msg
        printerMapper.updateFeieStatus(id, status); // 回写状态到本地
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
        String content = "<DIRECTION>1</DIRECTION><SIZE>" + width + "," + height + "</SIZE>" // 标签方向与纸张尺寸
                + "<TEXT x=\"10\" y=\"40\">GOGORDER 标签测试</TEXT>" // 标题文本
                + "<TEXT x=\"10\" y=\"72\">纸张 " + width + "x" + height + "mm</TEXT>" // 显示纸张尺寸
                + "<TEXT x=\"10\" y=\"104\">" + escapeXml(displayName(printer)) + "</TEXT>" // 显示设备名（XML 转义）
                + "<TEXT x=\"10\" y=\"136\">SN:" + escapeXml(printer.getSn()) + "</TEXT>" // 显示 SN（XML 转义）
                + "<TEXT x=\"10\" y=\"168\">请检查方向与边距</TEXT>"; // 提示文本
        FeieResponse response = feiePrintService.printLabel(printer.getSn(), content, 1); // 发送 1 份测试标签
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
