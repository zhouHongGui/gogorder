export interface StaffCategory {
  id: number
  name: string
  sortOrder?: number
  status?: number
}

export type StockState = 'ZERO'

export interface StaffProductQuery {
  keyword?: string
  categoryId?: number
  status?: number
  stockState?: StockState
  pageNum?: number
  pageSize?: number
}

export interface StaffProduct {
  shopProductId: number
  shopId: number
  productId: number
  productName: string
  productImage?: string
  categoryNames?: string
  basePrice: number
  price?: number
  effectivePrice: number
  status: number
  statusDesc: string
  stockDesc: string
  soldOut: boolean
  sortOrder?: number
  todaySales: number
  monthlySales: number
  createTime?: string
}

export interface StaffProductPage {
  rows: StaffProduct[]
  total: number
}
