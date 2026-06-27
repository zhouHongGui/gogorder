package com.ruoyi.web.controller.b;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.constant.BAuthConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.core.page.TableDataInfo;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.dto.BShopProductQuery;
import com.ruoyi.system.domain.dto.BShopProductSoldOutRequest;
import com.ruoyi.system.domain.dto.BShopProductStatusRequest;
import com.ruoyi.system.service.IBProductService;

/** 门店员工端商品管理接口。 */
@RestController
@RequestMapping("/api/b/product")
public class BProductController
{
    @Autowired private IBProductService bProductService;

    @GetMapping("/categories")
    public AjaxResult categories(@RequestHeader("X-Shop-Id") Long shopId)
    {
        return AjaxResult.success(bProductService.listCategories(shopId));
    }

    @GetMapping("/list")
    public TableDataInfo list(@RequestHeader("X-Shop-Id") Long shopId, BShopProductQuery query)
    {
        var page = bProductService.listProducts(shopId, query);
        TableDataInfo result = new TableDataInfo(page.getRows(), page.getTotal());
        result.setCode(HttpStatus.SUCCESS);
        result.setMsg("查询成功");
        return result;
    }

    @Log(title = "员工端商品状态", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/status")
    public AjaxResult updateStatus(HttpServletRequest servletRequest,
            @RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id,
            @Validated @RequestBody BShopProductStatusRequest request)
    {
        bProductService.updateStatus(shopId, id, request.getStatus(), currentStaffId(servletRequest));
        return AjaxResult.success();
    }

    @Log(title = "员工端商品售空", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/sold-out")
    public AjaxResult setSoldOut(HttpServletRequest servletRequest,
            @RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id,
            @Validated @RequestBody BShopProductSoldOutRequest request)
    {
        bProductService.setSoldOut(shopId, id, request, currentStaffId(servletRequest));
        return AjaxResult.success();
    }

    private Long currentStaffId(HttpServletRequest request)
    {
        Object value = request.getAttribute(BAuthConstants.STAFF_ID_ATTRIBUTE);
        if (value instanceof Long staffId) return staffId;
        throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
    }
}
