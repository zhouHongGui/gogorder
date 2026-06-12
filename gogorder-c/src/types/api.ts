export interface ApiResponse<T> {
  code: number
  msg?: string
  data?: T
}

export type ApiRequestOptions = Omit<UniApp.RequestOptions, 'success' | 'fail'> & {
  auth?: boolean
  redirectOnUnauthorized?: boolean
}
