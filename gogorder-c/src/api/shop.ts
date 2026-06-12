import { request } from '../utils/request'
import type { AmapConfig, NearbyShopParams, PreorderSlot, Shop } from '../types/shop'

export const getShopMapConfig = () => request<AmapConfig>({
  url: '/api/c/shop/map-config',
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})

export const getNearbyShops = (params: NearbyShopParams) => request<Shop[]>({
  url: '/api/c/shop/nearby',
  method: 'GET',
  data: params,
  auth: false,
  redirectOnUnauthorized: false
})

export const getShopDetail = (id: number) => request<Shop>({
  url: `/api/c/shop/${id}`,
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})

export const getPreorderSlots = (id: number) => request<PreorderSlot[]>({
  url: `/api/c/shop/${id}/preorder-slots`,
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})
