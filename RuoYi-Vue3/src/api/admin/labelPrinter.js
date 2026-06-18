// 引入统一的 axios 请求封装（携带 token、统一错误处理、/dev-api 代理）
import request from '@/utils/request'

// 分页查询标签打印机列表
export function listLabelPrinter(query) {
  return request({
    url: '/api/admin/shop-label-printer/list', // 列表接口
    method: 'get', // GET 请求
    params: query // 查询条件（含 pageNum/pageSize 及筛选字段）
  })
}

// 查询单台打印机详情（不含设备密钥）
export function getLabelPrinter(id) {
  return request({
    url: '/api/admin/shop-label-printer/' + id, // 详情接口，路径携带 id
    method: 'get'
  })
}

// 新增标签打印机（会同步绑定到飞鹅平台）
export function addLabelPrinter(data) {
  return request({
    url: '/api/admin/shop-label-printer', // 新增接口
    method: 'post', // POST 请求
    data // 表单数据（含 SN、密钥、尺寸等）
  })
}

// 修改标签打印机（按需同步飞鹅）
export function updateLabelPrinter(id, data) {
  return request({
    url: '/api/admin/shop-label-printer/' + id, // 修改接口，路径携带 id
    method: 'put', // PUT 请求
    data
  })
}

// 逻辑删除标签打印机（会先在飞鹅平台解绑）
export function delLabelPrinter(id) {
  return request({
    url: '/api/admin/shop-label-printer/' + id, // 删除接口，路径携带 id
    method: 'delete' // DELETE 请求
  })
}

// 查询打印机的飞鹅在线状态（并回写后端）
export function queryLabelPrinterStatus(id) {
  return request({
    url: '/api/admin/shop-label-printer/' + id + '/status', // 状态查询接口
    method: 'get'
  })
}

// 向指定打印机发送一张测试标签
export function testLabelPrinter(id) {
  return request({
    url: '/api/admin/shop-label-printer/' + id + '/test', // 测试打印接口
    method: 'post' // POST 请求
  })
}
