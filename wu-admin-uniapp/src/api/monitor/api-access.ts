import { get } from '@/core/http'
import type { PageResult } from '@/types/api'
import type { ApiAccessLogRow, ApiAccessPageQuery, ApiAccessStatistics } from '@/types/system'

export function getApiAccessPage(params: ApiAccessPageQuery) {
  return get<PageResult<ApiAccessLogRow>>('/infra/access-log/page', params)
}

export function getApiAccessStatistics(params?: {
  startDate?: string
  endDate?: string
  startTime?: string
  endTime?: string
}) {
  return get<ApiAccessStatistics>('/infra/access-log/statistics', params)
}
