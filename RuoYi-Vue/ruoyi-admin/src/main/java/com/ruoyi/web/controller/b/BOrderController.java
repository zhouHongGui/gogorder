package com.ruoyi.web.controller.b;

import java.util.Map;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import com.ruoyi.common.annotation.Log;
import com.ruoyi.common.annotation.RateLimiter;
import com.ruoyi.common.constant.BAuthConstants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.enums.BusinessType;
import com.ruoyi.common.enums.LimitType;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.dto.BOrderBatchStartMakeRequest;
import com.ruoyi.system.domain.dto.BOrderCancelRefundRequest;
import com.ruoyi.system.domain.dto.BOrderVerifyRequest;
import com.ruoyi.system.service.IBOrderService;
import com.ruoyi.system.service.IOrderCancelService;

/** 门店员工端订单看板、制作与核销接口。 */
@RestController
@RequestMapping("/api/b/order")
public class BOrderController
{
    @Autowired private IBOrderService bOrderService;
    @Autowired private IOrderCancelService orderCancelService;

    @GetMapping("/board")
    public AjaxResult board(@RequestHeader("X-Shop-Id") Long shopId)
    {
        return AjaxResult.success(bOrderService.getBoard(shopId));
    }

    @GetMapping("/pending-count")
    public AjaxResult pendingCount(@RequestHeader("X-Shop-Id") Long shopId)
    {
        var board = bOrderService.getBoard(shopId);
        return AjaxResult.success(Map.of(
                "preorders", board.getPreordersCount(),
                "pending", board.getPendingCount(),
                "making", board.getMakingCount(),
                "waiting", board.getWaitingCount(),
                "totalActive", board.getTotalActiveCount()));
    }

    @GetMapping("/{id}/detail")
    public AjaxResult detail(@RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id)
    {
        return AjaxResult.success(bOrderService.getDetail(shopId, id));
    }

    @Log(title = "员工端开始制作", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/start-make")
    public AjaxResult startMake(@RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id)
    {
        bOrderService.startMake(shopId, id);
        return AjaxResult.success();
    }

    @Log(title = "员工端批量开始制作", businessType = BusinessType.UPDATE)
    @PutMapping("/batch-start-make")
    public AjaxResult batchStartMake(@RequestHeader("X-Shop-Id") Long shopId,
            @Validated @RequestBody BOrderBatchStartMakeRequest request)
    {
        return AjaxResult.success(Map.of("count", bOrderService.batchStartMake(shopId, request.getOrderIds())));
    }

    @Log(title = "员工端制作完成", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/complete-make")
    public AjaxResult completeMake(@RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id)
    {
        bOrderService.completeMake(shopId, id);
        return AjaxResult.success();
    }

    @RateLimiter(key = "staff:order:verify:", time = 60, count = 10, limitType = LimitType.IP)
    @Log(title = "员工端核销取餐", businessType = BusinessType.UPDATE)
    @PutMapping("/verify")
    public AjaxResult verify(@RequestHeader("X-Shop-Id") Long shopId,
            @Validated @RequestBody BOrderVerifyRequest request)
    {
        return AjaxResult.success(bOrderService.verify(shopId, request.getPickupToken()));
    }

    @Log(title = "员工端取消退款", businessType = BusinessType.UPDATE)
    @PutMapping("/{id}/cancel-refund")
    public AjaxResult cancelRefund(HttpServletRequest servletRequest,
            @RequestHeader("X-Shop-Id") Long shopId, @PathVariable Long id,
            @Validated @RequestBody BOrderCancelRefundRequest request)
    {
        orderCancelService.cancelPaidOrderWithRefund(id, shopId, request.getReason(), currentStaffId(servletRequest));
        return AjaxResult.success();
    }

    private Long currentStaffId(HttpServletRequest request)
    {
        Object value = request.getAttribute(BAuthConstants.STAFF_ID_ATTRIBUTE);
        if (value instanceof Long staffId) return staffId;
        throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
    }
}
