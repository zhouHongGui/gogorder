/**
 * 订单相关 API（{@code /api/c/order/**}，需登录）。
 */
import { request } from '../utils/request'
import type {
  OrderDetail,
  OrderListParams,
  OrderPage,
  OrderSubmitRequest,
  OrderSubmitResponse,
  PayResponse
} from '../types/order'

/** 提交订单。showErrorToast:false 由调用方自行处理错误（如跳结果页展示原因）。 */
export const submitOrder = (data: OrderSubmitRequest) => request<OrderSubmitResponse>({
  url: '/api/c/order/submit',
  method: 'POST',
  data,
  showErrorToast: false
})

/** 支付订单（余额支付）。showErrorToast:false 以便调用方展示「余额不足」等富文本。 */
export const payOrder = (orderId: number) => request<PayResponse>({
  url: `/api/c/order/pay/${orderId}`,
  method: 'POST',
  showErrorToast: false
})

/** 取消订单（仅未支付订单可取消）。 */
export const cancelOrder = (orderId: number) => request<void>({
  url: `/api/c/order/cancel/${orderId}`,
  method: 'PUT'
})

/** 查询订单详情。 */
export const getOrderDetail = (orderId: number) => request<OrderDetail>({
  url: `/api/c/order/${orderId}`,
  method: 'GET'
})

/** 分页查询订单列表（dateScope/pageNum/pageSize）。 */
export const getOrders = (data: OrderListParams) => request<OrderPage>({
  url: '/api/c/order/list',
  method: 'GET',
  data
})
