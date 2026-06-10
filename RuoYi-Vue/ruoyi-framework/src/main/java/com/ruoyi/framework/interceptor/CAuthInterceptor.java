package com.ruoyi.framework.interceptor;

import java.io.IOException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.CAuthConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.dto.CAuthPrincipal;
import com.ruoyi.system.service.ICTokenService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * C端 Bearer Token 鉴权
 */
@Component
public class CAuthInterceptor implements HandlerInterceptor
{
    @Autowired
    private ICTokenService tokenService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException
    {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            return true;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX))
        {
            writeUnauthorized(response, "请先登录");
            return false;
        }
        try
        {
            CAuthPrincipal principal = tokenService.parseToken(token.substring(Constants.TOKEN_PREFIX.length()));
            request.setAttribute(CAuthConstants.USER_ID_ATTRIBUTE, principal.getUserId());
            request.setAttribute(CAuthConstants.USER_PHONE_ATTRIBUTE, principal.getPhone());
            return true;
        }
        catch (ServiceException e)
        {
            writeUnauthorized(response, e.getMessage());
            return false;
        }
    }

    private void writeUnauthorized(HttpServletResponse response, String message) throws IOException
    {
        response.setStatus(HttpStatus.UNAUTHORIZED);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().print(JSON.toJSONString(AjaxResult.error(HttpStatus.UNAUTHORIZED, message)));
    }
}
