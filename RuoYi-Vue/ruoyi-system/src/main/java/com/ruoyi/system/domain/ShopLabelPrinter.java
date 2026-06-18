package com.ruoyi.system.domain; // 门店标签打印机实体所在包

import java.time.LocalDateTime; // 引入日期时间类型，用于记录最后状态查询时间
import com.fasterxml.jackson.annotation.JsonProperty;
import com.ruoyi.common.core.domain.BaseEntity; // 引入若依基础实体，提供 remark/createTime/updateTime/createBy/updateBy 等公共字段

public class ShopLabelPrinter extends BaseEntity // 门店标签打印机的领域实体，对应表 shop_label_printer
{
    private static final long serialVersionUID = 1L; // 序列化版本号，保证实体序列化兼容性

    private Long id; // 主键 ID
    private Long shopId; // 绑定的门店 ID
    private String shopName; // 门店名称（联表 shop 查询得到，非持久化字段，用于列表展示）
    private String sn; // 飞鹅打印机编号 SN（设备唯一标识，6-32 位数字）
    @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
    private String printerKey; // 飞鹅打印机设备密钥（仅允许请求写入，任何响应均不序列化）
    private String printerName; // 设备备注名（如"南宁店标签机"），可空
    private Integer printWidth; // 标签纸宽度，单位毫米（mm），范围 20-120
    private Integer printHeight; // 标签纸高度，单位毫米（mm），范围 10-120
    private Integer status; // 启用状态：0 停用、1 启用
    private String feieStatus; // 飞鹅平台最近一次返回的状态描述（如"在线、正常工作"）
    private LocalDateTime lastStatusTime; // 最近一次查询飞鹅状态的时间
    private Integer delFlag; // 逻辑删除标志：0 正常、1 已删除

    public Long getId() { return id; } // 获取主键
    public void setId(Long id) { this.id = id; } // 设置主键
    public Long getShopId() { return shopId; } // 获取绑定门店 ID
    public void setShopId(Long shopId) { this.shopId = shopId; } // 设置绑定门店 ID
    public String getShopName() { return shopName; } // 获取门店名称
    public void setShopName(String shopName) { this.shopName = shopName; } // 设置门店名称
    public String getSn() { return sn; } // 获取打印机编号 SN
    public void setSn(String sn) { this.sn = sn; } // 设置打印机编号 SN
    public String getPrinterKey() { return printerKey; } // 获取设备密钥
    public void setPrinterKey(String printerKey) { this.printerKey = printerKey; } // 设置设备密钥
    public String getPrinterName() { return printerName; } // 获取设备备注名
    public void setPrinterName(String printerName) { this.printerName = printerName; } // 设置设备备注名
    public Integer getPrintWidth() { return printWidth; } // 获取标签宽度
    public void setPrintWidth(Integer printWidth) { this.printWidth = printWidth; } // 设置标签宽度
    public Integer getPrintHeight() { return printHeight; } // 获取标签高度
    public void setPrintHeight(Integer printHeight) { this.printHeight = printHeight; } // 设置标签高度
    public Integer getStatus() { return status; } // 获取启用状态
    public void setStatus(Integer status) { this.status = status; } // 设置启用状态
    public String getFeieStatus() { return feieStatus; } // 获取飞鹅状态描述
    public void setFeieStatus(String feieStatus) { this.feieStatus = feieStatus; } // 设置飞鹅状态描述
    public LocalDateTime getLastStatusTime() { return lastStatusTime; } // 获取最近状态查询时间
    public void setLastStatusTime(LocalDateTime lastStatusTime) { this.lastStatusTime = lastStatusTime; } // 设置最近状态查询时间
    public Integer getDelFlag() { return delFlag; } // 获取逻辑删除标志
    public void setDelFlag(Integer delFlag) { this.delFlag = delFlag; } // 设置逻辑删除标志
} // ShopLabelPrinter 类定义结束
