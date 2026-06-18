package com.ruoyi.system.mapper; // 门店标签打印机 Mapper 接口所在包

import java.util.List; // 引入集合类型，用于返回列表结果
import org.apache.ibatis.annotations.Param; // 引入参数注解，用于多参数方法命名映射
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入门店标签打印机实体

public interface ShopLabelPrinterMapper // 门店标签打印机的 MyBatis 数据访问接口（对应 mapper/business/ShopLabelPrinterMapper.xml）
{
    List<ShopLabelPrinter> selectEnabledByShopId(Long shopId); // 查询某门店下所有启用且未删除的打印机（订单打印时使用）

    List<ShopLabelPrinter> selectShopLabelPrinterList(ShopLabelPrinter printer); // 按条件查询打印机列表（管理后台列表页使用）

    ShopLabelPrinter selectShopLabelPrinterById(Long id); // 按主键查询打印机（不含密钥字段，列表/详情通用）

    ShopLabelPrinter selectShopLabelPrinterByIdWithKey(Long id); // 按主键查询打印机（含密钥字段，修改/删除前需要 SN 与密钥）

    ShopLabelPrinter selectShopLabelPrinterByIdWithKeyForUpdate(Long id); // 删除前加行锁读取，串行化同一设备的并发删除

    ShopLabelPrinter selectActiveBySn(String sn); // 按打印机编号 SN 查询未删除的打印机（用于唯一性校验，limit 1）

    int insertShopLabelPrinter(ShopLabelPrinter printer); // 新增打印机记录，回写自增主键 id

    int updateShopLabelPrinter(ShopLabelPrinter printer); // 按主键更新打印机（含密钥字段；密钥由 Service 在未提交新值时回填旧值，故始终写入有效密钥）

    int updateFeieStatus(@Param("id") Long id, @Param("feieStatus") String feieStatus); // 仅更新飞鹅状态字段及最近查询时间（查询状态后回写）

    int deleteShopLabelPrinterById(Long id); // 逻辑删除：将 del_flag 置为 1
} // ShopLabelPrinterMapper 接口定义结束
