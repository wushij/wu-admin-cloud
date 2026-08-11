import { get } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type { ApiAccessLogRow, ApiAccessPageQuery, ApiAccessStatistics } from '@/types/system'

export function getApiAccessPage(params: ApiAccessPageQuery) {
  return get<PageResult<ApiAccessLogRow>>('/system/monitor/api-access/page', params)
}

export function getApiAccessStatistics(params?: {
  startDate?: string
  endDate?: string
  startTime?: string
  endTime?: string
}) {
  return get<ApiAccessStatistics>('/system/monitor/api-access/statistics', params)
}
