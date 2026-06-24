package com.ruoyi.framework.interceptor;

import java.io.IOException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.BAuthConstants;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.service.IStaffShopService;

/**
 * B 端门店越权拦截器：对 {@code /api/b/**} 统一校验「当前员工 + X-Shop-Id ∈ staff_shop」。
 *
 * <p>登录接口 {@code /api/b/auth/**} 在注册时 exclude，不经过本拦截器（登录前尚无门店上下文）。
 * 所有员工均必须有明确门店绑定，不存在若依超管绕过。
 *
 * <p>异常统一手写 JSON 响应，不依赖全局 ExceptionHandler（拦截器阶段的异常处理路径在不同容器下行为不一致）。
 */
@Component
public class BShopInterceptor implements HandlerInterceptor
{
    @Autowired
    private IStaffShopService staffShopService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException
    {
        // CORS 预检不携带业务门店头，交给 CorsFilter 正常处理。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            return true;
        }
        Object staffIdValue = request.getAttribute(BAuthConstants.STAFF_ID_ATTRIBUTE);
        if (!(staffIdValue instanceof Long staffId))
        {
            writeJson(response, HttpServletResponse.SC_UNAUTHORIZED,
                    AjaxResult.error(HttpServletResponse.SC_UNAUTHORIZED, "未登录或登录已失效"));
            return false;
        }
        String shopIdStr = request.getHeader("X-Shop-Id");
        if (shopIdStr == null || shopIdStr.isBlank())
        {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, "缺少门店标识 X-Shop-Id"));
            return false;
        }
        final Long shopId;
        try
        {
            shopId = Long.parseLong(shopIdStr.trim());
        }
        catch (NumberFormatException ex)
        {
            writeJson(response, HttpServletResponse.SC_BAD_REQUEST,
                    AjaxResult.error(HttpServletResponse.SC_BAD_REQUEST, "门店标识格式错误"));
            return false;
        }
        try
        {
            staffShopService.requireShopAccess(staffId, shopId);
        }
        catch (ServiceException e)
        {
            int status = (e.getCode() != null && e.getCode() > 0) ? e.getCode() : HttpServletResponse.SC_FORBIDDEN;
            writeJson(response, status, AjaxResult.error(status, e.getMessage()));
            return false;
        }
        return true;
    }

    private void writeJson(HttpServletResponse response, int status, Object body) throws IOException
    {
        response.setStatus(status);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        response.getWriter().write(JSON.toJSONString(body));
    }
}
