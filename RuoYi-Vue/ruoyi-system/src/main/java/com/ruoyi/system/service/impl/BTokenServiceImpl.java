package com.ruoyi.system.service.impl;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.BAuthPrincipal;
import com.ruoyi.system.service.IBTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;

/** 独立门店员工 JWT 实现。 */
@Service
public class BTokenServiceImpl implements IBTokenService
{
    @Value("${staff-auth.jwt.secret}")
    private String secret;
    @Value("${staff-auth.jwt.expire-days:7}")
    private int expireDays;
    private byte[] signingKey;

    @PostConstruct
    public void validateSecret()
    {
        signingKey = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (signingKey.length < 64)
            throw new IllegalStateException("STAFF_TOKEN_SECRET or TOKEN_SECRET must contain at least 64 UTF-8 bytes");
    }

    @Override
    public String createToken(ShopStaff staff)
    {
        Map<String, Object> claims = new HashMap<>();
        claims.put("staffId", staff.getId());
        claims.put("phone", staff.getPhone());
        claims.put("tokenVersion", staff.getTokenVersion());
        long now = System.currentTimeMillis();
        return Jwts.builder().setClaims(claims).setSubject("shop-staff")
                .setIssuedAt(new Date(now))
                .setExpiration(new Date(now + expireDays * 24L * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS512, signingKey).compact();
    }

    @Override
    public BAuthPrincipal parseToken(String token)
    {
        try
        {
            Claims claims = Jwts.parser().setSigningKey(signingKey).parseClaimsJws(token).getBody();
            if (!"shop-staff".equals(claims.getSubject())) throw new IllegalArgumentException("subject");
            return new BAuthPrincipal(Long.valueOf(String.valueOf(claims.get("staffId"))),
                    String.valueOf(claims.get("phone")),
                    Integer.valueOf(String.valueOf(claims.get("tokenVersion"))));
        }
        catch (Exception e)
        {
            throw new ServiceException("登录状态已失效", HttpStatus.UNAUTHORIZED);
        }
    }
}
