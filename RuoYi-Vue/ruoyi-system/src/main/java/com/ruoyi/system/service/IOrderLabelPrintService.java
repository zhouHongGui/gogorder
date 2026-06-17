package com.ruoyi.system.service; // 订单标签打印服务接口所在包

public interface IOrderLabelPrintService // 订单标签打印服务接口：支付成功后为门店标签打印机推送标签
{
    void printPaidOrderAsync(Long orderId); // 异步打印指定订单的标签（不阻塞支付主流程，内部保证同一订单只打印一次）
} // IOrderLabelPrintService 接口定义结束
