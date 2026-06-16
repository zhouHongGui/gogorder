/**
 * 认证与用户 API（登录/微信绑定/用户信息/余额）。
 * 登录类接口为公开路径；用户信息/余额接口需登录。
 */
import { request } from '../utils/request'
import type { LoginResult, SendSmsResult, UserBalance, UserInfo, UserUpdateRequest, WechatLoginResult } from '../types/auth'

/** 发送短信验证码（返回 expiresIn/retryAfter；mock 模式额外返回 mockCode）。 */
export const sendSms = (phone: string) => request<SendSmsResult>({
  url: '/api/c/auth/send-sms',
  method: 'POST',
  data: { phone }
})

/** 短信验证码登录（返回 token + userInfo）。 */
export const loginBySms = (phone: string, code: string) => request<LoginResult>({
  url: '/api/c/auth/login-by-sms',
  method: 'POST',
  data: { phone, code }
})

/** 微信小程序登录第一步（login code 换 openid）。已绑定返回 token；未绑定返回 bindTicket。 */
export const loginByWechat = (code: string) => request<WechatLoginResult>({
  url: '/api/c/auth/login-by-wechat',
  method: 'POST',
  data: { code }
})

/** 微信绑定手机号（bindTicket + phone code），完成绑定并返回 token。 */
export const bindWechatPhone = (bindTicket: string, phoneCode: string) => request<LoginResult>({
  url: '/api/c/auth/bind-phone',
  method: 'POST',
  data: { bindTicket, phoneCode }
})

/** 获取当前用户信息。 */
export const getUserInfo = () => request<UserInfo>({
  url: '/api/c/user/info',
  method: 'GET'
})

/** 获取当前用户余额。 */
export const getUserBalance = () => request<UserBalance>({
  url: '/api/c/user/balance',
  method: 'GET'
})

/** 修改用户资料（昵称/头像）。 */
export const updateUserInfo = (data: UserUpdateRequest) => request<UserInfo>({
  url: '/api/c/user/update',
  method: 'PUT',
  data
})
