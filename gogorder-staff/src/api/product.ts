import { request } from '../utils/request'
import type { StaffCategory, StaffProductPage, StaffProductQuery } from '../types/product'

function cleanQuery<T extends object>(query: T): Partial<T> {
  const result: Partial<T> = {}
  Object.entries(query as Record<string, unknown>).forEach(([key, value]) => {
    if (value !== undefined && value !== null && value !== '') {
      result[key as keyof T] = value as T[keyof T]
    }
  })
  return result
}

export const getProductCategories = () => request<StaffCategory[]>({
  url: '/api/b/product/categories',
  method: 'GET'
})

export const getProductList = (query: StaffProductQuery) => request<StaffProductPage>({
  url: '/api/b/product/list',
  method: 'GET',
  data: cleanQuery(query),
  page: true
})

export const updateProductStatus = (shopProductId: number, status: number) => request<void>({
  url: `/api/b/product/${shopProductId}/status`,
  method: 'PUT',
  data: { status }
})

export const setProductSoldOut = (shopProductId: number, soldOut: boolean, reason: string) => request<void>({
  url: `/api/b/product/${shopProductId}/sold-out`,
  method: 'PUT',
  data: {
    soldOut,
    reason,
    requestId: `staff-soldout-${shopProductId}-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
  }
})
