import { request } from '../utils/request'
import type { PendingCountResult, StaffOrderBoard, StaffOrderDetail } from '../types/order'

export const getOrderBoard = () => request<StaffOrderBoard>({
  url: '/api/b/order/board',
  method: 'GET'
})

export const getPendingCount = () => request<PendingCountResult>({
  url: '/api/b/order/pending-count',
  method: 'GET',
  showErrorToast: false
})

export const getOrderDetail = (orderId: number) => request<StaffOrderDetail>({
  url: `/api/b/order/${orderId}/detail`,
  method: 'GET'
})

export const startMake = (orderId: number) => request<void>({
  url: `/api/b/order/${orderId}/start-make`,
  method: 'PUT',
  data: {}
})

export const batchStartMake = (orderIds: number[]) => request<{ count: number }>({
  url: '/api/b/order/batch-start-make',
  method: 'PUT',
  data: { orderIds }
})

export const completeMake = (orderId: number) => request<void>({
  url: `/api/b/order/${orderId}/complete-make`,
  method: 'PUT',
  data: {}
})

export const verifyOrder = (pickupToken: string) => request<StaffOrderDetail>({
  url: '/api/b/order/verify',
  method: 'PUT',
  data: { pickupToken }
})

export const cancelRefund = (orderId: number, reason: string) => request<void>({
  url: `/api/b/order/${orderId}/cancel-refund`,
  method: 'PUT',
  data: { reason }
})
