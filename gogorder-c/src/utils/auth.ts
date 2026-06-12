import type { LoginResult, UserInfo } from '../types/auth'

const TOKEN_KEY = 'gogorder_c_token'
const USER_KEY = 'gogorder_c_user'

export function getToken(): string {
  return uni.getStorageSync<string>(TOKEN_KEY) || ''
}

export function getUser(): UserInfo | null {
  return uni.getStorageSync<UserInfo>(USER_KEY) || null
}

export function saveSession(data: LoginResult): void {
  uni.setStorageSync(TOKEN_KEY, data.token)
  uni.setStorageSync(USER_KEY, data.userInfo)
}

export function saveUser(user: UserInfo): void {
  uni.setStorageSync(USER_KEY, user)
}

export function clearSession(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
