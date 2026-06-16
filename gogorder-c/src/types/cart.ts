/**
 * 购物车相关类型。金额单位：分；stock=-1 表示无限库存。
 */

/** 购物车条目里选中的单个规格选项（展示用）。 */
export interface CartSpecOption {
  templateId: number
  templateName: string
  optionId: string
  label: string
  priceAdd: number
}

/** 购物车条目。cartItemId 决定同商品同规格是否合并。 */
export interface CartItem {
  cartItemId: string
  shopProductId: number
  productId: number
  productName: string
  image?: string
  specs: Record<string, string[]>
  selectedSpecs: CartSpecOption[]
  specText: string
  unitPrice: number
  quantity: number
  amount: number
  stock: number
}

/** 购物车视图（查询/增删改返回）。 */
export interface Cart {
  shopId: number | null
  shopName: string
  items: CartItem[]
  totalAmount: number
  totalCount: number
}

/** 加购请求。specs：key=templateId，value=单个 optionId 或数组。 */
export interface AddCartItemRequest {
  shopId: number
  productId: number
  specs: Record<string, string | string[]>
  quantity: number
}

/** 修改数量请求（quantity=0 表示删除）。 */
export interface UpdateCartItemRequest {
  shopId: number
  cartItemId: string
  quantity: number
}
