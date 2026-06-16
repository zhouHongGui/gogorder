package com.ruoyi.common.constant;

/**
 * C 端认证相关常量。
 *
 * <p>{@code CAuthTokenFilter} 解析 JWT 并校验用户态后，把 userId/phone 写入 request 属性，
 * Controller 的 {@code currentUserId()} 从这些属性读取当前登录用户。
 */
public class CAuthConstants
{
    /** request 属性 key：当前 C 端用户 ID（由 CAuthTokenFilter 注入）。 */
    public static final String USER_ID_ATTRIBUTE = "cUserId";
    /** request 属性 key：当前 C 端用户手机号（由 CAuthTokenFilter 注入）。 */
    public static final String USER_PHONE_ATTRIBUTE = "cUserPhone";

    private CAuthConstants()
    {
    }
}
