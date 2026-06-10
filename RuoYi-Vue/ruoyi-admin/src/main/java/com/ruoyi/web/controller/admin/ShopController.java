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
 * 管理后台门店管理
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
        return toAjax(staffShopService.insertStaffShop(id, request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工关联", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/staff/{userId}")
    public AjaxResult updateStaff(@PathVariable Long id, @PathVariable Long userId,
            @Validated @RequestBody StaffShopUpdateRequest request)
    {
        return toAjax(staffShopService.updateStaffShop(id, userId, request));
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工关联", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}/staff/{userId}")
    public AjaxResult removeStaff(@PathVariable Long id, @PathVariable Long userId)
    {
        return toAjax(staffShopService.deleteStaffShop(id, userId));
    }
}
