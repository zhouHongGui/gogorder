/**
 * 订单相关类型。金额单位：分。
 * 状态码：orderStatus 0待支付 1已接单 2制作中 3待取餐 4已完成 5已取消；
 * payStatus 0待支付 1已支付 2已退款；refundStatus 0无退款 1退款成功。
 */
import type { OrderType } from './shop'

/** 下单请求中的单个商品明细。 */
export interface OrderItemRequest {
  productId: number
  specs: Record<string, string[]>
  quantity: number
}

/** 下单请求。submitToken 保证幂等。 */
export interface OrderSubmitRequest {
  shopId: number
  submitToken: string
  orderType: OrderType
  scheduledPickupTime?: string
  remark?: string
  items: OrderItemRequest[]
}

/** 下单响应。balance 为当前余额，供前端判断是否够付。 */
export interface OrderSubmitResponse {
  orderId: number
  orderNo: string
  totalAmount: number
  balance: number
  createTime: string
}

/** 支付响应。pickupDisplay 为取餐展示号，balanceAfter 为扣款后余额。 */
export interface PayResponse {
  orderNo: string
  pickupDisplay: string
  balanceAfter: number
}

/** 订单明细展示（详情/列表通用，来自订单明细快照）。 */
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

/** 订单详情（含完整时间线）。 */
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
  queueAheadOrders?: number
  queueAheadCups?: number
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

/** 订单列表项（比详情精简）。 */
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

/** 订单列表分页结果。hasMore 供前端判断是否可加载更多。 */
export interface OrderPage {
  rows: OrderListItem[]
  total: number
  pageNum: number
  pageSize: number
  hasMore: boolean
}

/** 订单列表查询参数。dateScope：TODAY=今日 / HISTORY=历史。 */
export interface OrderListParams {
  dateScope: 'TODAY' | 'HISTORY'
  pageNum: number
  pageSize: number
}
