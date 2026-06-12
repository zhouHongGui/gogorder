import type { Coordinates, Shop } from '../types/shop'

const CURRENT_SHOP_KEY = 'gogorder_current_shop'
const LOCATION_KEY = 'gogorder_last_location'

export function getCurrentShop(): Shop | null {
  return uni.getStorageSync<Shop>(CURRENT_SHOP_KEY) || null
}

export function saveCurrentShop(shop: Shop): void {
  uni.setStorageSync(CURRENT_SHOP_KEY, shop)
}

export function clearCurrentShop(): void {
  uni.removeStorageSync(CURRENT_SHOP_KEY)
}

export function getLastLocation(): Coordinates | null {
  return uni.getStorageSync<Coordinates>(LOCATION_KEY) || null
}

export function saveLastLocation(location: Coordinates): void {
  uni.setStorageSync(LOCATION_KEY, location)
}

export function formatDistance(distance: number | null | undefined): string {
  if (distance === null || distance === undefined) return '--'
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(distance < 10000 ? 1 : 0)}km`
}

export function businessHours(shop: Pick<Shop, 'openTime' | 'closeTime'> | null | undefined): string {
  if (!shop?.openTime || !shop?.closeTime) return '--'
  return `${shop.openTime} - ${shop.closeTime}`
}
