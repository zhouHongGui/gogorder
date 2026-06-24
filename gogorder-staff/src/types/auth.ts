export interface StaffProfile {
  staffId: number
  account: string
  nickname: string
  phone: string
}

export interface ShopContext {
  shopId: number
  shopCode: string
  shopName: string
  address: string
  status: number
  isDefault: number
}

export interface LoginResult {
  token: string
  userInfo: StaffProfile
  shops: ShopContext[]
  currentShop: ShopContext
}

export interface SendSmsResult {
  expiresIn: number
  retryAfter: number
  mockCode?: string
}

export interface ShopMineResult {
  shops: ShopContext[]
  currentShop: ShopContext
}
