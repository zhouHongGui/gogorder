package com.ruoyi.framework.security.filter;

import java.io.IOException;
import java.util.Collections;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import com.alibaba.fastjson2.JSON;
import com.ruoyi.common.constant.CAuthConstants;
import com.ruoyi.common.constant.Constants;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.domain.AjaxResult;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CAuthPrincipal;
import com.ruoyi.system.mapper.CUserMapper;
import com.ruoyi.system.service.ICTokenService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

@Component
public class CAuthTokenFilter extends OncePerRequestFilter
{
    @Autowired private ICTokenService tokenService;
    @Autowired private CUserMapper cUserMapper;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request)
    {
        String path = request.getRequestURI();
        return !path.startsWith("/api/c/")
                || isPublicPath(path, "/api/c/auth")
                || isPublicPath(path, "/api/c/shop")
                || isPublicPath(path, "/api/c/category")
                || isPublicPath(path, "/api/c/product");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            chain.doFilter(request, response);
            return;
        }
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX))
        {
            writeError(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return;
        }
        try
        {
            CAuthPrincipal principal = tokenService.parseToken(token.substring(Constants.TOKEN_PREFIX.length()));
            CUser user = cUserMapper.selectById(principal.getUserId());
            if (user == null)
            {
                throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
            }
            if (!Integer.valueOf(1).equals(user.getStatus()))
            {
                throw new ServiceException("账号已被禁用", HttpStatus.FORBIDDEN);
            }
            request.setAttribute(CAuthConstants.USER_ID_ATTRIBUTE, user.getId());
            request.setAttribute(CAuthConstants.USER_PHONE_ATTRIBUTE, user.getPhone());
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
        catch (ServiceException e)
        {
            writeError(response, e.getCode() == null ? HttpStatus.UNAUTHORIZED : e.getCode(), e.getMessage());
        }
    }

    private void writeError(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().print(JSON.toJSONString(AjaxResult.error(status, message)));
    }

    private boolean isPublicPath(String path, String prefix)
    {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
