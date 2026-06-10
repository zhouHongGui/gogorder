const CURRENT_SHOP_KEY = 'gogorder_current_shop'
const LOCATION_KEY = 'gogorder_last_location'

export function getCurrentShop() {
  return uni.getStorageSync(CURRENT_SHOP_KEY) || null
}

export function saveCurrentShop(shop) {
  uni.setStorageSync(CURRENT_SHOP_KEY, shop)
}

export function clearCurrentShop() {
  uni.removeStorageSync(CURRENT_SHOP_KEY)
}

export function getLastLocation() {
  return uni.getStorageSync(LOCATION_KEY) || null
}

export function saveLastLocation(location) {
  uni.setStorageSync(LOCATION_KEY, location)
}

export function formatDistance(distance) {
  if (distance === null || distance === undefined) return '--'
  if (distance < 1000) return `${distance}m`
  return `${(distance / 1000).toFixed(distance < 10000 ? 1 : 0)}km`
}

export function businessHours(shop) {
  if (!shop?.openTime || !shop?.closeTime) return '--'
  return `${shop.openTime} - ${shop.closeTime}`
}
