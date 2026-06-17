package com.ruoyi.system.service; // 门店标签打印机服务接口所在包

import java.util.List; // 引入集合类型，用于列表查询返回
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入门店标签打印机实体

public interface IShopLabelPrinterService // 门店标签打印机业务服务接口：管理后台 CRUD 与飞鹅联动
{
    List<ShopLabelPrinter> selectShopLabelPrinterList(ShopLabelPrinter printer); // 分页/条件查询打印机列表

    ShopLabelPrinter selectShopLabelPrinterById(Long id); // 按主键查询打印机详情（不含密钥）

    int insertShopLabelPrinter(ShopLabelPrinter printer); // 新增打印机：校验后同步绑定到飞鹅平台并落库

    int updateShopLabelPrinter(ShopLabelPrinter printer); // 修改打印机：按需同步飞鹅（换 SN/密钥则重新绑定，仅改名则编辑备注）并落库

    int deleteShopLabelPrinterById(Long id); // 逻辑删除打印机：先在飞鹅平台解绑，再置 del_flag=1

    String queryPrinterStatus(Long id); // 查询指定打印机的飞鹅在线状态，并回写 feie_status

    String printTestLabel(Long id); // 向指定打印机发送一张测试标签，用于验证接线和方向
} // IShopLabelPrinterService 接口定义结束
