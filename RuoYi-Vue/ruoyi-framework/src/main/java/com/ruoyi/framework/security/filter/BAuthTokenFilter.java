package com.ruoyi.framework.security.filter;

import java.io.IOException;
import java.util.Collections;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.BAuthConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.BAuthPrincipal;
import com.ruoyi.system.mapper.ShopStaffMapper;
import com.ruoyi.system.service.IBTokenService;

/** 门店员工端独立 Bearer Token 过滤器。 */
@Component
public class BAuthTokenFilter extends OncePerRequestFilter
{
    private static final Logger log = LoggerFactory.getLogger(BAuthTokenFilter.class);
    @Autowired private IBTokenService tokenService;
    @Autowired private ShopStaffMapper shopStaffMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return !path.startsWith("/api/b/") || path.equals("/api/b/auth") || path.startsWith("/api/b/auth/");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) { chain.doFilter(request, response); return; }
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX))
        {
            writeError(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return;
        }
        try
        {
            BAuthPrincipal principal = tokenService.parseToken(token.substring(Constants.TOKEN_PREFIX.length()));
            ShopStaff staff = shopStaffMapper.selectById(principal.getStaffId());
            if (staff == null) throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
            if (!Integer.valueOf(0).equals(staff.getStatus()))
                throw new ServiceException("员工账号已停用", HttpStatus.FORBIDDEN);
            if (!staff.getTokenVersion().equals(principal.getTokenVersion()))
                throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
            request.setAttribute(BAuthConstants.STAFF_ID_ATTRIBUTE, staff.getId());
            request.setAttribute(BAuthConstants.STAFF_PHONE_ATTRIBUTE, staff.getPhone());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    staff, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
        catch (ServiceException e)
        {
            int code = e.getCode() == null ? HttpStatus.UNAUTHORIZED : e.getCode();
            log.warn("员工端鉴权失败 uri={} code={} msg={}", request.getRequestURI(), code, e.getMessage());
            writeError(response, code, e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().print(JSON.toJSONString(AjaxResult.error(status, message)));
    }
}
