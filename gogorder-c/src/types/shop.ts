/**
 * 门店与定位相关类型。
 * ShopStatus：0休息 1营业 2暂停即时（仅可预订单）；
 * 营业时间支持跨午夜（openTime > closeTime 表示跨日）。
 */

/** 门店状态：0休息 1营业 2暂停即时。 */
export type ShopStatus = 0 | 1 | 2
/** 订单类型：NORMAL=即时单 / PREORDER=预订单。 */
export type OrderType = 'NORMAL' | 'PREORDER'

/** 经纬度坐标。 */
export interface Coordinates {
  longitude: number
  latitude: number
}

/** 高德地图配置（key + 安全密钥）。 */
export interface AmapConfig {
  key: string
  securityCode: string
}

/** 门店视图。distance=距调用点距离(米)；instantAvailable=可下即时单；preorderAvailable=可下预订单。 */
export interface Shop {
  id: number
  name: string
  image?: string
  phone?: string
  address: string
  longitude?: number
  latitude?: number
  openTime?: string
  closeTime?: string
  status: ShopStatus
  statusName: string
  notice?: string
  packFee?: number
  preorderMinMinutes?: number
  preorderMaxDays?: number
  makeLeadMinutes?: number
  distance: number | null
  isOpen: boolean
  instantAvailable: boolean
  preorderAvailable: boolean
}

/** 附近门店查询参数（经纬度可选，可带关键字）。 */
export interface NearbyShopParams extends Partial<Coordinates> {
  keyword?: string
}

/** 预订单可选取餐时段。value 为下单回传值，dateLabel/timeLabel 为展示文案。 */
export interface PreorderSlot {
  value: string
  dateLabel: string
  timeLabel: string
}

/** 首页门店卡片视图（含已格式化的距离/营业时间文案）。 */
export interface HomeShopView {
  id: number | null
  name: string
  address: string
  distanceText: string
  businessHours: string
  statusName: string
  instantAvailable: boolean
  notice: string
}
