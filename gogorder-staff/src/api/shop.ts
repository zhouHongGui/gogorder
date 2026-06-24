import { request } from '../utils/request'
import type { ShopContext, ShopMineResult } from '../types/auth'

export const getMyShops = () => request<ShopMineResult>({
  url: '/api/b/shop/mine',
  method: 'GET'
})

export const switchShop = (shopId: number) => request<ShopContext>({
  url: '/api/b/shop/switch',
  method: 'PUT',
  data: { shopId }
})
