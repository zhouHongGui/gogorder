package com.ruoyi.system.service.impl; // 飞鹅打印服务实现所在包

import java.util.Map; // 引入 Map，用于组装各接口的业务参数
import org.springframework.stereotype.Service; // 引入 Service 注解，声明为业务 Bean
import com.ruoyi.common.exception.ServiceException; // 引入若依业务异常
import com.ruoyi.common.utils.StringUtils; // 引入字符串工具，用于判空和兜底
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入门店标签打印机实体
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅响应对象
import com.ruoyi.system.service.IFeiePrintService; // 引入飞鹅打印服务接口

@Service // 声明为业务 Bean，实现 IFeiePrintService
public class FeiePrintServiceImpl implements IFeiePrintService // 飞鹅打印服务实现：在 FeieClient 基础上封装各业务接口和错误处理
{
    private final FeieClient feieClient; // 飞鹅底层 HTTP 客户端

    public FeiePrintServiceImpl(FeieClient feieClient) // 构造器注入飞鹅客户端
    {
        this.feieClient = feieClient; // 保存客户端引用
    }

    @Override
    public FeieResponse addLabelPrinter(ShopLabelPrinter printer) // 绑定打印机到飞鹅平台，若已绑定则降级为编辑备注
    {
        String content = printer.getSn() + "#" + printer.getPrinterKey() + "#" + safeName(printer) + "#"; // 飞鹅要求的多设备格式：SN#密钥#名称#（单个设备以 # 结尾）
        FeieResponse response = feieClient.post("Open_printerAddlist", Map.of("printerContent", content)); // 调用批量新增接口
        if (response.isSuccess()) // 绑定成功
        {
            return response; // 直接返回
        }
        if (isAlreadyAdded(response)) // 该 SN 已经在飞鹅后台绑定过
        {
            return editPrinter(printer.getSn(), safeName(printer)); // 降级为更新备注名，保证本地与飞鹅一致
        }
        throw new ServiceException("飞鹅绑定失败：" + StringUtils.nvl(response.getMsg(), "未知错误")); // 其他失败直接抛出业务异常
    }

    @Override
    public FeieResponse editPrinter(String sn, String name) // 修改飞鹅打印机备注名
    {
        FeieResponse response = feieClient.post("Open_printerEdit", Map.of("sn", sn, "name", StringUtils.nvl(name, ""))); // 调用编辑接口，name 为空时传空串
        ensureSuccess(response, "飞鹅打印机备注更新失败"); // 校验成功，否则抛异常
        return response; // 返回响应
    }

    @Override
    public FeieResponse deletePrinter(String sn) // 从飞鹅平台解绑打印机
    {
        FeieResponse response = feieClient.post("Open_printerDelList", Map.of("snlist", sn)); // 调用删除接口，snlist 为 SN 列表（单个即该 SN）
        ensureSuccess(response, "飞鹅打印机解绑失败"); // 校验成功，否则抛异常
        return response; // 返回响应
    }

    @Override
    public FeieResponse queryPrinterStatus(String sn) // 查询打印机在线/工作状态
    {
        FeieResponse response = feieClient.post("Open_queryPrinterStatus", Map.of("sn", sn)); // 调用状态查询接口
        ensureSuccess(response, "飞鹅打印机状态查询失败"); // 校验成功，否则抛异常
        return response; // 返回响应（状态文本在 data 或 msg 中）
    }

    @Override
    public FeieResponse printLabel(String sn, String content, int times) // 发送标签打印任务
    {
        FeieResponse response = feieClient.post("Open_printLabelMsg", Map.of( // 调用标签打印接口
                "sn", sn, // 目标打印机 SN
                "content", content, // 标签内容（飞鹅 TSPL/标签指令 XML）
                "times", String.valueOf(times))); // 打印份数，飞鹅要求字符串
        ensureSuccess(response, "飞鹅标签测试打印失败"); // 校验成功，否则抛异常
        return response; // 返回响应
    }

    private void ensureSuccess(FeieResponse response, String message) // 统一的成功校验：失败则抛业务异常
    {
        if (response == null || !response.isSuccess()) // 无响应或返回码非 0
        {
            throw new ServiceException(message + "：" + (response == null ? "无响应" : StringUtils.nvl(response.getMsg(), "未知错误"))); // 拼接飞鹅提示信息后抛出
        }
    }

    private boolean isAlreadyAdded(FeieResponse response) // 判断飞鹅返回是否表示"该 SN 已被添加过"
    {
        String msg = StringUtils.nvl(response.getMsg(), ""); // 取提示信息，空则视为空串
        String data = response.getData() == null ? "" : String.valueOf(response.getData()); // 取数据字段，空则视为空串
        return msg.contains("已被添加") || msg.contains("已经添加") || msg.contains("已添加") // 兼容飞鹅不同措辞的提示文本（msg 维度）
                || data.contains("已被添加") || data.contains("已经添加") || data.contains("已添加"); // 兼容 data 维度的提示文本
    }

    private String safeName(ShopLabelPrinter printer) // 取一个安全的备注名：优先设备名，其次门店名，最后 SN
    {
        if (StringUtils.isNotBlank(printer.getPrinterName())) // 设备名非空
        {
            return printer.getPrinterName(); // 用设备名作为飞鹅侧备注
        }
        return StringUtils.nvl(printer.getShopName(), printer.getSn()); // 否则用门店名，门店名也空则用 SN
    }
} // FeiePrintServiceImpl 类定义结束
