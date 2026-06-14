import { request } from '../utils/request'
import type { AddCartItemRequest, Cart, UpdateCartItemRequest } from '../types/cart'

export const getCart = (shopId: number) => request<Cart>({
  url: '/api/c/cart/get',
  method: 'GET',
  data: { shopId }
})

export const addCartItem = (data: AddCartItemRequest) => request<Cart>({
  url: '/api/c/cart/add',
  method: 'POST',
  data,
  showErrorToast: false
})

export const updateCartItem = (data: UpdateCartItemRequest) => request<Cart>({
  url: '/api/c/cart/update',
  method: 'PUT',
  data
})

export const removeCartItem = (shopId: number, cartItemId: string) => request<Cart>({
  url: `/api/c/cart/remove/${encodeURIComponent(cartItemId)}?shopId=${shopId}`,
  method: 'DELETE'
})

export const clearCart = (shopId: number) => request<Cart>({
  url: `/api/c/cart/clear?shopId=${shopId}`,
  method: 'DELETE'
})
