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

@RestController
@RequestMapping("/api/c/user")
public class CUserController
{
    @Autowired
    private ICAuthService authService;

    @GetMapping("/info")
    public AjaxResult info()
    {
        return AjaxResult.success(authService.getUserInfo(currentUserId()));
    }

    @GetMapping("/balance")
    public AjaxResult balance()
    {
        return AjaxResult.success(authService.getUserBalance(currentUserId()));
    }

    @PutMapping("/update")
    public AjaxResult update(@Validated @RequestBody CUserUpdateRequest request)
    {
        return AjaxResult.success(authService.updateUser(currentUserId(), request));
    }

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
