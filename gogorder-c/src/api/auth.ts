import { request } from '../utils/request'
import type { LoginResult, SendSmsResult, UserInfo, UserUpdateRequest, WechatLoginResult } from '../types/auth'

export const sendSms = (phone: string) => request<SendSmsResult>({
  url: '/api/c/auth/send-sms',
  method: 'POST',
  data: { phone }
})

export const loginBySms = (phone: string, code: string) => request<LoginResult>({
  url: '/api/c/auth/login-by-sms',
  method: 'POST',
  data: { phone, code }
})

export const loginByWechat = (code: string) => request<WechatLoginResult>({
  url: '/api/c/auth/login-by-wechat',
  method: 'POST',
  data: { code }
})

export const bindWechatPhone = (bindTicket: string, phoneCode: string) => request<LoginResult>({
  url: '/api/c/auth/bind-phone',
  method: 'POST',
  data: { bindTicket, phoneCode }
})

export const getUserInfo = () => request<UserInfo>({
  url: '/api/c/user/info',
  method: 'GET'
})

export const updateUserInfo = (data: UserUpdateRequest) => request<UserInfo>({
  url: '/api/c/user/update',
  method: 'PUT',
  data
})
