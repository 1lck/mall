import http from '@/utils/http'
import type { DashboardResponse } from '@/types/dashboard'

export function getAdminDashboardAPI(params?: { startDate?: string; endDate?: string }) {
  return http<DashboardResponse>({
    method: 'GET',
    url: '/api/v1/admin/dashboard',
    params,
  })
}
