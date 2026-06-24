package com.ruoyi.web.controller.admin;

import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.BShopContextView;
import com.ruoyi.system.domain.dto.ShopStaffCreateRequest;
import com.ruoyi.system.domain.dto.ShopStaffUpdateRequest;
import com.ruoyi.system.service.IStaffShopService;
import com.ruoyi.system.service.IShopStaffService;

/** 管理后台独立门店员工账号 CRUD。 */
@RestController
@RequestMapping("/api/admin/staff")
public class ShopStaffController extends BaseController
{
    @Autowired private IShopStaffService shopStaffService;
    @Autowired private IStaffShopService staffShopService;

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @GetMapping("/list")
    public TableDataInfo list(ShopStaff condition)
    {
        startPage();
        List<ShopStaff> list = shopStaffService.selectList(condition);
        // 不下发密码哈希（敏感凭证，仅后端比对用）
        list.forEach(staff -> staff.setPassword(null));
        return getDataTable(list);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        ShopStaff staff = shopStaffService.selectById(id);
        // 不下发密码哈希（敏感凭证，仅后端比对用）
        staff.setPassword(null);
        return success(staff);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @GetMapping("/{id}/shops")
    public AjaxResult shops(@PathVariable Long id)
    {
        List<BShopContextView> list = staffShopService.selectShopsByStaffId(id);
        return success(list);
    }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工", businessType = BusinessType.INSERT)
    @PostMapping
    public AjaxResult add(@Validated @RequestBody ShopStaffCreateRequest request)
    { return toAjax(shopStaffService.insert(request)); }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}")
    public AjaxResult edit(@PathVariable Long id, @Validated @RequestBody ShopStaffUpdateRequest request)
    { return toAjax(shopStaffService.update(id, request)); }

    @PreAuthorize("@ss.hasPermi('admin:shop:staff')")
    @Log(title = "门店员工", businessType = BusinessType.DELETE)
    @DeleteMapping("/{id}")
    public AjaxResult remove(@PathVariable Long id) { return toAjax(shopStaffService.delete(id)); }
}
