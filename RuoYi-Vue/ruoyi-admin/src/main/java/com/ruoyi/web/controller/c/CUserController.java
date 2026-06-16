package com.ruoyi.web.controller.c;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.CAuthConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.dto.CUserUpdateRequest;
import com.ruoyi.system.service.ICAuthService;

/**
 * C 端用户接口（{@code /api/c/user/**}，需登录）。
 *
 * <p>提供用户资料查询/修改与余额查询。所有接口都通过 {@code currentUserId()} 取登录态，
 * 服务层会再次校验账号是否被禁用。
 */
@RestController
@RequestMapping("/api/c/user")
public class CUserController
{
    @Autowired
    private ICAuthService authService;

    /** 获取当前用户信息（昵称/头像/手机号等）。 */
    @GetMapping("/info")
    public AjaxResult info()
    {
        return AjaxResult.success(authService.getUserInfo(currentUserId()));
    }

    /** 获取当前用户余额（无余额账户视为 0）。 */
    @GetMapping("/balance")
    public AjaxResult balance()
    {
        return AjaxResult.success(authService.getUserBalance(currentUserId()));
    }

    /** 修改当前用户资料（昵称/头像）。 */
    @PutMapping("/update")
    public AjaxResult update(@Validated @RequestBody CUserUpdateRequest request)
    {
        return AjaxResult.success(authService.updateUser(currentUserId(), request));
    }

    /**
     * 从 request 属性取当前登录用户 ID（由 {@code CAuthTokenFilter} 注入）。
     *
     * @throws ServiceException 未登录(401)
     */
    private Long currentUserId()
    {
        Object userId = ServletUtils.getRequest().getAttribute(CAuthConstants.USER_ID_ATTRIBUTE);
        if (userId == null)
        {
            throw new ServiceException("请先登录", 401);
        }
        return Long.valueOf(String.valueOf(userId));
    }
}
