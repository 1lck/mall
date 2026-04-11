export type DashboardSummaryCards = {
  todayOrderCount: number
  todaySalesAmount: number
  yesterdaySalesAmount: number
}

export type DashboardPendingTask = {
  label: string
  count: number
}

export type DashboardProductOverview = {
  offShelfCount: number
  onSaleCount: number
  lowStockCount: number
  totalCount: number
}

export type DashboardUserOverview = {
  todayNewCount: number
  yesterdayNewCount: number
  monthNewCount: number
  totalCount: number
}

export type DashboardOrderStatistics = {
  monthOrderCount: number
  weekOrderCount: number
  monthSalesAmount: number
  weekSalesAmount: number
}

export type DashboardOrderTrendItem = {
  date: string
  orderCount: number
  orderAmount: number
}

export type DashboardResponse = {
  summaryCards: DashboardSummaryCards
  pendingTasks: DashboardPendingTask[]
  productOverview: DashboardProductOverview
  userOverview: DashboardUserOverview
  orderStatistics: DashboardOrderStatistics
  orderTrend: DashboardOrderTrendItem[]
}
