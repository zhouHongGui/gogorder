export interface Category {
  id: number
  name: string
  sortOrder?: number
  status?: number
}

export interface MenuCategory {
  id: number | null
  name: string
}

export interface SpecOption {
  id: number
  optionId: string
  templateId: number
  label: string
  priceAdd: number
  isDefault: number
  sortOrder: number
  status: number
}

export interface ProductSpec {
  templateId: number
  name: string
  type: 1 | 2
  required: boolean
  minSelect: number
  maxSelect: number
  options: SpecOption[]
}

export interface Product {
  shopProductId: number
  productId: number
  name: string
  image?: string
  description?: string
  price: number
  displayPrice: number
  stock: number
  soldOut: boolean
  hasSpecs: boolean
  monthlySales: number
  tags: string[]
  categories: Category[]
  specs: ProductSpec[]
}

export interface ProductQuery {
  shopId: number
  categoryId?: number | null
  keyword?: string
}

export interface PopularProductCard {
  name: string
  description: string
  price: string
  badge: string
  color: string
  background: string
  productId?: number
}

export type SpecSelection = string | string[]
export type SpecSelections = Record<number, SpecSelection>
