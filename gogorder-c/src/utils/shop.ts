/**
 * 门店相关本地存储与展示工具。
 * 维护「当前选中门店」与「上次定位」的本地缓存，并提供距离/营业时间的格式化。
 */
import type { Coordinates, Shop } from '../types/shop'

/** 当前选中门店的本地存储 key。 */
const CURRENT_SHOP_KEY = 'gogorder_current_shop'
/** 上次定位的本地存储 key。 */
const LOCATION_KEY = 'gogorder_last_location'

/** 读取当前选中门店（无则 null）。 */
export function getCurrentShop(): Shop | null {
  return uni.getStorageSync<Shop>(CURRENT_SHOP_KEY) || null
}

/** 保存当前选中门店（选店后调用，全局共享）。 */
export function saveCurrentShop(shop: Shop): void {
  uni.setStorageSync(CURRENT_SHOP_KEY, shop)
}

/** 清除当前选中门店。 */
export function clearCurrentShop(): void {
  uni.removeStorageSync(CURRENT_SHOP_KEY)
}

/** 读取上次定位（无则 null）。 */
export function getLastLocation(): Coordinates | null {
  return uni.getStorageSync<Coordinates>(LOCATION_KEY) || null
}

/** 保存上次定位（定位成功后缓存，减少重复定位）。 */
export function saveLastLocation(location: Coordinates): void {
  uni.setStorageSync(LOCATION_KEY, location)
}

/** 格式化距离（米）：<1km 显示 m，否则显示 km（<10km 保留 1 位小数）。 */
export function formatDistance(distance: number | null | undefined): string {
  if (distance === null || distance === undefined) return '--'
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(distance < 10000 ? 1 : 0)}km`
}

/** 格式化营业时间区间（如「08:00 - 22:00」），缺失则「--」。 */
export function businessHours(shop: Pick<Shop, 'openTime' | 'closeTime'> | null | undefined): string {
  if (!shop?.openTime || !shop?.closeTime) return '--'
  return `${shop.openTime} - ${shop.closeTime}`
}
