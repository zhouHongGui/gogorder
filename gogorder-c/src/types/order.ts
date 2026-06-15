import type { OrderType } from './shop'

export interface OrderItemRequest {
  productId: number
  specs: Record<string, string[]>
  quantity: number
}

export interface OrderSubmitRequest {
  shopId: number
  submitToken: string
  orderType: OrderType
  scheduledPickupTime?: string
  remark?: string
  items: OrderItemRequest[]
}

export interface OrderSubmitResponse {
  orderId: number
  orderNo: string
  totalAmount: number
  balance: number
  createTime: string
}

export interface PayResponse {
  orderNo: string
  pickupDisplay: string
  balanceAfter: number
}

export interface OrderItemView {
  productId: number
  productName: string
  productImage?: string
  specText: string
  specs: unknown[]
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface OrderDetail {
  orderId: number
  orderNo: string
  shopId: number
  shopName: string
  shopPhone?: string
  reservedPhone?: string
  orderType: OrderType
  orderTypeDesc: string
  scheduledPickupTime?: string
  orderStatus: number
  orderStatusDesc: string
  payStatus: number
  payStatusDesc: string
  refundStatus: number
  refundStatusDesc: string
  productAmount: number
  packFee: number
  totalAmount: number
  remark?: string
  pickupDisplay?: string
  pickupToken?: string
  pickupDate?: string
  payTime?: string
  acceptTime?: string
  makeStartTime?: string
  completeMakeTime?: string
  verifyTime?: string
  cancelTime?: string
  cancelReason?: string
  createTime: string
  items: OrderItemView[]
}

export interface OrderListItem {
  orderId: number
  orderNo: string
  shopId: number
  shopName: string
  orderType: OrderType
  orderTypeDesc: string
  scheduledPickupTime?: string
  orderStatus: number
  orderStatusDesc: string
  payStatus: number
  payStatusDesc: string
  refundStatus: number
  refundStatusDesc: string
  totalAmount: number
  itemCount: number
  pickupDisplay?: string
  createTime: string
  items: OrderItemView[]
}

export interface OrderPage {
  rows: OrderListItem[]
  total: number
  pageNum: number
  pageSize: number
  hasMore: boolean
}

export interface OrderListParams {
  dateScope: 'TODAY' | 'HISTORY'
  pageNum: number
  pageSize: number
}
