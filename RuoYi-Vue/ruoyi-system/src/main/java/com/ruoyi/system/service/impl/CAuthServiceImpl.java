package com.ruoyi.system.service.impl;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
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
import com.ruoyi.system.service.ICAuthService;
import com.ruoyi.system.service.ICTokenService;

@Service
public class CAuthServiceImpl implements ICAuthService
{
    private static final String WECHAT_PLATFORM_MP = "MP";
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired
    private CUserMapper cUserMapper;

    @Autowired
    private ICTokenService tokenService;

    @Autowired
    private RedisCache redisCache;

    @Value("${c-auth.sms.mock-enabled:true}")
    private boolean smsMockEnabled;

    @Value("${c-auth.wechat.app-id:}")
    private String wechatAppId;

    @Value("${c-auth.wechat.app-secret:}")
    private String wechatAppSecret;

    @Override
    public Map<String, Object> sendSms(String phone, String ip)
    {
        String intervalKey = "c:auth:sms:interval:" + phone;
        if (!redisCache.setCacheObjectIfAbsent(intervalKey, "1", 60, TimeUnit.SECONDS))
        {
            throw new ServiceException("验证码发送过于频繁，请稍后再试");
        }

        String day = LocalDate.now().toString();
        checkDailyLimit("c:auth:sms:phone:" + day + ":" + phone, 10, "该手机号今日验证码发送次数已达上限");
        checkDailyLimit("c:auth:sms:ip:" + day + ":" + ip, 50, "该IP今日验证码发送次数已达上限");

        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        redisCache.setCacheObject("c:auth:sms:code:" + phone, code, 5, TimeUnit.MINUTES);

        Map<String, Object> result = new HashMap<>();
        result.put("expiresIn", 300);
        result.put("retryAfter", 60);
        if (smsMockEnabled)
        {
            result.put("mockCode", code);
            return result;
        }
        throw new ServiceException("短信服务尚未配置");
    }

    @Override
    @Transactional
    public Map<String, Object> loginBySms(String phone, String code)
    {
        String key = "c:auth:sms:code:" + phone;
        String cachedCode = redisCache.getCacheObject(key);
        if (!StringUtils.equals(code, cachedCode))
        {
            throw new ServiceException("验证码错误或已过期");
        }
        redisCache.deleteObject(key);
        return buildLoginResult(findOrCreateUser(phone));
    }

    @Override
    public Map<String, Object> loginByWechat(String code)
    {
        requireWechatConfig();
        String usedKey = "c:auth:wechat:login-code:" + sha256(code);
        if (!redisCache.setCacheObjectIfAbsent(usedKey, "1", 5, TimeUnit.MINUTES))
        {
            throw new ServiceException("微信登录code已使用");
        }

        JSONObject session = requestWechatSession(code);
        String openid = session.getString("openid");
        String unionid = StringUtils.nvl(session.getString("unionid"), "");
        CUserWechat wechat = cUserMapper.selectWechatByOpenid(WECHAT_PLATFORM_MP, openid);
        if (wechat != null)
        {
            Map<String, Object> result = buildLoginResult(requireActiveUser(wechat.getUserId()));
            result.put("bound", true);
            return result;
        }

        String ticket = IdUtils.fastSimpleUUID();
        redisCache.setCacheObject("c:auth:wechat:bind:" + ticket,
                new CWechatBindTicket(openid, unionid), 5, TimeUnit.MINUTES);
        Map<String, Object> result = new HashMap<>();
        result.put("bound", false);
        result.put("bindTicket", ticket);
        return result;
    }

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
        String phone = requestWechatPhone(phoneCode);
        CUser user = findOrCreateUser(phone);

        CUserWechat wechat = new CUserWechat();
        wechat.setUserId(user.getId());
        wechat.setPlatform(WECHAT_PLATFORM_MP);
        wechat.setOpenid(ticket.getOpenid());
        wechat.setUnionid(ticket.getUnionid());
        cUserMapper.insertWechatIfAbsent(wechat);

        CUserWechat bound = cUserMapper.selectWechatByOpenid(WECHAT_PLATFORM_MP, ticket.getOpenid());
        if (bound == null || !user.getId().equals(bound.getUserId()))
        {
            throw new ServiceException("微信身份已绑定其他账号");
        }
        redisCache.deleteObject(ticketKey);
        return buildLoginResult(user);
    }

    @Override
    public CUser getUserInfo(Long userId)
    {
        return requireActiveUser(userId);
    }

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

    protected CUser findOrCreateUser(String phone)
    {
        cUserMapper.insertUserIfAbsent(phone);
        CUser user = cUserMapper.selectByPhone(phone);
        if (user == null)
        {
            throw new ServiceException("创建用户失败");
        }
        if (!Integer.valueOf(1).equals(user.getStatus()))
        {
            throw new ServiceException("账号已被禁用", 403);
        }
        cUserMapper.insertBalanceIfAbsent(user.getId());
        return user;
    }

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

    private Map<String, Object> buildLoginResult(CUser user)
    {
        Map<String, Object> result = new HashMap<>();
        result.put("token", tokenService.createToken(user));
        result.put("userInfo", user);
        return result;
    }

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
        redisCache.setCacheObject(key, accessToken, Math.max(60, expiresIn - 300), TimeUnit.SECONDS);
        return accessToken;
    }

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

    private void requireWechatConfig()
    {
        if (StringUtils.isAnyBlank(wechatAppId, wechatAppSecret))
        {
            throw new ServiceException("微信小程序登录尚未配置");
        }
    }

    private String encode(String value)
    {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

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
