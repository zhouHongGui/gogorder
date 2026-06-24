import request from '@/utils/request'

export function listShop(query) {
  return request({
    url: '/api/admin/shop/list',
    method: 'get',
    params: query
  })
}

export function listShopStaff(shopId) {
  return request({
    url: '/api/admin/shop/' + shopId + '/staff',
    method: 'get'
  })
}

export function addShopStaff(shopId, data) {
  return request({
    url: '/api/admin/shop/' + shopId + '/staff',
    method: 'post',
    data
  })
}

export function updateShopStaff(shopId, staffId, data) {
  return request({
    url: '/api/admin/shop/' + shopId + '/staff/' + staffId,
    method: 'put',
    data
  })
}

export function delShopStaff(shopId, staffId) {
  return request({
    url: '/api/admin/shop/' + shopId + '/staff/' + staffId,
    method: 'delete'
  })
}
