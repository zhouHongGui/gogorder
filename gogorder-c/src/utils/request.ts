import { clearSession, getToken } from './auth'
import type { ApiRequestOptions, ApiResponse } from '../types/api'

const baseUrl = import.meta.env.VITE_API_BASE_URL || ''

export function request<T>(options: ApiRequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const token = getToken()
    const {
      auth = true,
      redirectOnUnauthorized = true,
      ...requestOptions
    } = options
    uni.request({
      ...requestOptions,
      url: baseUrl + requestOptions.url,
      header: {
        'Content-Type': 'application/json',
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...(requestOptions.header || {})
      },
      success(response) {
        const body = response.data as ApiResponse<T>
        if (response.statusCode === 401 || body.code === 401) {
          if (redirectOnUnauthorized) {
            clearSession()
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(new Error(body.msg || '请先登录'))
          return
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(new Error(body.msg || '请求失败'))
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
