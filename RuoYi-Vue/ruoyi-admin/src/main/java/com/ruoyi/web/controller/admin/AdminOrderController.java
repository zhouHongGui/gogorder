package com.ruoyi.web.controller.admin;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.controller.BaseController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.system.domain.dto.AdminOrderQuery;
import com.ruoyi.system.service.IAdminOrderService;

@RestController
@RequestMapping("/api/admin/order")
public class AdminOrderController extends BaseController
{
    @Autowired
    private IAdminOrderService adminOrderService;

    @PreAuthorize("@ss.hasPermi('admin:order:list')")
    @GetMapping("/list")
    public TableDataInfo list(AdminOrderQuery query)
    {
        startPage();
        return getDataTable(adminOrderService.selectOrderList(query));
    }

    @PreAuthorize("@ss.hasPermi('admin:order:query')")
    @GetMapping("/{id}")
    public AjaxResult getInfo(@PathVariable Long id)
    {
        return success(adminOrderService.selectOrderDetail(id));
    }
}
