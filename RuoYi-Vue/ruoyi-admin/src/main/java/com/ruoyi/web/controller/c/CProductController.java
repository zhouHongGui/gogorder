package com.ruoyi.web.controller.c;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ICProductBrowseService;

/**
 * C端商品菜单接口。
 */
@RestController
@RequestMapping("/api/c/product")
public class CProductController
{
    @Autowired
    private ICProductBrowseService productBrowseService;

    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam Long shopId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword)
    {
        return AjaxResult.success(productBrowseService.selectProducts(shopId, categoryId, keyword));
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id, @RequestParam Long shopId)
    {
        return AjaxResult.success(productBrowseService.selectProductDetail(shopId, id));
    }
}
