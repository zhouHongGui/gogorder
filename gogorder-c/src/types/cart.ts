export interface CartSpecOption {
  templateId: number
  templateName: string
  optionId: string
  label: string
  priceAdd: number
}

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

export interface Cart {
  shopId: number | null
  shopName: string
  items: CartItem[]
  totalAmount: number
  totalCount: number
}

export interface AddCartItemRequest {
  shopId: number
  productId: number
  specs: Record<string, string | string[]>
  quantity: number
}

export interface UpdateCartItemRequest {
  shopId: number
  cartItemId: string
  quantity: number
}
