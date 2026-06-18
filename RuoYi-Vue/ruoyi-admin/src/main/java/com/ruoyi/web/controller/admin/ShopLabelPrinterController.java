package com.ruoyi.web.controller.admin; // 标签打印机管理后台控制器所在包

import java.util.List; // 引入集合类型，用于列表返回
import org.springframework.beans.factory.annotation.Autowired; // 引入自动注入注解
import org.springframework.security.access.prepost.PreAuthorize; // 引入权限注解，做细粒度按钮级鉴权
import org.springframework.validation.annotation.Validated; // 引入校验注解
import org.springframework.web.bind.annotation.DeleteMapping; // 引入 DELETE 映射
import org.springframework.web.bind.annotation.GetMapping; // 引入 GET 映射
import org.springframework.web.bind.annotation.PathVariable; // 引入路径变量
import org.springframework.web.bind.annotation.PostMapping; // 引入 POST 映射
import org.springframework.web.bind.annotation.PutMapping; // 引入 PUT 映射
import org.springframework.web.bind.annotation.RequestBody; // 引入请求体
import org.springframework.web.bind.annotation.RequestMapping; // 引入请求映射
import org.springframework.web.bind.annotation.RestController; // 引入 REST 控制器注解
import com.ruoyi.common.annotation.Log; // 引入操作日志注解，记录关键操作到 sys_oper_log
import com.ruoyi.common.core.controller.BaseController; // 引入若依基础控制器，提供分页和统一返回
import com.ruoyi.common.core.domain.AjaxResult; // 引入统一响应对象
import com.ruoyi.common.core.page.TableDataInfo; // 引入分页响应对象
import com.ruoyi.common.enums.BusinessType; // 引入业务操作类型枚举
import com.ruoyi.system.domain.ShopLabelPrinter; // 引入打印机实体
import com.ruoyi.system.service.IShopLabelPrinterService; // 引入打印机服务

@RestController // 声明为 REST 控制器，返回 JSON
@RequestMapping("/api/admin/shop-label-printer") // 管理后台打印机接口前缀
public class ShopLabelPrinterController extends BaseController // 标签打印机管理后台控制器：CRUD + 状态查询 + 测试打印
{
    @Autowired
    private IShopLabelPrinterService shopLabelPrinterService; // 注入打印机业务服务

    @PreAuthorize("@ss.hasPermi('admin:label-printer:list')") // 校验列表权限
    @GetMapping("/list") // GET /list 分页查询
    public TableDataInfo list(ShopLabelPrinter printer) // 列表查询接口
    {
        startPage(); // 启用分页（从请求参数读取 pageNum/pageSize）
        List<ShopLabelPrinter> list = shopLabelPrinterService.selectShopLabelPrinterList(printer); // 查询列表
        return getDataTable(list); // 包装为若依分页响应
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:query')") // 校验查询权限
    @GetMapping("/{id}") // GET /{id} 查询详情
    public AjaxResult getInfo(@PathVariable Long id) // 详情查询接口
    {
        return success(shopLabelPrinterService.selectShopLabelPrinterById(id)); // 返回详情（不含密钥）
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:add')") // 校验新增权限
    @Log(title = "标签打印机", businessType = BusinessType.INSERT) // 记录新增操作日志
    @PostMapping // POST 新增
    public AjaxResult add(@Validated @RequestBody ShopLabelPrinter printer) // 新增接口
    {
        return toAjax(shopLabelPrinterService.insertShopLabelPrinter(printer)); // 新增并返回成功/失败
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:edit')") // 校验修改权限
    @Log(title = "标签打印机", businessType = BusinessType.UPDATE) // 记录修改操作日志
    @PutMapping("/{id}") // PUT /{id} 修改
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody ShopLabelPrinter printer) // 修改接口
    {
        printer.setId(id); // 用路径中的 id 覆盖请求体，防止越权改其他记录
        return toAjax(shopLabelPrinterService.updateShopLabelPrinter(printer)); // 修改并返回结果
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:remove')") // 校验删除权限
    @Log(title = "标签打印机", businessType = BusinessType.DELETE) // 记录删除操作日志
    @DeleteMapping("/{id}") // DELETE /{id} 删除
    public AjaxResult remove(@PathVariable Long id) // 删除接口
    {
        return toAjax(shopLabelPrinterService.deleteShopLabelPrinterById(id)); // 逻辑删除并返回结果
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:status')") // 校验状态查询权限
    @GetMapping("/{id}/status") // GET /{id}/status 查询飞鹅状态
    public AjaxResult queryStatus(@PathVariable Long id) // 状态查询接口
    {
        return success(shopLabelPrinterService.queryPrinterStatus(id)); // 返回飞鹅状态文本
    }

    @PreAuthorize("@ss.hasPermi('admin:label-printer:test')") // 校验测试打印权限
    @Log(title = "标签打印测试", businessType = BusinessType.OTHER) // 记录测试打印操作日志
    @PostMapping("/{id}/test") // POST /{id}/test 测试打印
    public AjaxResult testPrint(@PathVariable Long id) // 测试打印接口
    {
        return success(shopLabelPrinterService.printTestLabel(id)); // 发送测试标签并返回结果
    }
} // ShopLabelPrinterController 类定义结束
