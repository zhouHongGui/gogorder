import { request } from '../utils/request'
import type { StaffStatOverview, StaffStatRange } from '../types/stat'

export const getStatOverview = (range: StaffStatRange) => request<StaffStatOverview>({
  url: '/api/b/stat/overview',
  method: 'GET',
  data: { range }
})
