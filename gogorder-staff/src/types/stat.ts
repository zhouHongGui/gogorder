export type StaffStatRange = 'TODAY' | 'YESTERDAY' | 'LAST_7_DAYS'

export interface StaffStatStatusCount {
  orderStatus: number
  orderStatusDesc: string
  count: number
}

export interface StaffStatProductRank {
  productId: number
  productName: string
  quantity: number
  salesAmount: number
}

export interface StaffStatOverview {
  range: StaffStatRange
  rangeDesc: string
  startDate: string
  endDate: string
  turnoverAmount: number
  orderCount: number
  cupCount: number
  avgOrderAmount: number
  statusCounts: StaffStatStatusCount[]
  productRanks: StaffStatProductRank[]
}
