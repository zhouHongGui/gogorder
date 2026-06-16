import { getShopMapConfig } from '../api/shop'
import type { Coordinates } from '../types/shop'

export interface AmapMarker {
  on(event: string, handler: () => void): void
}

export interface AmapMap {
  add(markers: AmapMarker[]): void
  clearMap(): void
  destroy(): void
  setCenter(center: [number, number]): void
  setZoomAndCenter(zoom: number, center: [number, number]): void
}

interface AmapPosition {
  lng?: number
  lat?: number
  getLng?: () => number
  getLat?: () => number
}

interface AmapGeolocationResult {
  position?: AmapPosition
  message?: string
  info?: string
}

interface AmapGeolocation {
  getCurrentPosition(callback: (status: string, result: AmapGeolocationResult) => void): void
}

export interface AmapApi {
  Map: new (container: string | HTMLElement, options: Record<string, unknown>) => AmapMap
  Marker: new (options: Record<string, unknown>) => AmapMarker
  Geolocation: new (options: Record<string, unknown>) => AmapGeolocation
  plugin(plugin: string, callback: () => void): void
}

interface AmapWindow extends Window {
  AMap?: AmapApi
  _AMapSecurityConfig?: {
    securityJsCode: string
  }
}

let amapLoader: Promise<AmapApi> | undefined

/**
 * 按需加载高德地图 JS API（单例，避免重复注入 script）。
 * 从后端 {@code /api/c/shop/map-config} 取 key 与安全密钥，注入 script 标签加载 AMap。
 * 失败时清空 loader 以便下次重试。
 */
export async function loadAmap(): Promise<AmapApi> {
  const amapWindow = window as AmapWindow
  // 已加载直接返回（单例）。
  if (amapWindow.AMap) return amapWindow.AMap
  // 正在加载则复用同一 Promise（防并发重复加载）。
  if (amapLoader) return amapLoader

  // 从后端取地图配置（key/安全密钥）。
  const config = await getShopMapConfig()
  const key = String(config.key || '').trim()
  const securityCode = String(config.securityCode || '').trim()
  if (!key) throw new Error('请先在后端配置 AMAP_WEB_KEY')
  // 新版 JS API 必须设置安全密钥。
  if (securityCode) amapWindow._AMapSecurityConfig = { securityJsCode: securityCode }

  // 动态注入高德地图 script 标签。
  amapLoader = new Promise<AmapApi>((resolve, reject) => {
    const script = document.createElement('script')
    script.src = `https://webapi.amap.com/maps?v=2.0&key=${encodeURIComponent(key)}`
    script.async = true
    script.onload = () => {
      if (amapWindow.AMap) resolve(amapWindow.AMap)
      else reject(new Error('高德地图初始化失败'))
    }
    script.onerror = () => reject(new Error('高德地图加载失败'))
    document.head.appendChild(script)
  })
  // 加载失败清空 loader，允许后续重试。
  amapLoader.catch(() => {
    amapLoader = undefined
  })
  return amapLoader
}

/**
 * 获取当前定位（经纬度）。基于高德 Geolocation 插件，10 秒超时。
 * 失败时抛出包含 result.message 的错误。
 */
export async function getAmapLocation(): Promise<Coordinates> {
  const amap = await loadAmap()

  return new Promise<Coordinates>((resolve, reject) => {
    amap.plugin('AMap.Geolocation', () => {
      const geolocation = new amap.Geolocation({
        enableHighAccuracy: true,
        GeoLocationFirst: false,
        noIpLocate: 0,
        noGeoLocation: 0,
        getCityWhenFail: true,
        timeout: 10_000,
        showButton: false,
        showMarker: false,
        showCircle: false
      })
      geolocation.getCurrentPosition((status, result) => {
        const longitude = result.position?.lng ?? result.position?.getLng?.()
        const latitude = result.position?.lat ?? result.position?.getLat?.()
        if (status === 'complete' && Number.isFinite(longitude) && Number.isFinite(latitude)) {
          resolve({ longitude: Number(longitude), latitude: Number(latitude) })
          return
        }
        reject(new Error(result.message || result.info || '高德定位失败'))
      })
    })
  })
}
