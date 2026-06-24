package com.ruoyi.web.controller.admin;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.Shop;
import com.ruoyi.system.domain.StaffShop;
import com.ruoyi.system.domain.dto.ShopStatusRequest;
import com.ruoyi.system.domain.dto.StaffShopRequest;
import com.ruoyi.system.domain.dto.StaffShopUpdateRequest;
import com.ruoyi.system.service.IShopService;
import com.ruoyi.system.service.IStaffShopService;

/**
 * 管理后台门店管理接口（{@code /api/admin/shop/**}，需管理端登录 + 权限）。
 *
 * <h3>管理内容</h3>
 * <ul>
 *   <li><b>门店 CRUD</b>：营业时间（支持跨午夜）、预约参数、地理位置等。门店为逻辑删除，保留历史订单引用。</li>
 *   <li><b>门店状态</b>：{@code /status} 切换营业/休息/暂停（影响是否可下即时单）。</li>
 *   <li><b>门店员工关联</b> {@code /staff/**}：{@code staff_shop} 多对多，店员默认一家、店长可关联多家。</li>
 *   <li><b>地图配置</b> {@code /map-config}：返回高德 key/安全密钥供后台选址地图使用。</li>
 * </ul>
 *
 * <h3>约定</h3>
 * 同 {@link ProductCenterController}：每个接口 {@code @PreAuthorize} 权限码校验，
 * 写操作加 {@code @Log} 记录操作日志，列表分页走 {@link BaseController}。业务逻辑在
 * {@link IShopService} / {@link IStaffShopService}。
 */
@RestController
@RequestMapping("/api/admin/shop")
public class ShopController extends BaseController
{
    @Autowired
    private IShopService shopService;

    @Autowired
    private IStaffShopService staffShopService;

    @Value("${amap.web.key:}")
    private String amapWebKey;

    @Value("${amap.web.security-code:}")
    private String amapSecurityCode;

    @PreAuthorize("@ss.hasAnyPermi('admin:shop:add,admin:shop:edit')")
    @GetMapping("/map-config")
    public AjaxResult mapConfig(HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-store");
        return success(Map.of(
                "key", amapWebKey,
                "securityCode", amapSecurityCode));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:list')")
    @GetMapping("/list")
    public TableDataInfo list(Shop shop)
    {
        startPage();
        List<Shop> list = shopService.selectShopList(shop);
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(shopService.selectShopById(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:add')")
    @Log(title = "门店管理", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody Shop shop)
    {
        return toAjax(shopService.insertShop(shop));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:edit')")
    @Log(title = "门店管理", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody Shop shop)
    {
        shop.setId(id);
        return toAjax(shopService.updateShop(shop));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:status')")
    @Log(title = "门店状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult changeStatus(@PathVariable Long id, @Validated @RequestBody ShopStatusRequest request)
    {
        return toAjax(shopService.updateShopStatus(id, request.getStatus()));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:remove')")
    @Log(title = "门店管理", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id)
    {
        return toAjax(shopService.deleteShopById(id));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @GetMapping("/{id}/staff")
    public AjaxResult listStaff(@PathVariable Long id)
    {
        List<StaffShop> list = staffShopService.selectStaffByShopId(id);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工关联", businessType = BusinessType.INSERT)
    @PostMapping("/{id}/staff")
    public AjaxResult addStaff(@PathVariable Long id, @Validated @RequestBody StaffShopRequest request)
    {
        int rows = staffShopService.insertStaffShop(id, request);
        return toAjax(rows);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工关联", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/staff/{staffId}")
    public AjaxResult updateStaff(@PathVariable Long id, @PathVariable Long staffId,
            @Validated @RequestBody StaffShopUpdateRequest request)
    {
        return toAjax(staffShopService.updateStaffShop(id, staffId, request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}/staff/{staffId}")
    public AjaxResult removeStaff(@PathVariable Long id, @PathVariable Long staffId)
    {
        return toAjax(staffShopService.deleteStaffShop(id, staffId));
    }
}
