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

export async function loadAmap(): Promise<AmapApi> {
  const amapWindow = window as AmapWindow
  if (amapWindow.AMap) return amapWindow.AMap
  if (amapLoader) return amapLoader

  const config = await getShopMapConfig()
  const key = String(config.key || '').trim()
  const securityCode = String(config.securityCode || '').trim()
  if (!key) throw new Error('请先在后端配置 AMAP_WEB_KEY')
  if (securityCode) amapWindow._AMapSecurityConfig = { securityJsCode: securityCode }

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
  amapLoader.catch(() => {
    amapLoader = undefined
  })
  return amapLoader
}

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
