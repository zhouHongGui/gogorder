package com.ruoyi.web.controller.c;

import java.math.BigDecimal;
import java.util.Map;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Anonymous;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.system.service.ICShopService;

/**
 * C端门店查询接口。
 */
@Anonymous
@RestController
@RequestMapping("/api/c/shop")
public class CShopController
{
    @Autowired
    private ICShopService cShopService;

    @Value("${amap.web.key:}")
    private String amapWebKey;

    @Value("${amap.web.security-code:}")
    private String amapSecurityCode;

    @GetMapping("/map-config")
    public AjaxResult mapConfig(HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-store");
        return AjaxResult.success(Map.of(
                "key", amapWebKey,
                "securityCode", amapSecurityCode));
    }

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
