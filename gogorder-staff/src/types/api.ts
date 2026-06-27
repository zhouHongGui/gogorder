export interface ApiResponse<T> {
  code: number
  msg?: string
  data?: T
  rows?: unknown[]
  total?: number
}

export type ApiRequestOptions = Omit<UniApp.RequestOptions, 'success' | 'fail'> & {
  auth?: boolean
  shop?: boolean
  redirectOnUnauthorized?: boolean
  showErrorToast?: boolean
  page?: boolean
}
