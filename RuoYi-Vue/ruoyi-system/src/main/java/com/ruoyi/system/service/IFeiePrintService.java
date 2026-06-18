package com.ruoyi.system.service; // 飞鹅打印服务接口所在包

import com.ruoyi.system.domain.ShopLabelPrinter; // 引入门店标签打印机实体，绑定设备时需要
import com.ruoyi.system.domain.dto.FeiePrinterBindResult;
import com.ruoyi.system.domain.dto.FeieResponse; // 引入飞鹅统一响应对象，作为各方法返回值

public interface IFeiePrintService // 飞鹅云打印开放平台服务接口：封装与飞鹅 API 的交互
{
    FeiePrinterBindResult addLabelPrinter(ShopLabelPrinter printer); // 绑定打印机，并标识本次是否新建了远端绑定

    FeieResponse editPrinter(String sn, String name); // 修改飞鹅打印机备注名（Open_printerEdit）

    FeieResponse deletePrinter(String sn); // 从飞鹅平台解绑打印机（Open_printerDelList）

    FeieResponse queryPrinterStatus(String sn); // 查询飞鹅打印机在线/工作状态（Open_queryPrinterStatus）

    FeieResponse printLabel(String sn, String content, int times); // 向指定打印机发送标签打印任务（Open_printLabelMsg），times 为打印份数
} // IFeiePrintService 接口定义结束
