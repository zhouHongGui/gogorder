import { request } from '../utils/request'

export const sendSms = phone => request({
  url: '/api/c/auth/send-sms',
  method: 'POST',
  data: { phone }
})

export const loginBySms = (phone, code) => request({
  url: '/api/c/auth/login-by-sms',
  method: 'POST',
  data: { phone, code }
})

export const loginByWechat = code => request({
  url: '/api/c/auth/login-by-wechat',
  method: 'POST',
  data: { code }
})

export const bindWechatPhone = (bindTicket, phoneCode) => request({
  url: '/api/c/auth/bind-phone',
  method: 'POST',
  data: { bindTicket, phoneCode }
})

export const getUserInfo = () => request({
  url: '/api/c/user/info',
  method: 'GET'
})

export const updateUserInfo = data => request({
  url: '/api/c/user/update',
  method: 'PUT',
  data
})
