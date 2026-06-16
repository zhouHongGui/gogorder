/**
 * 认证相关类型：用户信息、登录结果、短信发送结果。
 */

/** C 端用户信息。status：1=正常 0=禁用。 */
export interface UserInfo {
  id: number
  phone: string
  nickname: string
  avatar: string
  status: number
  createTime?: string
  updateTime?: string
}

/** 修改用户资料请求（仅昵称/头像）。 */
export interface UserUpdateRequest {
  nickname: string
  avatar: string
}

/** 用户余额查询结果（balance 单位：分）。 */
export interface UserBalance {
  balance: number
}

/** 登录成功结果：token + 用户信息。bound 标记微信是否已绑定（短信登录恒为 true）。 */
export interface LoginResult {
  token: string
  userInfo: UserInfo
  bound?: boolean
}

/** 微信登录结果：已绑定含 token/userInfo；未绑定含 bindTicket 供绑定手机号用。 */
export interface WechatLoginResult extends Partial<LoginResult> {
  bound: boolean
  bindTicket?: string
}

/** 发送短信验证码结果：expiresIn 有效期秒、retryAfter 重发间隔秒；mock 模式含 mockCode。 */
export interface SendSmsResult {
  expiresIn: number
  retryAfter: number
  mockCode?: string
}
