/**
 * 商品与分类 API（{@code /api/c/product/**}、{@code /api/c/category/**}，公开路径）。
 */
import { request } from '../utils/request'
import type { Category, Product, ProductQuery } from '../types/product'

/** 查询门店下的商品分类列表（菜单顶部分类筛选）。 */
export const getCategories = (shopId: number) => request<Category[]>({
  url: '/api/c/category/list',
  method: 'GET',
  data: { shopId }
})

/** 查询门店商品列表（菜单），可按分类/关键字筛选。 */
export const getProducts = (params: ProductQuery) => request<Product[]>({
  url: '/api/c/product/list',
  method: 'GET',
  data: params
})

/** 查询商品详情（含完整规格，供加购/下单选规格）。 */
export const getProductDetail = (shopId: number, productId: number) => request<Product>({
  url: `/api/c/product/${productId}`,
  method: 'GET',
  data: { shopId }
})
