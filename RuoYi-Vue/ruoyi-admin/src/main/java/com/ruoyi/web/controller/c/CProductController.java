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
 * C 端商品菜单接口（{@code /api/c/product/**}，公开路径，无需登录）。
 * 商品数据按门店维度组织（shop_product），含规格、库存、近 30 天销量。
 */
@RestController
@RequestMapping("/api/c/product")
public class CProductController
{
    @Autowired
    private ICProductBrowseService productBrowseService;

    /**
     * 门店商品列表（菜单）。支持按分类、关键字筛选；销量为单条分组查询批量回填，非逐行子查询。
     */
    @GetMapping("/list")
    public AjaxResult list(
            @RequestParam Long shopId,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) String keyword)
    {
        return AjaxResult.success(productBrowseService.selectProducts(shopId, categoryId, keyword));
    }

    /** 商品详情（含完整规格模板与选项，供加购/下单选择规格）。 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id, @RequestParam Long shopId)
    {
        return AjaxResult.success(productBrowseService.selectProductDetail(shopId, id));
    }
}
