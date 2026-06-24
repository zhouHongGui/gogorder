package com.ruoyi.framework.web.service;

import java.security.SecureRandom;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.ruoyi.common.constant.HttpStatus;
import com.ruoyi.common.core.redis.RedisCache;
import com.ruoyi.common.exception.ServiceException;
import com.ruoyi.common.utils.SecurityUtils;
import com.ruoyi.common.utils.StringUtils;
import com.ruoyi.system.domain.ShopStaff;
import com.ruoyi.system.domain.dto.BLoginResult;
import com.ruoyi.system.domain.dto.BShopContextView;
import com.ruoyi.system.domain.dto.BStaffProfileView;
import com.ruoyi.system.mapper.ShopStaffMapper;
import com.ruoyi.system.mapper.StaffShopMapper;
import com.ruoyi.system.service.IBTokenService;

/** 独立门店员工认证与门店上下文服务。 */
@Component
public class StaffAuthService
{
    private static final Logger log = LoggerFactory.getLogger(StaffAuthService.class);
    private static final int SMS_MAX_VERIFY_FAILURES = 5;
    private static final int PASSWORD_MAX_FAILURES = 5;
    private static final SecureRandom RANDOM = new SecureRandom();

    @Autowired private ShopStaffMapper shopStaffMapper;
    @Autowired private StaffShopMapper staffShopMapper;
    @Autowired private IBTokenService tokenService;
    @Autowired private RedisCache redisCache;

    @Value("${staff-auth.sms.mock-enabled:${c-auth.sms.mock-enabled:false}}")
    private boolean smsMockEnabled;

    public Map<String, Object> sendSms(String phone, String ip)
    {
        String intervalKey = "staff:auth:sms:interval:" + phone;
        if (!redisCache.setCacheObjectIfAbsent(intervalKey, "1", 60, TimeUnit.SECONDS))
            throw new ServiceException("验证码发送过于频繁，请稍后再试", HttpStatus.BAD_REQUEST);
        String day = LocalDate.now().toString();
        checkDailyLimit("staff:auth:sms:phone:" + day + ":" + phone, 10, "今日验证码发送次数已达上限");
        checkDailyLimit("staff:auth:sms:ip:" + day + ":" + ip, 50, "当前网络今日验证码发送次数已达上限");

        Map<String, Object> result = new HashMap<>();
        result.put("expiresIn", 300);
        result.put("retryAfter", 60);
        if (!smsMockEnabled) throw new ServiceException("短信服务尚未配置", HttpStatus.NOT_IMPLEMENTED);

        ShopStaff staff = shopStaffMapper.selectByPhone(phone);
        if (staff == null)
        {
            // 未登记手机号静默返回：不发码、不回传 mockCode，避免账号枚举
            log.warn("员工短信发送忽略(手机号未登记) phone={} ip={}", maskPhone(phone), ip);
            return result;
        }
        String code = String.format("%06d", RANDOM.nextInt(1_000_000));
        result.put("mockCode", code);
        redisCache.setCacheObject(smsCodeKey(phone), code, 5, TimeUnit.MINUTES);
        redisCache.deleteObject(smsFailureKey(phone));
        log.info("员工短信验证码已生成 staffId={} phone={}", staff.getId(), maskPhone(phone));
        return result;
    }

    public BLoginResult loginBySms(String phone, String code, String ip)
    {
        String failureKey = smsFailureKey(phone);
        Number failures = redisCache.getCacheObject(failureKey);
        if (failures != null && failures.longValue() >= SMS_MAX_VERIFY_FAILURES)
            throw new ServiceException("账号或验证码错误", HttpStatus.UNAUTHORIZED);
        String cachedCode = redisCache.getCacheObject(smsCodeKey(phone));
        if (!StringUtils.equals(code, cachedCode))
        {
            recordSmsFailure(phone, failureKey);
            throw new ServiceException("账号或验证码错误", HttpStatus.UNAUTHORIZED);
        }
        ShopStaff staff = requireActiveStaff(shopStaffMapper.selectByPhone(phone));
        List<BShopContextView> shops = requireShopContexts(staff.getId());
        redisCache.deleteObject(smsCodeKey(phone));
        redisCache.deleteObject(failureKey);
        shopStaffMapper.updateLoginInfo(staff.getId(), ip);
        log.info("员工短信登录成功 staffId={} phone={} defaultShopId={}",
                staff.getId(), maskPhone(phone), findDefaultShop(shops).getShopId());
        return buildLoginResult(staff, shops);
    }

    public BLoginResult loginByPassword(String account, String password, String ip)
    {
        String failureKey = pwdFailureKey(account);
        Number failures = redisCache.getCacheObject(failureKey);
        if (failures != null && failures.longValue() >= PASSWORD_MAX_FAILURES)
            throw new ServiceException("账号或密码错误", HttpStatus.UNAUTHORIZED);
        ShopStaff staff = shopStaffMapper.selectByAccount(account);
        if (staff == null || !SecurityUtils.matchesPassword(password, staff.getPassword()))
        {
            recordPwdFailure(account, failureKey);
            throw new ServiceException("账号或密码错误", HttpStatus.UNAUTHORIZED);
        }
        requireActiveStaff(staff);
        List<BShopContextView> shops = requireShopContexts(staff.getId());
        redisCache.deleteObject(failureKey);
        shopStaffMapper.updateLoginInfo(staff.getId(), ip);
        log.info("员工密码登录成功 staffId={} account={} defaultShopId={}",
                staff.getId(), account, findDefaultShop(shops).getShopId());
        return buildLoginResult(staff, shops);
    }

    public List<BShopContextView> getShopContexts(Long staffId)
    {
        requireActiveStaff(shopStaffMapper.selectById(staffId));
        return requireShopContexts(staffId);
    }

    public BShopContextView requireShopContext(Long staffId, Long shopId)
    {
        if (shopId == null) throw new ServiceException("门店不能为空", HttpStatus.BAD_REQUEST);
        return getShopContexts(staffId).stream().filter(item -> shopId.equals(item.getShopId())).findFirst()
                .orElseThrow(() -> new ServiceException("无权访问当前门店", HttpStatus.FORBIDDEN));
    }

    public BShopContextView switchShopContext(Long staffId, Long shopId)
    {
        List<BShopContextView> shops = getShopContexts(staffId);
        return shops.stream().filter(item -> shopId != null && shopId.equals(item.getShopId())).findFirst()
                .orElseThrow(() -> new ServiceException("无权访问目标门店", HttpStatus.FORBIDDEN));
    }

    private BLoginResult buildLoginResult(ShopStaff staff, List<BShopContextView> shops)
    {
        BStaffProfileView profile = new BStaffProfileView();
        profile.setStaffId(staff.getId());
        profile.setAccount(staff.getAccount());
        profile.setNickname(staff.getNickname());
        profile.setPhone(maskPhone(staff.getPhone()));
        BLoginResult result = new BLoginResult();
        result.setToken(tokenService.createToken(staff));
        result.setUserInfo(profile);
        result.setShops(shops);
        result.setCurrentShop(findDefaultShop(shops));
        return result;
    }

    private List<BShopContextView> requireShopContexts(Long staffId)
    {
        List<BShopContextView> shops = staffShopMapper.selectShopContextsByStaffId(staffId);
        if (shops == null || shops.isEmpty())
            throw new ServiceException("员工尚未授权门店", HttpStatus.FORBIDDEN);
        findDefaultShop(shops);
        return shops;
    }

    private BShopContextView findDefaultShop(List<BShopContextView> shops)
    {
        return shops.stream().filter(item -> Integer.valueOf(1).equals(item.getIsDefault())).findFirst()
                .orElseThrow(() -> new ServiceException("员工未配置默认门店", HttpStatus.FORBIDDEN));
    }

    private ShopStaff requireActiveStaff(ShopStaff staff)
    {
        if (staff == null) throw new ServiceException("员工账号不存在", HttpStatus.UNAUTHORIZED);
        if (!Integer.valueOf(0).equals(staff.getStatus()))
            throw new ServiceException("员工账号已停用", HttpStatus.FORBIDDEN);
        return staff;
    }

    private void recordSmsFailure(String phone, String failureKey)
    {
        long count = redisCache.increment(failureKey, 1);
        if (count == 1) redisCache.expire(failureKey, 5, TimeUnit.MINUTES);
        if (count >= SMS_MAX_VERIFY_FAILURES) redisCache.deleteObject(smsCodeKey(phone));
        log.warn("员工短信登录失败 phone={} failureCount={}", maskPhone(phone), count);
    }

    private void checkDailyLimit(String key, int limit, String message)
    {
        long count = redisCache.increment(key, 1);
        if (count == 1) redisCache.expire(key, 2, TimeUnit.DAYS);
        if (count > limit) throw new ServiceException(message, HttpStatus.BAD_REQUEST);
    }

    private String smsCodeKey(String phone) { return "staff:auth:sms:code:" + phone; }
    private String smsFailureKey(String phone) { return "staff:auth:sms:verify-fail:" + phone; }
    private String pwdFailureKey(String account) { return "staff:auth:pwd:fail:" + account; }

    private void recordPwdFailure(String account, String failureKey)
    {
        long count = redisCache.increment(failureKey, 1);
        if (count == 1) redisCache.expire(failureKey, 5, TimeUnit.MINUTES);
        if (count >= PASSWORD_MAX_FAILURES)
            log.warn("员工密码登录锁定 account={} failureCount={}", account, count);
    }
    private String maskPhone(String phone)
    {
        return phone == null || phone.length() < 7 ? "***"
                : phone.substring(0, 3) + "****" + phone.substring(phone.length() - 4);
    }
}
