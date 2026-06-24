import request from '@/utils/request'

export function listStaff(query) {
  return request({
    url: '/api/admin/staff/list',
    method: 'get',
    params: query
  })
}

export function getStaff(id) {
  return request({
    url: '/api/admin/staff/' + id,
    method: 'get'
  })
}

export function addStaff(data) {
  return request({
    url: '/api/admin/staff',
    method: 'post',
    data
  })
}

export function updateStaff(id, data) {
  return request({
    url: '/api/admin/staff/' + id,
    method: 'put',
    data
  })
}

export function delStaff(id) {
  return request({
    url: '/api/admin/staff/' + id,
    method: 'delete'
  })
}

export function listStaffShops(id) {
  return request({
    url: '/api/admin/staff/' + id + '/shops',
    method: 'get'
  })
}
