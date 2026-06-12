import { request } from '../utils/request'
import type { Category, Product, ProductQuery } from '../types/product'

export const getCategories = (shopId: number) => request<Category[]>({
  url: '/api/c/category/list',
  method: 'GET',
  data: { shopId }
})

export const getProducts = (params: ProductQuery) => request<Product[]>({
  url: '/api/c/product/list',
  method: 'GET',
  data: params
})

export const getProductDetail = (shopId: number, productId: number) => request<Product>({
  url: `/api/c/product/${productId}`,
  method: 'GET',
  data: { shopId }
})
