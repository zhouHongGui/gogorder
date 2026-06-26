/**
 * 统一 HTTP 请求封装（基于 uni.request）。
 * 负责：拼接 baseUrl、注入 Bearer Token、统一处理响应码（401 跳登录、非 200 报错）、网络失败提示。
 * 所有 api/*.ts 都应通过此函数发请求，以获得统一的鉴权与错误处理。
 */
import { clearSession, getToken } from './auth'
import type { ApiRequestOptions, ApiResponse } from '../types/api'

/** API 基础地址。H5 开发默认走 Vite 代理；小程序/真机使用局域网或生产绝对地址。 */
let baseUrl = import.meta.env.VITE_API_BASE_URL || ''
// #ifdef H5
baseUrl = import.meta.env.VITE_H5_API_BASE_URL ?? ''
// #endif

/**
 * API 业务异常。携带后端返回的 code 与 data（如余额不足时的 {balance, required}），
 * 供调用方做精细化处理（如 confirm.vue 提取 balance/required 展示）。
 */
export class ApiRequestError<T = unknown> extends Error {
  /** 业务错误码（后端 code 或 HTTP 状态码）。 */
  code: number
  /** 后端返回的 data 字段（可为 undefined）。 */
  data?: T

  constructor(code: number, message: string, data?: T) {
    super(message)
    this.name = 'ApiRequestError'
    this.code = code
    this.data = data
  }
}

/**
 * 发起 API 请求并解析响应。
 *
 * @param options 请求配置，额外支持：
 *   - auth：是否带 Token（默认 true，登录/发短信等接口设 false）
 *   - redirectOnUnauthorized：401 是否自动清会话并跳登录（默认 true）
 *   - showErrorToast：非 200 是否自动弹 toast（默认 true；submit/pay 等需自定义错误处理的设 false）
 * @returns 解析后的 data 字段（已脱去 {code,msg,data} 外壳）
 */
export function request<T>(options: ApiRequestOptions): Promise<T> {
  return new Promise<T>((resolve, reject) => {
    const token = getToken()
    // 解构出本封装专用的选项，剩余的透传给 uni.request。
    const {
      auth = true,
      redirectOnUnauthorized = true,
      showErrorToast = true,
      ...requestOptions
    } = options
    uni.request({
      ...requestOptions,
      url: baseUrl + requestOptions.url,
      header: {
        'Content-Type': 'application/json',
        // 需鉴权且本地有 token 时注入 Bearer 头。
        ...(auth && token ? { Authorization: `Bearer ${token}` } : {}),
        ...(requestOptions.header || {})
      },
      success(response) {
        const body = response.data as ApiResponse<T>
        // 401（HTTP 层或业务层）：未登录/token 失效，清会话并跳登录页。
        if (response.statusCode === 401 || body.code === 401) {
          if (redirectOnUnauthorized) {
            clearSession()
            uni.reLaunch({ url: '/pages/login/index' })
          }
          reject(new ApiRequestError(body.code || response.statusCode, body.msg || '请先登录', body.data))
          return
        }
        // 非 200：业务错误。默认弹 toast；携带 data 供调用方细查。
        if (body.code !== 200) {
          if (showErrorToast) uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(new ApiRequestError(body.code, body.msg || '请求失败', body.data))
          return
        }
        // 成功：剥掉外壳返回 data。
        resolve(body.data as T)
      },
      fail(error) {
        // 网络层失败（断网/超时等）。
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(error)
      }
    })
  })
}
