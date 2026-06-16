package com.ruoyi.system.domain.dto;

/**
 * C 端登录主体（JWT 解析结果）。{@code CTokenServiceImpl.parseToken} 返回，
 * 由 {@code CAuthTokenFilter} 据此查库校验用户态。
 */
public class CAuthPrincipal
{
    /** 用户 ID。 */
    private Long userId;
    /** 手机号。 */
    private String phone;

    public CAuthPrincipal(Long userId, String phone)
    {
        this.userId = userId;
        this.phone = phone;
    }

    public Long getUserId() { return userId; }
    public String getPhone() { return phone; }
}
