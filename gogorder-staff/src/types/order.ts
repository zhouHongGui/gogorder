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
  makeTimeout?: boolean
  items: OrderItem[]
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
  productAmount: number
  packFee: number
  pickupToken?: string
  pickupDate?: string
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
