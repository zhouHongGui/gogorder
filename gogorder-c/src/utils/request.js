import { clearSession, getToken } from './auth'

const baseUrl = import.meta.env.VITE_API_BASE_URL || ''

export function request(options) {
  return new Promise((resolve, reject) => {
    const token = getToken()
    uni.request({
      ...options,
      url: baseUrl + options.url,
      header: {
        'Content-Type': 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...(options.header || {})
      },
      success(response) {
        const body = response.data || {}
        if (response.statusCode === 401 || body.code === 401) {
          clearSession()
          uni.reLaunch({ url: '/pages/login/index' })
          reject(new Error(body.msg || '请先登录'))
          return
        }
        if (body.code !== 200) {
          uni.showToast({ title: body.msg || '请求失败', icon: 'none' })
          reject(new Error(body.msg || '请求失败'))
          return
        }
        resolve(body.data)
      },
      fail(error) {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(error)
      }
    })
  })
}
