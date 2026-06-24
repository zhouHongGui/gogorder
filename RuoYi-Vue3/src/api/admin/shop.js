import request from '@/utils/request'

export function getShopMapConfig() {
  return request({
    url: '/api/admin/shop/map-config',
    method: 'get'
  })
}

export function listShop(query) {
  return request({
    url: '/api/admin/shop/list',
    method: 'get',
    params: query
  })
}

export function getShop(id) {
  return request({
    url: '/api/admin/shop/' + id,
    method: 'get'
  })
}

export function addShop(data) {
  return request({
    url: '/api/admin/shop',
    method: 'post',
    data
  })
}

export function updateShop(id, data) {
  return request({
    url: '/api/admin/shop/' + id,
    method: 'put',
    data
  })
}

export function changeShopStatus(id, status) {
  return request({
    url: '/api/admin/shop/' + id + '/status',
    method: 'put',
    data: { status }
  })
}

export function delShop(id) {
  return request({
    url: '/api/admin/shop/' + id,
    method: 'delete'
  })
}

export function listShopStaff(id) {
  return request({
    url: '/api/admin/shop/' + id + '/staff',
    method: 'get'
  })
}

export function addShopStaff(id, data) {
  return request({
    url: '/api/admin/shop/' + id + '/staff',
    method: 'post',
    data
  })
}

export function updateShopStaff(id, staffId, data) {
  return request({
    url: '/api/admin/shop/' + id + '/staff/' + staffId,
    method: 'put',
    data
  })
}

export function delShopStaff(id, staffId) {
  return request({
    url: '/api/admin/shop/' + id + '/staff/' + staffId,
    method: 'delete'
  })
}
