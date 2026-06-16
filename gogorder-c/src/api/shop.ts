/**
 * 门店 API（{@code /api/c/shop/**}，公开路径，无需登录）。
 * 注意：这些接口在登录前使用，故 auth:false（不带 Token）、redirectOnUnauthorized:false（401 不跳登录）。
 */
import { request } from '../utils/request'
import type { AmapConfig, NearbyShopParams, PreorderSlot, Shop } from '../types/shop'

/** 获取高德地图配置（key + 安全密钥），供前端初始化地图。 */
export const getShopMapConfig = () => request<AmapConfig>({
  url: '/api/c/shop/map-config',
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})

/** 查询附近门店列表（按经纬度，可按关键字筛选）。 */
export const getNearbyShops = (params: NearbyShopParams) => request<Shop[]>({
  url: '/api/c/shop/nearby',
  method: 'GET',
  data: params,
  auth: false,
  redirectOnUnauthorized: false
})

/** 查询门店详情（含营业时间、预约参数，供菜单页/下单页）。 */
export const getShopDetail = (id: number) => request<Shop>({
  url: `/api/c/shop/${id}`,
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})

/** 查询门店可选取餐时段（仅预订单用，每 30 分钟一档）。 */
export const getPreorderSlots = (id: number) => request<PreorderSlot[]>({
  url: `/api/c/shop/${id}/preorder-slots`,
  method: 'GET',
  auth: false,
  redirectOnUnauthorized: false
})
