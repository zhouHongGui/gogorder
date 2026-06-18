package com.ruoyi.system.domain.dto;

/**
 * 飞鹅打印机绑定结果。
 *
 * @param response 飞鹅接口响应
 * @param newlyBound 本次调用是否在飞鹅平台新建了绑定；已存在时仅更新备注，值为 false
 */
public record FeiePrinterBindResult(FeieResponse response, boolean newlyBound)
{
}
