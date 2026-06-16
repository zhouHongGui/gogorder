package com.ruoyi.web.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.Category;
import com.ruoyi.system.domain.Product;
import com.ruoyi.system.domain.SpecOption;
import com.ruoyi.system.domain.SpecTemplate;
import com.ruoyi.system.domain.dto.ShopProductAssignRequest;
import com.ruoyi.system.domain.dto.ShopProductBatchRemoveRequest;
import com.ruoyi.system.domain.dto.ShopProductUpdateRequest;
import com.ruoyi.system.domain.dto.StockAdjustRequest;
import com.ruoyi.system.service.IProductCenterService;

/**
 * 管理后台商品中心接口（{@code /api/admin/**}，需管理端登录 + 权限）。
 *
 * <h3>管理四类对象</h3>
 * <ul>
 *   <li><b>分类</b> {@code /category/**}：商品分类 CRUD。</li>
 *   <li><b>规格模板/选项</b> {@code /spec-template/**}、{@code /spec-option/**}：
 *       规格定义，optionId 全局唯一不可变；选项支持禁用（保留历史引用）。</li>
 *   <li><b>商品库</b> {@code /product/**}：商品主数据 CRUD（含多分类、多规格模板关联）。</li>
 *   <li><b>门店商品</b> {@code /shop-product/**}：商品分配到门店、改售价/状态、库存调整、批量下架。</li>
 * </ul>
 *
 * <h3>约定（接手必读）</h3>
 * <ul>
 *   <li><b>权限</b>：每个接口 {@code @PreAuthorize("@ss.hasPermi('admin:xxx:yyy')")}，
 *       权限码对应 {@code sys_menu}，由角色分配。新增接口需同步在菜单/角色配置权限码。</li>
 *   <li><b>操作日志</b>：写操作加 {@code @Log}，自动记录到 {@code sys_oper_log} 便于审计。</li>
 *   <li><b>分页</b>：列表接口继承 {@link BaseController}，{@code startPage()} + {@code getDataTable()}
 *       走若依 PageHelper，前端用 {@code rows/total}。</li>
 *   <li>业务规则与校验全部在 {@link IProductCenterService}，控制器只做转发。</li>
 * </ul>
 */
@RestController
@RequestMapping("/api/admin")
public class ProductCenterController extends BaseController
{
    @Autowired
    private IProductCenterService productCenterService;

    @PreAuthorize("@ss.hasPermi('admin:category:list')")
    @GetMapping("/category/list")
    public AjaxResult categoryList(Category category)
    {
        return success(productCenterService.selectCategoryList(category));
    }

    @PreAuthorize("@ss.hasPermi('admin:category:add')")
    @Log(title = "商品分类", businessType = BusinessType.INSERT)
    @PostMapping("/category")
    public AjaxResult addCategory(@Validated @RequestBody Category category)
    {
        return toAjax(productCenterService.insertCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('admin:category:edit')")
    @Log(title = "商品分类", businessType = BusinessType.UPDATE)
    @PutMapping("/category/{id}")
    public AjaxResult editCategory(@PathVariable Long id, @Validated @RequestBody Category category)
    {
        category.setId(id);
        return toAjax(productCenterService.updateCategory(category));
    }

    @PreAuthorize("@ss.hasPermi('admin:category:remove')")
    @Log(title = "商品分类", businessType = BusinessType.DELETE)
    @DeleteMapping("/category/{id}")
    public AjaxResult removeCategory(@PathVariable Long id)
    {
        return toAjax(productCenterService.deleteCategory(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:list')")
    @GetMapping("/spec-template/list")
    public AjaxResult specTemplateList(SpecTemplate template)
    {
        return success(productCenterService.selectSpecTemplateList(template));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:add')")
    @Log(title = "规格模板", businessType = BusinessType.INSERT)
    @PostMapping("/spec-template")
    public AjaxResult addSpecTemplate(@Validated @RequestBody SpecTemplate template)
    {
        return toAjax(productCenterService.insertSpecTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:edit')")
    @Log(title = "规格模板", businessType = BusinessType.UPDATE)
    @PutMapping("/spec-template/{id}")
    public AjaxResult editSpecTemplate(@PathVariable Long id, @Validated @RequestBody SpecTemplate template)
    {
        template.setId(id);
        return toAjax(productCenterService.updateSpecTemplate(template));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:remove')")
    @Log(title = "规格模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/spec-template/{id}")
    public AjaxResult removeSpecTemplate(@PathVariable Long id)
    {
        return toAjax(productCenterService.deleteSpecTemplate(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:list')")
    @GetMapping("/spec-option/list")
    public AjaxResult specOptionList(@RequestParam Long templateId)
    {
        return success(productCenterService.selectSpecOptionList(templateId));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:add')")
    @Log(title = "规格选项", businessType = BusinessType.INSERT)
    @PostMapping("/spec-option")
    public AjaxResult addSpecOption(@Validated @RequestBody SpecOption option)
    {
        return toAjax(productCenterService.insertSpecOption(option));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:edit')")
    @Log(title = "规格选项", businessType = BusinessType.UPDATE)
    @PutMapping("/spec-option/{id}")
    public AjaxResult editSpecOption(@PathVariable Long id, @Validated @RequestBody SpecOption option)
    {
        option.setId(id);
        return toAjax(productCenterService.updateSpecOption(option));
    }

    @PreAuthorize("@ss.hasPermi('admin:spec:edit')")
    @Log(title = "规格选项", businessType = BusinessType.UPDATE)
    @PutMapping("/spec-option/{id}/disable")
    public AjaxResult disableSpecOption(@PathVariable Long id)
    {
        return toAjax(productCenterService.disableSpecOption(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:product:list')")
    @GetMapping("/product/list")
    public TableDataInfo productList(Product product)
    {
        startPage();
        return getDataTable(productCenterService.selectProductList(product));
    }

    @PreAuthorize("@ss.hasPermi('admin:product:query')")
    @GetMapping("/product/{id}")
    public AjaxResult productInfo(@PathVariable Long id)
    {
        return success(productCenterService.selectProductById(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:product:add')")
    @Log(title = "商品库", businessType = BusinessType.INSERT)
    @PostMapping("/product")
    public AjaxResult addProduct(@Validated @RequestBody Product product)
    {
        return toAjax(productCenterService.insertProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('admin:product:edit')")
    @Log(title = "商品库", businessType = BusinessType.UPDATE)
    @PutMapping("/product/{id}")
    public AjaxResult editProduct(@PathVariable Long id, @Validated @RequestBody Product product)
    {
        product.setId(id);
        return toAjax(productCenterService.updateProduct(product));
    }

    @PreAuthorize("@ss.hasPermi('admin:product:remove')")
    @Log(title = "商品库", businessType = BusinessType.DELETE)
    @DeleteMapping("/product/{id}")
    public AjaxResult removeProduct(@PathVariable Long id)
    {
        return toAjax(productCenterService.deleteProduct(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop-product:list')")
    @GetMapping("/shop-product/list/{shopId}")
    public TableDataInfo shopProductList(@PathVariable Long shopId)
    {
        startPage();
        return getDataTable(productCenterService.selectShopProductList(shopId));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop-product:assign')")
    @Log(title = "门店商品分配", businessType = BusinessType.INSERT)
    @PostMapping("/shop-product/assign")
    public AjaxResult assignShopProduct(@Validated @RequestBody ShopProductAssignRequest request)
    {
        return success(productCenterService.assignShopProducts(request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop-product:edit')")
    @Log(title = "门店商品", businessType = BusinessType.UPDATE)
    @PutMapping("/shop-product/{id}")
    public AjaxResult editShopProduct(@PathVariable Long id, @Validated @RequestBody ShopProductUpdateRequest request)
    {
        return toAjax(productCenterService.updateShopProduct(id, request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop-product:stock')")
    @Log(title = "门店商品库存", businessType = BusinessType.UPDATE)
    @PostMapping("/shop-product/{id}/stock-adjust")
    public AjaxResult adjustStock(@PathVariable Long id, @Validated @RequestBody StockAdjustRequest request)
    {
        return success(productCenterService.adjustStock(id, request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop-product:remove')")
    @Log(title = "门店商品", businessType = BusinessType.DELETE)
    @DeleteMapping("/shop-product/batch-remove")
    public AjaxResult removeShopProducts(@Validated @RequestBody ShopProductBatchRemoveRequest request)
    {
        return toAjax(productCenterService.deleteShopProducts(request.getIds()));
    }
}
