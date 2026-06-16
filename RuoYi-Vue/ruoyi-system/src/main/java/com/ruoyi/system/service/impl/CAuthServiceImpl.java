package com.ruoyi.system.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONObject;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.common.utils.http.HttpUtils;
import com.ruoyi.common.utils.uuid.IdUtils;
import com.ruoyi.system.domain.CUser;
import com.ruoyi.system.domain.CUserWechat;
import com.ruoyi.system.domain.dto.CUserUpdateRequest;
import com.ruoyi.system.domain.dto.CWechatBindTicket;
import com.ruoyi.system.mapper.CUserMapper;
import com.ruoyi.system.mapper.CUserBalanceMapper;
import com.ruoyi.system.service.ICAuthService;
import com.ruoyi.system.service.ICTokenService;

/**
 * C 端用户认证服务。
 *
 * <h3>登录方式</h3>
 * <ul>
 *   <li><b>短信验证码登录</b>（H5/通用）：手机号 + 6 位验证码。</li>
 *   <li><b>微信小程序登录</b>：login code 换 openid（未绑定则发 bindTicket，前端再用 phone code 换手机号绑定）。</li>
 * </ul>
 *
 * <h3>安全机制（接手必读）</h3>
 * <ul>
 *   <li><b>短信发送频控</b>：同一手机号 60 秒间隔、每日 10 条；同一 IP 每日 50 条。</li>
 *   <li><b>验证码防爆破</b>：同一手机号验证失败累计 {@value #SMS_MAX_VERIFY_FAILURES} 次即作废验证码并锁 5 分钟。
 *       避免攻击者在 5 分钟有效期内枚举 10^6 种 6 位码。</li>
 *   <li><b>mock 模式</b>：仅开发用（默认关闭）。开启时直接把验证码返回给前端，生产必须关闭。</li>
 *   <li><b>账号禁用</b>：登录与各业务接口均校验 {@code c_user.status=1}，被禁用账号无法登录/操作。</li>
 *   <li><b>微信 code 一次性</b>：login code 用后即标记，防重放。</li>
 * </ul>
 *
 * <h3>用户与余额</h3>
 * 新用户在首次登录时由 {@link #findOrCreateUser}「自动注册」（建用户行 + 余额账户），V1.0 初始余额为 0，
 * 仅管理后台可充值。
 */
@Service
public class CAuthServiceImpl implements ICAuthService
{
    private static final Logger log = LoggerFactory.getLogger(CAuthServiceImpl.class);

    /** 微信平台标识：MP=小程序。 */
    private static final String WECHAT_PLATFORM_MP = "MP";
    /** 短信验证码最大验证失败次数，超过即作废验证码并锁定。 */
    private static final int SMS_MAX_VERIFY_FAILURES = 5;
    /** 生成验证码/取餐号用的安全随机源。 */
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private CUserMapper cUserMapper;

    @Autowired
    private CUserBalanceMapper cUserBalanceMapper;

    @Autowired
    private ICTokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    /** 短信 mock 开关。生产必须为 false；默认 false（未配置环境变量时）。 */
    @Value("${c-auth.sms.mock-enabled:false}")
    private boolean smsMockEnabled;

    @Value("${c-auth.wechat.app-id:}")
    private String wechatAppId;

    @Value("${c-auth.wechat.app-secret:}")
    private String wechatAppSecret;

    /**
     * 发送短信验证码。
     *
     * <p>频控：手机号 60 秒间隔 + 每日 10 条、IP 每日 50 条。通过后生成 6 位验证码存 Redis（5 分钟有效），
     * 并清空该手机号之前的验证失败计数。mock 模式下直接把验证码返回前端。
     *
     * @param phone 手机号
     * @param ip    调用方 IP（用于 IP 维度频控）
     * @return 含 expiresIn/retryAfter，mock 模式额外含 mockCode
     */
    @Override
    public Map<String, Object> sendSms(String phone, String ip)
    {
        // 频控 1：同一手机号 60 秒内只能发一次（SET NX 实现，原子）。
        String intervalKey = "c:auth:sms:interval:" + phone;
        if (!redisCache.setCacheObjectIfAbsent(intervalKey, "1", 60, TimeUnit.SECONDS))
        {
            throw new ServiceException("验证码发送过于频繁，请稍后再试");
        }

        String day = LocalDate.now().toString();
        // 频控 2/3：手机号每日 10 条、IP 每日 50 条。
        checkDailyLimit("c:auth:sms:phone:" + day + ":" + phone, 10, "该手机号今日验证码发送次数已达上限");
        checkDailyLimit("c:auth:sms:ip:" + day + ":" + ip, 50, "该IP今日验证码发送次数已达上限");

        // 生成 6 位数字验证码，5 分钟有效。
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisCache.setCacheObject("c:auth:sms:code:" + phone, code, 5, TimeUnit.MINUTES);
        // 新发验证码清空旧失败计数，给用户重新尝试的机会。
        redisCache.deleteObject(smsVerifyFailureKey(phone));

        Map<String, Object> result = new HashMap<>();
        result.put("expiresIn", 300);
        result.put("retryAfter", 60);
        if (smsMockEnabled)
        {
            // 仅开发模式：直接返回验证码，方便联调。生产必须关闭。
            result.put("mockCode", code);
            return result;
        }
        // 非 mock 模式：V1.0 尚未接入真实短信通道，直接抛异常提示未配置。
        throw new ServiceException("短信服务尚未配置");
    }

    /**
     * 短信验证码登录。
     *
     * <p>防爆破：失败累计达 {@link #SMS_MAX_VERIFY_FAILURES} 次即作废验证码并锁 5 分钟。
     * 验证通过则删除验证码 + 失败计数，并 {@link #findOrCreateUser} 自动注册/取用户，签发 token。
     *
     * @param phone 手机号
     * @param code  6 位验证码
     * @return 含 token、userInfo；账号被禁用抛 403
     */
    @Override
    @Transactional
    public Map<String, Object> loginBySms(String phone, String code)
    {
        String key = "c:auth:sms:code:" + phone;
        String failureKey = smsVerifyFailureKey(phone);
        // 防爆破：失败次数已达上限，拒绝（需重新获取验证码）。
        Number failures = redisCache.getCacheObject(failureKey);
        if (failures != null && failures.longValue() >= SMS_MAX_VERIFY_FAILURES)
        {
            throw new ServiceException("验证码错误次数过多，请重新获取");
        }
        String cachedCode = redisCache.getCacheObject(key);
        if (!StringUtils.equals(code, cachedCode))
        {
            // 验证失败：累加失败计数；首次设置 5 分钟过期；达上限作废验证码。
            long failureCount = redisCache.increment(failureKey, 1);
            if (failureCount == 1)
            {
                redisCache.expire(failureKey, 5, TimeUnit.MINUTES);
            }
            if (failureCount >= SMS_MAX_VERIFY_FAILURES)
            {
                redisCache.deleteObject(key);   // 作废验证码，阻止继续枚举
                throw new ServiceException("验证码错误次数过多，请重新获取");
            }
            log.warn("短信登录失败(验证码错误) phone={} failureCount={}", maskPhone(phone), failureCount);
            throw new ServiceException("验证码错误或已过期");
        }
        // 验证通过：清理验证码与失败计数，签发登录态。
        redisCache.deleteObject(key);
        redisCache.deleteObject(failureKey);
        CUser loginUser = findOrCreateUser(phone);
        log.info("短信登录成功 userId={} phone={}", loginUser.getId(), maskPhone(phone));
        return buildLoginResult(loginUser);
    }

    /**
     * 微信小程序登录（第一步：login code 换 openid）。
     *
     * <p>已绑定手机号 → 直接签发 token（bound=true）；未绑定 → 返回 bindTicket，
     * 前端拿到 bindTicket 后调 {@link #bindWechatPhone} 用 phone code 绑定手机号。
     *
     * @param code 微信 login code（一次性）
     * @return bound=true 含 token；bound=false 含 bindTicket
     */
    @Override
    public Map<String, Object> loginByWechat(String code)
    {
        requireWechatConfig();
        // login code 一次性消费：用 SHA-256(code) 做 key 标记，5 分钟内不可重放。
        String usedKey = "c:auth:wechat:login-code:" + sha256(code);
        if (!redisCache.setCacheObjectIfAbsent(usedKey, "1", 5, TimeUnit.MINUTES))
        {
            throw new ServiceException("微信登录code已使用");
        }

        JSONObject session = requestWechatSession(code);
        String openid = session.getString("openid");
        String unionid = StringUtils.nvl(session.getString("unionid"), "");
        // 已绑定 → 直接登录。
        CUserWechat wechat = cUserMapper.selectWechatByOpenid(WECHAT_PLATFORM_MP, openid);
        if (wechat != null)
        {
            CUser wechatUser = requireActiveUser(wechat.getUserId());
            log.info("微信登录成功(已绑定) userId={} phone={}", wechatUser.getId(), maskPhone(wechatUser.getPhone()));
            Map<String, Object> result = buildLoginResult(wechatUser);
            result.put("bound", true);
            return result;
        }

        // 未绑定 → 发一张 5 分钟有效的 bindTicket，前端用它 + phone code 完成绑定。
        String ticket = IdUtils.fastSimpleUUID();
        redisCache.setCacheObject("c:auth:wechat:bind:" + ticket,
                new CWechatBindTicket(openid, unionid), 5, TimeUnit.MINUTES);
        Map<String, Object> result = new HashMap<>();
        result.put("bound", false);
        result.put("bindTicket", ticket);
        return result;
    }

    /**
     * 微信绑定手机号（第二步）。用 bindTicket 取回 openid，用 phone code 向微信换手机号，
     * 然后 {@link #findOrCreateUser} 建用户、写入微信身份绑定。
     *
     * <p>绑定冲突防护：若该 openid 已绑定到其他用户，抛异常拒绝。
     *
     * @param bindTicket loginByWechat 返回的绑定凭证
     * @param phoneCode  微信获取手机号的 phone code
     * @return 含 token、userInfo
     */
    @Override
    @Transactional
    public Map<String, Object> bindWechatPhone(String bindTicket, String phoneCode)
    {
        String ticketKey = "c:auth:wechat:bind:" + bindTicket;
        CWechatBindTicket ticket = redisCache.getCacheObject(ticketKey);
        if (ticket == null)
        {
            throw new ServiceException("绑定凭证无效或已过期");
        }
        // 用 phone code 向微信换取手机号。
        String phone = requestWechatPhone(phoneCode);
        CUser user = findOrCreateUser(phone);

        // 写入微信身份绑定（openid+unionid → userId），幂等插入。
        CUserWechat wechat = new CUserWechat();
        wechat.setUserId(user.getId());
        wechat.setPlatform(WECHAT_PLATFORM_MP);
        wechat.setOpenid(ticket.getOpenid());
        wechat.setUnionid(ticket.getUnionid());
        cUserMapper.insertWechatIfAbsent(wechat);

        // 复查：确保绑定到的是当前用户（并发下可能被其他用户抢先绑定）。
        CUserWechat bound = cUserMapper.selectWechatByOpenid(WECHAT_PLATFORM_MP, ticket.getOpenid());
        if (bound == null || !user.getId().equals(bound.getUserId()))
        {
            log.warn("微信绑定冲突(身份已绑其他账号) userId={}", user.getId());
            throw new ServiceException("微信身份已绑定其他账号");
        }
        redisCache.deleteObject(ticketKey);
        log.info("微信绑定手机号成功 userId={} phone={}", user.getId(), maskPhone(user.getPhone()));
        return buildLoginResult(user);
    }

    /** 获取当前用户信息（校验账号可用）。 */
    @Override
    public CUser getUserInfo(Long userId)
    {
        return requireActiveUser(userId);
    }

    /** 获取当前用户余额（校验账号可用）。余额账户不存在视为 0。 */
    @Override
    public Map<String, Object> getUserBalance(Long userId)
    {
        requireActiveUser(userId);
        var balance = cUserBalanceMapper.selectByUserId(userId);
        return Map.of("balance", balance == null ? 0 : balance.getBalance());
    }

    /**
     * 更新用户资料（昵称/头像）。先校验账号可用，更新后返回最新用户信息。
     */
    @Override
    public CUser updateUser(Long userId, CUserUpdateRequest request)
    {
        requireActiveUser(userId);
        CUser user = new CUser();
        user.setId(userId);
        user.setNickname(request.getNickname());
        user.setAvatar(request.getAvatar());
        cUserMapper.updateUser(user);
        return requireActiveUser(userId);
    }

    /**
     * 按手机号「查或建」用户，并确保余额账户存在。被 {@code @Transactional} 方法调用，
     * 用户创建与余额账户创建在同一事务内。
     *
     * @throws ServiceException 创建失败、或账号已被禁用(403)
     */
    protected CUser findOrCreateUser(String phone)
    {
        // 幂等插入用户（已存在则跳过），适合并发首次登录。
        int inserted = cUserMapper.insertUserIfAbsent(phone);
        CUser user = cUserMapper.selectByPhone(phone);
        if (user == null)
        {
            throw new ServiceException("创建用户失败");
        }
        if (inserted > 0)
        {
            log.info("新用户注册 userId={} phone={}", user.getId(), maskPhone(phone));
        }
        // 禁用账号禁止登录。
        if (!Integer.valueOf(1).equals(user.getStatus()))
        {
            log.warn("登录被拒(账号禁用) userId={} phone={}", user.getId(), maskPhone(phone));
            throw new ServiceException("账号已被禁用", 403);
        }
        // 确保余额账户存在（幂等），V1.0 初始余额 0。
        cUserMapper.insertBalanceIfAbsent(user.getId());
        return user;
    }

    /**
     * 校验用户存在且未被禁用，返回用户实体。被各业务方法调用做账号可用性校验。
     *
     * @throws ServiceException 用户不存在(401) 或已禁用(403)
     */
    private CUser requireActiveUser(Long userId)
    {
        CUser user = cUserMapper.selectById(userId);
        if (user == null)
        {
            throw new ServiceException("用户不存在", 401);
        }
        if (!Integer.valueOf(1).equals(user.getStatus()))
        {
            throw new ServiceException("账号已被禁用", 403);
        }
        return user;
    }

    /** 组装登录返回结果：签发 token + 用户信息。 */
    private Map<String, Object> buildLoginResult(CUser user)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.createToken(user));
        result.put("userInfo", user);
        return result;
    }

    /**
     * 日频次检查：用 Redis INCR 计数，首次设置 2 天过期，超限抛异常。
     *
     * @param key     计数 key（含日期维度）
     * @param limit   上限
     * @param message 超限提示
     */
    private void checkDailyLimit(String key, int limit, String message)
    {
        long count = redisCache.increment(key, 1);
        if (count == 1)
        {
            redisCache.expire(key, 2, TimeUnit.DAYS);
        }
        if (count > limit)
        {
            throw new ServiceException(message);
        }
    }

    /** 短信验证失败计数 key。 */
    private String smsVerifyFailureKey(String phone)
    {
        return "c:auth:sms:verify-fail:" + phone;
    }

    /**
     * 用 login code 调微信 jscode2session 换取 openid/unionid。
     *
     * @throws ServiceException 微信返回无 openid
     */
    private JSONObject requestWechatSession(String code)
    {
        String params = "appid=" + encode(wechatAppId)
                + "&secret=" + encode(wechatAppSecret)
                + "&js_code=" + encode(code)
                + "&grant_type=authorization_code";
        JSONObject result = parseWechatResponse(HttpUtils.sendGet("https://api.weixin.qq.com/sns/jscode2session", params));
        if (StringUtils.isEmpty(result.getString("openid")))
        {
            throw new ServiceException("微信登录失败");
        }
        return result;
    }

    /**
     * 用 phone code 调微信接口换取手机号（新版动态 phone code 方式）。
     *
     * @throws ServiceException 获取失败
     */
    private String requestWechatPhone(String phoneCode)
    {
        requireWechatConfig();
        String accessToken = getWechatAccessToken();
        String body = JSON.toJSONString(Map.of("code", phoneCode));
        JSONObject result = parseWechatResponse(HttpUtils.sendPost(
                "https://api.weixin.qq.com/wxa/business/getuserphonenumber?access_token=" + encode(accessToken),
                body, MediaType.APPLICATION_JSON_VALUE));
        String phone = result.getJSONObject("phone_info") == null
                ? null : result.getJSONObject("phone_info").getString("phoneNumber");
        if (StringUtils.isEmpty(phone))
        {
            throw new ServiceException("获取微信手机号失败");
        }
        return phone;
    }

    /**
     * 获取微信 access_token（带 Redis 缓存，提前 5 分钟过期避免边界问题）。
     */
    private String getWechatAccessToken()
    {
        String key = "c:auth:wechat:access-token";
        String cached = redisCache.getCacheObject(key);
        if (StringUtils.isNotEmpty(cached))
        {
            return cached;
        }
        String params = "grant_type=client_credential&appid=" + encode(wechatAppId)
                + "&secret=" + encode(wechatAppSecret);
        JSONObject result = parseWechatResponse(HttpUtils.sendGet("https://api.weixin.qq.com/cgi-bin/token", params));
        String accessToken = result.getString("access_token");
        if (StringUtils.isEmpty(accessToken))
        {
            throw new ServiceException("获取微信访问凭证失败");
        }
        int expiresIn = result.getIntValue("expires_in", 7200);
        // 提前 300 秒过期，避免使用即将失效的 token。
        redisCache.setCacheObject(key, accessToken, Math.max(60, expiresIn - 300), TimeUnit.SECONDS);
        return accessToken;
    }

    /**
     * 解析微信接口响应：空响应或 errcode!=0 抛异常。
     */
    private JSONObject parseWechatResponse(String response)
    {
        if (StringUtils.isEmpty(response))
        {
            throw new ServiceException("微信服务暂不可用");
        }
        JSONObject result = JSON.parseObject(response);
        if (result.getIntValue("errcode") != 0)
        {
            throw new ServiceException("微信服务调用失败：" + result.getString("errmsg"));
        }
        return result;
    }

    /** 校验微信小程序配置（appId/appSecret）已配置。 */
    private void requireWechatConfig()
    {
        if (StringUtils.isAnyBlank(wechatAppId, wechatAppSecret))
        {
            throw new ServiceException("微信小程序登录尚未配置");
        }
    }

    /** URL 编码工具（UTF-8）。 */
    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * 手机号脱敏：保留前 3 位和后 4 位，中间用 **** 替换（如 138****1234）。
     * 长度不足或为空时返回 ***，避免日志泄露完整手机号。
     */
    private String maskPhone(String phone)
    {
        if (phone == null || phone.length() < 7)
        {
            return "***";
        }
        return phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }

    /**
     * 计算字符串的 SHA-256 十六进制摘要（用于微信 login code 去重 key，避免明文存长串）。
     */
    private String sha256(String value)
    {
        try
        {
            byte[] bytes = MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8));
            StringBuilder result = new StringBuilder();
            for (byte item : bytes)
            {
                result.append(String.format("%02x", item));
            }
            return result.toString();
        }
        catch (Exception e)
        {
            throw new ServiceException("生成登录凭证摘要失败");
        }
    }
}
