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

/**
 * C 端 Bearer Token 鉴权过滤器（接入 Spring Security 链）。
 *
 * <h3>设计目的（接手必读）</h3>
 * <p>早期 C 端认证只靠 {@code CAuthInterceptor}，而 Spring Security 对 {@code /api/c/**} 全部 permitAll，
 * 防御纵深不足。本过滤器把 C 端 JWT 接入 Spring Security：解析 token → 校验用户存在且未禁用 →
 * 构建 {@link org.springframework.security.core.Authentication} 放入 {@link SecurityContextHolder}，
 * 使 {@code /api/c/**} 可以配 {@code .authenticated()}，由 Security 兜底鉴权。
 *
 * <h3>覆盖范围</h3>
 * 只处理 {@code /api/c/**} 下的<b>非公开</b>路径（auth/shop/category/product 为公开，跳过）。
 * 公开路径见 {@link #shouldNotFilter}，需与 {@code SecurityConfig} 的 permitAll 列表保持一致。
 *
 * <h3>禁用用户实时失效</h3>
 * 每次请求都查 {@code c_user.status}，账号被禁用后即使 token 仍在有效期也会立即失效（返 403）。
 * 代价是每个受保护请求多一次主键查询；高流量下可考虑加短缓存。
 */
@Component
public class CAuthTokenFilter extends OncePerRequestFilter
{
    @Autowired private ICTokenService tokenService;
    @Autowired private CUserMapper cUserMapper;

    /**
     * 跳过公开路径：非 {@code /api/c/} 前缀，或属于 auth/shop/category/product 公开模块。
     * 这些路径不走鉴权（与 SecurityConfig 的 permitAll 列表一致）。
     */
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

    /**
     * 过滤逻辑：解析 token → 校验用户 → 注入 request 属性 + SecurityContext → 放行。
     * 校验失败直接写 JSON 错误响应（401/403），不进入后续 filter chain。
     */
    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException
    {
        // 预检请求直接放行。
        if ("OPTIONS".equalsIgnoreCase(request.getMethod()))
        {
            chain.doFilter(request, response);
            return;
        }
        // 取 Authorization 头，必须是 "Bearer xxx"。
        String token = request.getHeader("Authorization");
        if (StringUtils.isEmpty(token) || !token.startsWith(Constants.TOKEN_PREFIX))
        {
            writeError(response, HttpStatus.UNAUTHORIZED, "请先登录");
            return;
        }
        try
        {
            // 解析 JWT 拿到 userId/phone。
            CAuthPrincipal principal = tokenService.parseToken(token.substring(Constants.TOKEN_PREFIX.length()));
            // 查用户并校验存在性 + 禁用态（每请求一次 DB 查询，保证禁用实时生效）。
            CUser user = cUserMapper.selectById(principal.getUserId());
            if (user == null)
            {
                throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
            }
            if (!Integer.valueOf(1).equals(user.getStatus()))
            {
                // 账号被禁用：即使 token 有效也拒绝。
                throw new ServiceException("账号已被禁用", HttpStatus.FORBIDDEN);
            }
            // 注入 request 属性，供 Controller 的 currentUserId() 读取（向后兼容旧代码）。
            request.setAttribute(CAuthConstants.USER_ID_ATTRIBUTE, user.getId());
            request.setAttribute(CAuthConstants.USER_PHONE_ATTRIBUTE, user.getPhone());
            // 构建 Spring Security 认证对象放入上下文，使 .authenticated() 校验通过。
            UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                    user, null, Collections.emptyList());
            authentication.setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
            SecurityContextHolder.getContext().setAuthentication(authentication);
            chain.doFilter(request, response);
        }
        catch (ServiceException e)
        {
            // 校验异常 → 写错误响应（401/403），带上异常 code。
            writeError(response, e.getCode() == null ? HttpStatus.UNAUTHORIZED : e.getCode(), e.getMessage());
        }
    }

    /** 直接写 JSON 错误响应（不走 Controller，故不经过全局异常处理器）。 */
    private void writeError(HttpServletResponse response, int status, String message) throws IOException
    {
        response.setStatus(status);
        response.setCharacterEncoding("UTF-8");
        response.setContentType("application/json");
        response.getWriter().print(JSON.toJSONString(AjaxResult.error(status, message)));
    }

    /** 判断 path 是否等于 prefix 或以其 + "/" 开头（避免 /api/c/shopXYZ 被误判为 shop 公开路径）。 */
    private boolean isPublicPath(String path, String prefix)
    {
        return path.equals(prefix) || path.startsWith(prefix + "/");
    }
}
