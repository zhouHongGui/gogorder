import { request } from '../utils/request'

export const getNearbyShops = params => request({
  url: '/api/c/shop/nearby',
  method: 'GET',
  data: params
})

export const getShopDetail = id => request({
  url: `/api/c/shop/${id}`,
  method: 'GET'
})

export const getPreorderSlots = id => request({
  url: `/api/c/shop/${id}/preorder-slots`,
  method: 'GET'
})
