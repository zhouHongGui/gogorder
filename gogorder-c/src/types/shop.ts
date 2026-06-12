export type ShopStatus = 0 | 1 | 2
export type OrderType = 'NORMAL' | 'PREORDER'

export interface Coordinates {
  longitude: number
  latitude: number
}

export interface AmapConfig {
  key: string
  securityCode: string
}

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

export interface NearbyShopParams extends Partial<Coordinates> {
  keyword?: string
}

export interface PreorderSlot {
  value: string
  dateLabel: string
  timeLabel: string
}

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
