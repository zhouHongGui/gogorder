package com.ruoyi.web.controller.c;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.constant.CAuthConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.dto.CCartAddRequest;
import com.ruoyi.system.domain.dto.CCartUpdateRequest;
import com.ruoyi.system.service.ICCartService;

@RestController
@RequestMapping("/api/c/cart")
public class CCartController
{
    @Autowired
    private ICCartService cartService;

    @GetMapping("/get")
    public AjaxResult get(@RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.getCart(currentUserId(), shopId));
    }

    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody CCartAddRequest request)
    {
        return AjaxResult.success(cartService.addItem(currentUserId(), request));
    }

    @PutMapping("/update")
    public AjaxResult update(@Validated @RequestBody CCartUpdateRequest request)
    {
        return AjaxResult.success(cartService.updateItem(currentUserId(), request));
    }

    @DeleteMapping("/remove/{cartItemId}")
    public AjaxResult remove(@PathVariable String cartItemId, @RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.removeItem(currentUserId(), shopId, cartItemId));
    }

    @DeleteMapping("/clear")
    public AjaxResult clear(@RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.clearCart(currentUserId(), shopId));
    }

    private Long currentUserId()
    {
        Object userId = ServletUtils.getRequest().getAttribute(CAuthConstants.USER_ID_ATTRIBUTE);
        if (userId == null)
        {
            throw new ServiceException("请先登录", HttpStatus.UNAUTHORIZED);
        }
        return Long.valueOf(String.valueOf(userId));
    }
}
