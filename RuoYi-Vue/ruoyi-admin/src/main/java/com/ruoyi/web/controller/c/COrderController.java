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

/**
 * C 端订单接口（{@code /api/c/order/**}，需登录）。
 *
 * <p>下单/支付是交易核心入口，加用户级 {@link RateLimiter} 防刷：
 * <ul>
 *   <li>下单：60 秒 5 次（防恶意下单占库存）；</li>
 *   <li>支付：60 秒 10 次（防对余额行/取餐号行高频加锁拖累他人）。</li>
 * </ul>
 * 业务层另有下单幂等(submitToken)、支付幂等(订单行锁+流水)兜底，限流是第一道防线。
 */
@RestController
@RequestMapping("/api/c/order")
public class COrderController
{
    @Autowired private IOrderService orderService;
    @Autowired private IBalanceService balanceService;

    /**
     * 提交订单。用户限流 60 秒 5 次。幂等：相同 submitToken 返回同一订单。
     *
     * @param request 下单请求（含门店、订单类型、商品明细、预约时间、提交令牌）
     */
    @PostMapping("/submit")
    @RateLimiter(key = "c:order:submit:", time = 60, count = 5, limitType = LimitType.USER)
    public AjaxResult submit(@Validated @RequestBody OrderSubmitRequest request)
    {
        return AjaxResult.success(orderService.submitOrder(currentUserId(), request));
    }

    /**
     * 支付订单（余额支付）。用户限流 60 秒 10 次。幂等：已支付的订单重复调用返回原结果。
     *
     * @param orderId 待支付订单
     */
    @PostMapping("/pay/{orderId}")
    @RateLimiter(key = "c:order:pay:", time = 60, count = 10, limitType = LimitType.USER)
    public AjaxResult pay(@PathVariable Long orderId)
    {
        return AjaxResult.success("支付成功", balanceService.pay(currentUserId(), orderId));
    }

    /**
     * 取消订单（仅未支付订单可取消，已支付需走管理端退款）。
     */
    @PutMapping("/cancel/{orderId}")
    public AjaxResult cancel(@PathVariable Long orderId)
    {
        orderService.cancelOrder(currentUserId(), orderId);
        return AjaxResult.success("取消成功");
    }

    /**
     * 分页查询订单列表。
     *
     * @param dateScope TODAY=今日 / HISTORY=历史
     * @param pageNum   页码（默认 1）
     * @param pageSize  每页条数（默认 10）
     */
    @GetMapping("/list")
    public AjaxResult list(@RequestParam(defaultValue = "TODAY") String dateScope,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize)
    {
        return AjaxResult.success(orderService.getOrderList(currentUserId(), dateScope, pageNum, pageSize));
    }

    /**
     * 查询订单详情（越权访问返回「订单不存在」）。
     */
    @GetMapping("/{orderId}")
    public AjaxResult detail(@PathVariable Long orderId)
    {
        return AjaxResult.success(orderService.getOrderDetail(currentUserId(), orderId));
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
