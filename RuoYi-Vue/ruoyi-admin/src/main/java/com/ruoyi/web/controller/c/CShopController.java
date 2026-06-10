package com.ruoyi.web.controller.c;

import java.math.BigDecimal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ICShopService;

/**
 * C端门店查询接口。
 */
@RestController
@RequestMapping("/api/c/shop")
public class CShopController
{
    @Autowired
    private ICShopService cShopService;

    @GetMapping("/nearby")
    public AjaxResult nearby(
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) String keyword)
    {
        return AjaxResult.success(cShopService.selectNearbyShops(longitude, latitude, keyword));
    }

    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(cShopService.selectShopDetail(id));
    }

    @GetMapping("/{id}/preorder-slots")
    public AjaxResult preorderSlots(@PathVariable Long id)
    {
        return AjaxResult.success(cShopService.selectPreorderSlots(id));
    }
}
