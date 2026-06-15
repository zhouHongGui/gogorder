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

@Service
public class CTokenServiceImpl implements ICTokenService
{
    @Value("${c-auth.jwt.secret}")
    private String secret;

    private byte[] signingKey;

    @Value("${c-auth.jwt.expire-days:7}")
    private int expireDays;

    @PostConstruct
    public void validateSecret()
    {
        signingKey = secret == null ? new byte[0] : secret.getBytes(StandardCharsets.UTF_8);
        if (signingKey.length < 64)
        {
            throw new IllegalStateException("C_TOKEN_SECRET or TOKEN_SECRET must contain at least 64 UTF-8 bytes");
        }
    }

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
                .setExpiration(new Date(now + expireDays * 24L * 60 * 60 * 1000))
                .signWith(SignatureAlgorithm.HS512, signingKey)
                .compact();
    }

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
            throw new ServiceException("登录状态已失效", 401);
        }
    }
}
