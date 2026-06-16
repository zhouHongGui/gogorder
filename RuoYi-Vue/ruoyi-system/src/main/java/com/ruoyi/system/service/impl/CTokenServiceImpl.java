package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.dto.CAuthPrincipal;
import com.ruoyi.system.service.ICTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import jakarta.annotation.PostConstruct;

/**
 * C 端 JWT Token 生成与解析。
 *
 * <h3>安全要点（接手必读）</h3>
 * <ul>
 *   <li><b>密钥强制校验</b>：{@link #validateSecret} 在 Bean 初始化时强制要求 secret ≥ 64 字节，
 *       否则应用启动失败。杜绝了「使用弱/默认密钥导致 token 可伪造」的风险。</li>
 *   <li><b>无默认密钥</b>：secret 通过 {@code c-auth.jwt.secret} 配置注入，生产建议由环境变量
 *       {@code C_TOKEN_SECRET}/{@code TOKEN_SECRET} 提供，配置文件不再提供明文默认值（未配置即启动失败，fail-fast）。</li>
 *   <li><b>算法</b>：HS512（要求 ≥64 字节密钥），claim 含 userId、phone。</li>
 *   <li><b>有效期</b>：默认 7 天，可由 {@code c-auth.jwt.expire-days} 配置。</li>
 * </ul>
 *
 * <p>注意：当前依赖 jjwt 0.9.1，{@code signWith(SignatureAlgorithm, byte[])} 等为旧式 API，
 * 升级 jjwt 时需同步迁移到 {@code Keys.hmacShaKeyFor} 新 API。
 */
@Service
public class CTokenServiceImpl implements ICTokenService
{
    @Value("${c-auth.jwt.secret}")
    private String secret;

    /** 签名密钥的字节形式，{@link #validateSecret} 中初始化，createToken/parseToken 共用。 */
    private byte[] signingKey;

    /** Token 有效期（天），默认 7。 */
    @Value("${c-auth.jwt.expire-days:7}")
    private int expireDays;

    /**
     * Bean 初始化后校验密钥强度：必须 ≥ 64 字节（HS512 要求 512 位）。
     *
     * @throws IllegalStateException 密钥过短，启动失败（fail-fast）
     */
    @PostConstruct
    public void validateSecret()
    {
        signingKey = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (signingKey.length < 64)
        {
            throw new IllegalStateException("C_TOKEN_SECRET or TOKEN_SECRET must contain at least 64 UTF-8 bytes");
        }
    }

    /**
     * 签发 JWT。claim 含 userId、phone，subject 固定为 "c-user"。
     *
     * @param user 已通过登录校验的用户
     * @return JWT 字符串
     */
    @Override
    public String createToken(CUser user)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", user.getId());
        claims.put("phone", user.getPhone());
        long now = System.currentTimeMillis();
        return Jwts.builder()
                .setClaims(claims)
                .setSubject("c-user")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireDays * 24L * 60 * 60 * 1000))   // 过期时间 = 现在 + N 天
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

    /**
     * 解析 JWT，取出 userId/phone 封装为 {@link CAuthPrincipal}。
     *
     * @param token Bearer Token 去掉前缀后的纯 JWT
     * @return 用户主体
     * @throws ServiceException token 非法/过期/签名不符 → 统一提示「登录状态已失效」(401)
     */
    @Override
    public CAuthPrincipal parseToken(String token)
    {
        try
        {
            Claims claims = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token).getBody();
            Long userId = Long.valueOf(String.valueOf(claims.get("userId")));
            String phone = String.valueOf(claims.get("phone"));
            return new CAuthPrincipal(userId, phone);
        }
        catch (Exception e)
        {
            // 任何解析/校验失败都视为未登录，不向调用方暴露具体原因。
            throw new ServiceException("登录状态已失效", 401);
        }
    }
}
