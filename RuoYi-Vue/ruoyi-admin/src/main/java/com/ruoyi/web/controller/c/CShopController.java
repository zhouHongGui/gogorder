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
 * C 端门店查询接口（{@code /api/c/shop/**}，公开路径，无需登录）。
 *
 * <p>{@code @Anonymous} 标记整个控制器为匿名可访问（与 SecurityConfig 的 permitAll 一致），
 * 因为「选门店、看菜单」是下单前的前置流程，未登录用户也应能浏览。
 */
@Anonymous
@RestController
@RequestMapping("/api/c/shop")
public class CShopController
{
    @Autowired
    private ICShopService cShopService;

    /** 高德地图 Web 端 key（前端 JS API 初始化用）。 */
    @Value("${amap.web.key:}")
    private String amapWebKey;

    /** 高德地图安全密钥（新版 JS API 必需）。 */
    @Value("${amap.web.security-code:}")
    private String amapSecurityCode;

    /**
     * 返回高德地图配置（key + securityCode），供前端初始化地图。
     * 设 {@code no-store} 防缓存，避免改配置后前端用旧值。
     */
    @GetMapping("/map-config")
    public AjaxResult mapConfig(HttpServletResponse response)
    {
        response.setHeader("Cache-Control", "no-store");
        return AjaxResult.success(Map.of(
                "key", amapWebKey,
                "securityCode", amapSecurityCode));
    }

    /**
     * 附近门店列表。按经纬度排序，支持关键字筛选。
     */
    @GetMapping("/nearby")
    public AjaxResult nearby(
            @RequestParam(required = false) BigDecimal longitude,
            @RequestParam(required = false) BigDecimal latitude,
            @RequestParam(required = false) String keyword)
    {
        return AjaxResult.success(cShopService.selectNearbyShops(longitude, latitude, keyword));
    }

    /** 门店详情（含营业时间、预约参数等，供菜单页/下单页使用）。 */
    @GetMapping("/{id}")
    public AjaxResult detail(@PathVariable Long id)
    {
        return AjaxResult.success(cShopService.selectShopDetail(id));
    }

    /** 预订单可选取餐时段列表（按门店营业时间 + 预约窗口生成）。 */
    @GetMapping("/{id}/preorder-slots")
    public AjaxResult preorderSlots(@PathVariable Long id)
    {
        return AjaxResult.success(cShopService.selectPreorderSlots(id));
    }
}
