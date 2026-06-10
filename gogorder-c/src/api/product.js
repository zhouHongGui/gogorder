import { request } from '../utils/request'

export const getCategories = shopId => request({
  url: '/api/c/category/list',
  method: 'GET',
  data: { shopId }
})

export const getProducts = params => request({
  url: '/api/c/product/list',
  method: 'GET',
  data: params
})

export const getProductDetail = (shopId, productId) => request({
  url: `/api/c/product/${productId}`,
  method: 'GET',
  data: { shopId }
})
