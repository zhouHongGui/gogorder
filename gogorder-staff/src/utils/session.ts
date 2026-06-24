import type { LoginResult, ShopContext, StaffProfile } from '../types/auth'

const TOKEN_KEY = 'gogorder_staff_token'
const USER_KEY = 'gogorder_staff_user'
const SHOPS_KEY = 'gogorder_staff_shops'
const CURRENT_SHOP_KEY = 'gogorder_staff_current_shop'

export const getToken = (): string => uni.getStorageSync<string>(TOKEN_KEY) || ''
export const getUser = (): StaffProfile | null => uni.getStorageSync<StaffProfile>(USER_KEY) || null
export const getShops = (): ShopContext[] => uni.getStorageSync<ShopContext[]>(SHOPS_KEY) || []
export const getCurrentShop = (): ShopContext | null => uni.getStorageSync<ShopContext>(CURRENT_SHOP_KEY) || null

export function saveSession(data: LoginResult): void {
  uni.setStorageSync(TOKEN_KEY, data.token)
  uni.setStorageSync(USER_KEY, data.userInfo)
  uni.setStorageSync(SHOPS_KEY, data.shops)
  uni.setStorageSync(CURRENT_SHOP_KEY, data.currentShop)
}

export function saveShopContext(shops: ShopContext[], currentShop: ShopContext): void {
  uni.setStorageSync(SHOPS_KEY, shops)
  uni.setStorageSync(CURRENT_SHOP_KEY, currentShop)
}

export function clearSession(): void {
  uni.removeStorageSync(TOKEN_KEY)
  uni.removeStorageSync(USER_KEY)
  uni.removeStorageSync(SHOPS_KEY)
  uni.removeStorageSync(CURRENT_SHOP_KEY)
}
