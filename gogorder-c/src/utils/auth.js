const TOKEN_KEY = 'gogorder_c_token'
const USER_KEY = 'gogorder_c_user'

export function getToken() {
  return uni.getStorageSync(TOKEN_KEY) || ''
}

export function getUser() {
  return uni.getStorageSync(USER_KEY) || null
}

export function saveSession(data) {
  uni.setStorageSync(TOKEN_KEY, data.token)
  uni.setStorageSync(USER_KEY, data.userInfo)
}

export function saveUser(user) {
  uni.setStorageSync(USER_KEY, user)
}

export function clearSession() {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
}
