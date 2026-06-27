export type OrderType = 'NORMAL' | 'PREORDER'

export interface OrderItem {
  productId: number
  productName: string
  productImage?: string
  specText: string
  unitPrice: number
  quantity: number
  subtotal: number
}

export interface StaffOrderCard {
  orderId: number
  orderNo: string
  orderType: OrderType
  orderTypeDesc: string
  scheduledPickupTime?: string
  orderStatus: number
  orderStatusDesc: string
  payStatus: number
  refundStatus: number
  totalAmount: number
  itemCount: number
  pickupDisplay?: string
  remark?: string
  createTime: string
  acceptTime?: string
  makeStartTime?: string
  completeMakeTime?: string
  estimatedReadyTime?: string
  makeTimeout?: boolean
  items: OrderItem[]
}

export interface StaffOrderListItem {
  orderId: number
  orderNo: string
  userPhoneMasked?: string
  orderType: OrderType
  orderTypeDesc: string
  scheduledPickupTime?: string
  orderStatus: number
  orderStatusDesc: string
  payStatus: number
  payStatusDesc?: string
  refundStatus: number
  refundStatusDesc?: string
  totalAmount: number
  itemCount: number
  pickupDisplay?: string
  pickupDate?: string
  remark?: string
  estimatedReadyTime?: string
  payTime?: string
  acceptTime?: string
  makeStartTime?: string
  completeMakeTime?: string
  verifyTime?: string
  cancelTime?: string
  createTime: string
}

export interface StaffOrderQuery {
  keyword?: string
  phone?: string
  orderStatus?: number
  payStatus?: number
  refundStatus?: number
  startTime?: string
  endTime?: string
  pageNum?: number
  pageSize?: number
}

export interface StaffOrderPage {
  rows: StaffOrderListItem[]
  total: number
}

export interface StaffOrderBoard {
  preorders: StaffOrderCard[]
  pending: StaffOrderCard[]
  making: StaffOrderCard[]
  waiting: StaffOrderCard[]
  preordersCount: number
  pendingCount: number
  makingCount: number
  waitingCount: number
  totalActiveCount: number
}

export interface StaffOrderDetail extends StaffOrderCard {
  shopId: number
  shopName: string
  shopPhone?: string
  reservedPhone?: string
  payStatusDesc?: string
  refundStatusDesc?: string
  productAmount: number
  packFee: number
  pickupToken?: string
  pickupDate?: string
  estimatedReadyTime?: string
  queueAheadOrders?: number
  queueAheadCups?: number
  payTime?: string
  verifyTime?: string
  cancelTime?: string
  cancelReason?: string
}

export interface PendingCountResult {
  preorders: number
  pending: number
  making: number
  waiting: number
  totalActive: number
}
