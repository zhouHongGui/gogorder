package com.ruoyi.system.service.impl; // 飞鹅打印服务实现所在包

import java.util.Map; // 引入 Map，用于组装各接口的业务参数
import org.springframework.stereotype.Service; // 引入 Service 注解，声明为业务 Bean
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.exception.ServiceException; // 引入若依业务异常
import com.ruoyi.common.utils.StringUtils; // 引入字符串工具，用于判空和兜底
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入门店标签打印机实体
import com.ruoyi.system.domain.dto.FeiePrinterBindResult;
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
    public FeiePrinterBindResult addLabelPrinter(ShopLabelPrinter printer) // 绑定打印机到飞鹅平台，若已绑定则降级为编辑备注
    {
        String content = printer.getSn() + "#" + printer.getPrinterKey() + "#" + safeName(printer) + "#"; // 飞鹅要求的多设备格式：SN#密钥#名称#（单个设备以 # 结尾）
        FeieResponse response = feieClient.post("Open_printerAddlist", Map.of("printerContent", content)); // 调用批量新增接口
        String failedEntry = findBatchEntry(response, "no", printer.getSn());
        if (isAlreadyAdded(response) || isAlreadyAdded(failedEntry)) // 该 SN 已经在飞鹅后台绑定过
        {
            return new FeiePrinterBindResult(editPrinter(printer.getSn(), safeName(printer)), false); // 仅更新备注，不把既有设备视为本次新绑定
        }
        if (StringUtils.isNotBlank(failedEntry))
        {
            throw new ServiceException("飞鹅绑定失败：" + failedEntry);
        }
        if (response != null && response.isSuccess()) // 绑定成功
        {
            ensureBatchSuccess(response, printer.getSn(), "飞鹅绑定失败");
            return new FeiePrinterBindResult(response, true);
        }
        throw new ServiceException("飞鹅绑定失败：" + (response == null ? "无响应" : StringUtils.nvl(response.getMsg(), "未知错误"))); // 其他失败直接抛出业务异常
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
        String failedEntry = findBatchEntry(response, "no", sn);
        if (isPrinterAbsent(response) || isPrinterAbsent(failedEntry)) // 重复解绑按成功处理，使删除操作具备幂等性
        {
            return response;
        }
        if (StringUtils.isNotBlank(failedEntry))
        {
            throw new ServiceException("飞鹅打印机解绑失败：" + failedEntry);
        }
        ensureSuccess(response, "飞鹅打印机解绑失败"); // 校验成功，否则抛异常
        ensureBatchSuccess(response, sn, "飞鹅打印机解绑失败");
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
        if (response == null)
        {
            return false;
        }
        String msg = StringUtils.nvl(response.getMsg(), ""); // 取提示信息，空则视为空串
        String data = response.getData() == null ? "" : String.valueOf(response.getData()); // 取数据字段，空则视为空串
        return msg.contains("已被添加") || msg.contains("已经添加") || msg.contains("已添加") // 兼容飞鹅不同措辞的提示文本（msg 维度）
                || data.contains("已被添加") || data.contains("已经添加") || data.contains("已添加"); // 兼容 data 维度的提示文本
    }

    private boolean isAlreadyAdded(String text)
    {
        return StringUtils.isNotBlank(text)
                && (text.contains("已被添加") || text.contains("已经添加") || text.contains("已添加"));
    }

    private boolean isPrinterAbsent(FeieResponse response)
    {
        if (response == null)
        {
            return false;
        }
        String text = StringUtils.nvl(response.getMsg(), "") + (response.getData() == null ? "" : String.valueOf(response.getData()));
        return isPrinterAbsent(text);
    }

    private boolean isPrinterAbsent(String text)
    {
        if (StringUtils.isBlank(text))
        {
            return false;
        }
        return text.contains("打印机不存在") || text.contains("设备不存在")
                || text.contains("打印机未添加") || text.contains("未添加该打印机")
                || text.contains("打印机未绑定") || text.contains("未绑定该打印机");
    }

    private void ensureBatchSuccess(FeieResponse response, String sn, String message)
    {
        JSONObject data = batchData(response);
        if (data == null || (!data.containsKey("ok") && !data.containsKey("no")))
        {
            return;
        }
        if (findBatchEntry(response, "ok", sn) == null)
        {
            throw new ServiceException(message + "：飞鹅返回中未包含设备成功结果");
        }
    }

    private String findBatchEntry(FeieResponse response, String field, String sn)
    {
        JSONObject data = batchData(response);
        if (data == null)
        {
            return null;
        }
        JSONArray entries = data.getJSONArray(field);
        if (entries == null)
        {
            return null;
        }
        for (Object item : entries)
        {
            String entry = String.valueOf(item);
            if (entry.equals(sn) || entry.startsWith(sn + "(") || entry.startsWith(sn + "（"))
            {
                return entry;
            }
        }
        return null;
    }

    private JSONObject batchData(FeieResponse response)
    {
        if (response == null || response.getData() == null)
        {
            return null;
        }
        try
        {
            return response.getData() instanceof JSONObject object ? object
                    : JSON.parseObject(JSON.toJSONString(response.getData()));
        }
        catch (Exception e)
        {
            return null;
        }
    }

    private String safeName(ShopLabelPrinter printer) // 取一个安全的备注名：优先设备名，其次门店名，最后 SN
    {
        if (StringUtils.isNotBlank(printer.getPrinterName())) // 设备名非空
        {
            return sanitizeName(printer.getPrinterName()); // 用设备名作为飞鹅侧备注
        }
        return sanitizeName(StringUtils.nvl(printer.getShopName(), printer.getSn())); // 否则用门店名，门店名也空则用 SN
    }

    private String sanitizeName(String name)
    {
        String sanitized = StringUtils.nvl(name, "").replace('#', ' ').replace('\r', ' ').replace('\n', ' ').trim();
        return sanitized.length() <= 100 ? sanitized : sanitized.substring(0, 100);
    }
} // FeiePrintServiceImpl 类定义结束
