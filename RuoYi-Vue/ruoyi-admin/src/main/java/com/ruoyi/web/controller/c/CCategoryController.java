package com.ruoyi.web.controller.c;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ICProductBrowseService;

/**
 * C 端商品分类接口（{@code /api/c/category/**}，公开路径，无需登录）。
 * 用于菜单页顶部分类筛选。
 */
@RestController
@RequestMapping("/api/c/category")
public class CCategoryController
{
    @Autowired
    private ICProductBrowseService productBrowseService;

    /** 查询门店下的商品分类列表（按门店在售商品关联的分类）。 */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam Long shopId)
    {
        return AjaxResult.success(productBrowseService.selectCategories(shopId));
    }
}
