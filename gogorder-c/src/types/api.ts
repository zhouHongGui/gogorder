/**
 * API 通用类型：统一响应外壳与请求选项。
 */

/** 后端统一响应外壳：code=200 成功，data 承载业务数据。 */
export interface ApiResponse<T> {
  code: number
  msg?: string
  data?: T
}

/** 请求选项：在 uni.request 选项基础上扩展封装专用选项（见 utils/request.ts）。 */
export type ApiRequestOptions = Omit<UniApp.RequestOptions, 'success' | 'fail'> & {
  /** 是否带 Token（默认 true）。 */
  auth?: boolean
  /** 401 是否自动清会话跳登录（默认 true）。 */
  redirectOnUnauthorized?: boolean
  /** 非 200 是否自动弹 toast（默认 true）。 */
  showErrorToast?: boolean
}
