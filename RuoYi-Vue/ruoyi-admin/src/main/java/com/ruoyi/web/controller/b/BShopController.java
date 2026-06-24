package com.ruoyi.web.controller.b;

import java.util.List;
import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.constant.BAuthConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.framework.web.service.StaffAuthService;
import com.ruoyi.system.domain.dto.BShopContextView;
import com.ruoyi.system.domain.dto.BShopSwitchRequest;

/** 门店员工端门店上下文接口。 */
@RestController
@RequestMapping("/api/b/shop")
public class BShopController
{
    @Autowired
    private StaffAuthService staffAuthService;

    @GetMapping("/mine")
    public AjaxResult mine(HttpServletRequest request,
            @RequestHeader(value = "X-Shop-Id", required = false) Long shopId)
    {
        Long staffId = currentStaffId(request);
        List<BShopContextView> shops = staffAuthService.getShopContexts(staffId);
        BShopContextView current = shopId == null
                ? shops.stream().filter(item -> Integer.valueOf(1).equals(item.getIsDefault())).findFirst().orElse(shops.get(0))
                : staffAuthService.requireShopContext(staffId, shopId);
        return AjaxResult.success(Map.of("shops", shops, "currentShop", current));
    }

    @PutMapping("/switch")
    public AjaxResult switchShop(HttpServletRequest servletRequest,
            @Validated @RequestBody BShopSwitchRequest request)
    {
        return AjaxResult.success(staffAuthService.switchShopContext(
                currentStaffId(servletRequest), request.getShopId()));
    }

    private Long currentStaffId(HttpServletRequest request)
    {
        Object value = request.getAttribute(BAuthConstants.STAFF_ID_ATTRIBUTE);
        if (value instanceof Long staffId) return staffId;
        throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
    }
}
