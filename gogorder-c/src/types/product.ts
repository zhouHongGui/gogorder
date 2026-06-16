/**
 * 商品与规格相关类型。金额单位：分；stock=-1 无限库存；type 1=单选 2=多选。
 */

/** 商品分类。 */
export interface Category {
  id: number
  name: string
  sortOrder?: number
  status?: number
}

/** 菜单分类（含「全部」占位，id=null）。 */
export interface MenuCategory {
  id: number | null
  name: string
}

/** 规格选项。optionId 全局唯一不可变；isDefault=1 默认选中。 */
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

/** 规格模板视图（商品详情返回，供规格选择 UI）。 */
export interface ProductSpec {
  templateId: number
  name: string
  type: 1 | 2
  required: boolean
  minSelect: number
  maxSelect: number
  options: SpecOption[]
}

/** 商品视图（菜单/详情）。displayPrice=起售价（含必选规格最小加价）。 */
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

/** 商品列表查询参数。 */
export interface ProductQuery {
  shopId: number
  categoryId?: number | null
  keyword?: string
}

/** 首页热门商品卡片（展示用，price 为已格式化字符串）。 */
export interface PopularProductCard {
  name: string
  description: string
  price: string
  badge: string
  color: string
  background: string
  productId?: number
}

/** 单个规格模板的选择：单选为 optionId 字符串，多选为数组。 */
export type SpecSelection = string | string[]
/** 所有规格模板的选择集合 {templateId → selection}。 */
export type SpecSelections = Record<number, SpecSelection>
