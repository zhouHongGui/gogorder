/**
 * 购物车相关 API（{@code /api/c/cart/**}，需登录）。所有接口都需带 shopId（每门店一车）。
 */
import { request } from '../utils/request'
import type { AddCartItemRequest, Cart, UpdateCartItemRequest } from '../types/cart'

/** 查询指定门店的购物车。 */
export const getCart = (shopId: number) => request<Cart>({
  url: '/api/c/cart/get',
  method: 'GET',
  data: { shopId }
})

/** 加入购物车（同商品同规格自动合并）。showErrorToast:false 以便菜单页自定义库存不足等提示。 */
export const addCartItem = (data: AddCartItemRequest) => request<Cart>({
  url: '/api/c/cart/add',
  method: 'POST',
  data,
  showErrorToast: false
})

/** 修改购物车条目数量（quantity=0 表示删除）。 */
export const updateCartItem = (data: UpdateCartItemRequest) => request<Cart>({
  url: '/api/c/cart/update',
  method: 'PUT',
  data
})

/** 删除购物车条目。 */
export const removeCartItem = (shopId: number, cartItemId: string) => request<Cart>({
  url: `/api/c/cart/remove/${encodeURIComponent(cartItemId)}?shopId=${shopId}`,
  method: 'DELETE'
})

/** 清空指定门店的购物车（下单成功后调用）。 */
export const clearCart = (shopId: number) => request<Cart>({
  url: `/api/c/cart/clear?shopId=${shopId}`,
  method: 'DELETE'
})
