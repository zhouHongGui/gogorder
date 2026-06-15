package com.ruoyi.web.controller.c;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.constant.CAuthConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.ServletUtils;
import com.ruoyi.system.domain.dto.OrderSubmitRequest;
import com.ruoyi.system.service.IBalanceService;
import com.ruoyi.system.service.IOrderService;

@RestController
@RequestMapping("/api/c/order")
public class COrderController
{
    @Autowired private IOrderService orderService;
    @Autowired private IBalanceService balanceService;

    @PostMapping("/submit")
    @RateLimiter(key = "c:order:submit:", time = 60, count = 5, limitType = LimitType.USER)
    public AjaxResult submit(@Validated @RequestBody OrderSubmitRequest request)
    {
        return AjaxResult.success(orderService.submitOrder(currentUserId(), request));
    }

    @PostMapping("/pay/{orderId}")
    @RateLimiter(key = "c:order:pay:", time = 60, count = 10, limitType = LimitType.USER)
    public AjaxResult pay(@PathVariable Long orderId)
    {
        return AjaxResult.success("支付成功", balanceService.pay(currentUserId(), orderId));
    }

    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancel(@PathVariable Long orderId)
    {
        orderService.cancelOrder(currentUserId(), orderId);
        return AjaxResult.success("取消成功");
    }

    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "TODAY") String dateScope,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return AjaxResult.success(orderService.getOrderList(currentUserId(), dateScope, pageNum, pageSize));
    }

    @GetMapping("/{orderId}")
    public AjaxResult detail(@PathVariable Long orderId)
    {
        return AjaxResult.success(orderService.getOrderDetail(currentUserId(), orderId));
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
