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

/**
 * C 端购物车接口（{@code /api/c/cart/**}，需登录）。
 *
 * <p>购物车存于 Redis（每门店一车，Key {@code cart:{userId}:{shopId}}），所有操作必须带 shopId。
 * 并发安全由 Lua 脚本保证（见 {@link com.ruoyi.system.service.impl.CCartServiceImpl}）。
 */
@RestController
@RequestMapping("/api/c/cart")
public class CCartController
{
    @Autowired
    private ICCartService cartService;

    /** 查询指定门店的购物车（非空购物车会续期 TTL）。 */
    @GetMapping("/get")
    public AjaxResult get(@RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.getCart(currentUserId(), shopId));
    }

    /** 加入购物车（同商品同规格自动合并数量）。 */
    @PostMapping("/add")
    public AjaxResult add(@Validated @RequestBody CCartAddRequest request)
    {
        return AjaxResult.success(cartService.addItem(currentUserId(), request));
    }

    /** 修改购物车条目数量（quantity=0 表示删除）。 */
    @PutMapping("/update")
    public AjaxResult update(@Validated @RequestBody CCartUpdateRequest request)
    {
        return AjaxResult.success(cartService.updateItem(currentUserId(), request));
    }

    /** 删除购物车条目。 */
    @DeleteMapping("/remove/{cartItemId}")
    public AjaxResult remove(@PathVariable String cartItemId, @RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.removeItem(currentUserId(), shopId, cartItemId));
    }

    /** 清空指定门店的购物车。 */
    @DeleteMapping("/clear")
    public AjaxResult clear(@RequestParam Long shopId)
    {
        return AjaxResult.success(cartService.clearCart(currentUserId(), shopId));
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
            throw new ServiceException("请先登录", HttpStatus.UNAUTHORIZED);
        }
        return Long.valueOf(String.valueOf(userId));
    }
}
