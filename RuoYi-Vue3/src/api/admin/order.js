import request from '@/utils/request'

export function listOrder(query) {
  return request({
    url: '/api/admin/order/list',
    method: 'get',
    params: query
  })
}

export function getOrder(id) {
  return request({
    url: '/api/admin/order/' + id,
    method: 'get'
  })
}
