import { request } from '../utils/request'
import type {
  OrderDetail,
  OrderListParams,
  OrderPage,
  OrderSubmitRequest,
  OrderSubmitResponse,
  PayResponse
} from '../types/order'

export const submitOrder = (data: OrderSubmitRequest) => request<OrderSubmitResponse>({
  url: '/api/c/order/submit',
  method: 'POST',
  data,
  showErrorToast: false
})

export const payOrder = (orderId: number) => request<PayResponse>({
  url: `/api/c/order/pay/${orderId}`,
  method: 'POST',
  showErrorToast: false
})

export const cancelOrder = (orderId: number) => request<void>({
  url: `/api/c/order/cancel/${orderId}`,
  method: 'PUT'
})

export const getOrderDetail = (orderId: number) => request<OrderDetail>({
  url: `/api/c/order/${orderId}`,
  method: 'GET'
})

export const getOrders = (data: OrderListParams) => request<OrderPage>({
  url: '/api/c/order/list',
  method: 'GET',
  data
})
