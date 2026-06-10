import request from '@/utils/request'

export const listCategory = params => request({ url: '/api/admin/category/list', method: 'get', params })
export const addCategory = data => request({ url: '/api/admin/category', method: 'post', data })
export const updateCategory = (id, data) => request({ url: `/api/admin/category/${id}`, method: 'put', data })
export const delCategory = id => request({ url: `/api/admin/category/${id}`, method: 'delete' })

export const listSpecTemplate = params => request({ url: '/api/admin/spec-template/list', method: 'get', params })
export const addSpecTemplate = data => request({ url: '/api/admin/spec-template', method: 'post', data })
export const updateSpecTemplate = (id, data) => request({ url: `/api/admin/spec-template/${id}`, method: 'put', data })
export const delSpecTemplate = id => request({ url: `/api/admin/spec-template/${id}`, method: 'delete' })
export const listSpecOption = templateId => request({ url: '/api/admin/spec-option/list', method: 'get', params: { templateId } })
export const addSpecOption = data => request({ url: '/api/admin/spec-option', method: 'post', data })
export const updateSpecOption = (id, data) => request({ url: `/api/admin/spec-option/${id}`, method: 'put', data })
export const disableSpecOption = id => request({ url: `/api/admin/spec-option/${id}/disable`, method: 'put' })

export const listProduct = params => request({ url: '/api/admin/product/list', method: 'get', params })
export const getProduct = id => request({ url: `/api/admin/product/${id}`, method: 'get' })
export const addProduct = data => request({ url: '/api/admin/product', method: 'post', data })
export const updateProduct = (id, data) => request({ url: `/api/admin/product/${id}`, method: 'put', data })
export const delProduct = id => request({ url: `/api/admin/product/${id}`, method: 'delete' })

export const listShopProduct = (shopId, params) => request({ url: `/api/admin/shop-product/list/${shopId}`, method: 'get', params })
export const assignShopProduct = data => request({ url: '/api/admin/shop-product/assign', method: 'post', data })
export const updateShopProduct = (id, data) => request({ url: `/api/admin/shop-product/${id}`, method: 'put', data })
export const adjustShopProductStock = (id, data) => request({ url: `/api/admin/shop-product/${id}/stock-adjust`, method: 'post', data })
export const removeShopProducts = ids => request({ url: '/api/admin/shop-product/batch-remove', method: 'delete', data: { ids } })
