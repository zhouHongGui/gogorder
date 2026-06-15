export interface UserInfo {
  id: number
  phone: string
  nickname: string
  avatar: string
  status: number
  createTime?: string
  updateTime?: string
}

export interface UserUpdateRequest {
  nickname: string
  avatar: string
}

export interface UserBalance {
  balance: number
}

export interface LoginResult {
  token: string
  userInfo: UserInfo
  bound?: boolean
}

export interface WechatLoginResult extends Partial<LoginResult> {
  bound: boolean
  bindTicket?: string
}

export interface SendSmsResult {
  expiresIn: number
  retryAfter: number
  mockCode?: string
}
