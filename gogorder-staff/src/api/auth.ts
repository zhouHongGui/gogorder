import { request } from '../utils/request'
import type { LoginResult, SendSmsResult } from '../types/auth'

export const sendSms = (phone: string) => request<SendSmsResult>({
  url: '/api/b/auth/send-sms',
  method: 'POST',
  data: { phone },
  auth: false,
  shop: false
})

export const loginBySms = (phone: string, code: string) => request<LoginResult>({
  url: '/api/b/auth/login/sms',
  method: 'POST',
  data: { phone, code },
  auth: false,
  shop: false
})

export const loginByPassword = (account: string, password: string) => request<LoginResult>({
  url: '/api/b/auth/login/password',
  method: 'POST',
  data: { account, password },
  auth: false,
  shop: false
})

export const logout = () => request<void>({
  url: '/logout',
  method: 'POST',
  shop: false,
  redirectOnUnauthorized: false,
  showErrorToast: false
})
