import type { ApiRequestOptions, ApiResponse } from '../types/api'
import { clearSession, getCurrentShop, getToken } from './session'

const baseUrl = import.meta.env.VITE_API_BASE_URL || ''

export class ApiRequestError<T = unknown> extends Error {
  code: number
  data?: T

  constructor(code: number, message: string, data?: T) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.data = data
  }
}

export function request<T>(options: ApiRequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const {
      auth = true,
      shop = true,
      redirectOnUnauthorized = true,
      showErrorToast = true,
      ...requestOptions
    } = options
    const token = getToken()
    const currentShop = getCurrentShop()
    uni.request({
      ...requestOptions,
      url: baseUrl + requestOptions.url,
      header: {
        'Content-Type': 'application/json',
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...(auth && shop && currentShop ? { 'X-Shop-Id': String(currentShop.shopId) } : {}),
        ...(requestOptions.header || {})
      },
      success(response) {
        const body = response.data as ApiResponse<T>
        if (response.statusCode === 401 || body?.code === 401) {
          if (redirectOnUnauthorized) {
            clearSession()
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(new ApiRequestError(body?.code || response.statusCode, body?.msg || '请先登录', body?.data))
          return
        }
        if (!body || body.code !== 200) {
          const message = body?.msg || '请求失败'
          if (showErrorToast) uni.showToast({ title: message, icon: 'none' })
          reject(new ApiRequestError(body?.code || response.statusCode, message, body?.data))
          return
        }
        resolve(body.data as T)
      },
      fail(error) {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(error)
      }
    })
  })
}
