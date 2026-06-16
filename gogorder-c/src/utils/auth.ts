/**
 * C 端登录会话本地存储（基于 uni 本地缓存，H5/小程序通用）。
 * 登录态 = Token + 用户信息，存在本地；登出/401 时清空。
 */
import type { LoginResult, UserInfo } from '../types/auth'

/** Token 本地存储 key。 */
const TOKEN_KEY = 'gogorder_c_token'
/** 用户信息本地存储 key。 */
const USER_KEY = 'gogorder_c_user'

/** 读取本地 Token（无则返回空串）。 */
export function getToken(): string {
  return uni.getStorageSync<string>(TOKEN_KEY) || ''
}

/** 读取本地用户信息（无则返回 null）。 */
export function getUser(): UserInfo | null {
  return uni.getStorageSync<UserInfo>(USER_KEY) || null
}

/** 登录成功后保存会话（Token + 用户信息）。 */
export function saveSession(data: LoginResult): void {
  uni.setStorageSync(TOKEN_KEY, data.token)
  uni.setStorageSync(USER_KEY, data.userInfo)
}

/** 仅更新本地用户信息（修改资料后调用）。 */
export function saveUser(user: UserInfo): void {
  uni.setStorageSync(USER_KEY, user)
}

/** 清空会话（登出/401 时调用）。 */
export function clearSession(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
