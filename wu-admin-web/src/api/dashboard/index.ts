import { get } from '@/utils/request'
import type { WuRequestConfig } from '@/utils/request'

/** 工作台统计（与后端 DashboardStatsVO 对齐） */
export interface DashboardStats {
  userCount?: number
  roleCount?: number
  deptCount?: number
  menuCount?: number
  postCount?: number
  onlineCount?: number
  visitCount?: number
  todayVisits?: number
  yesterdayVisits?: number
  platformName?: string
  platformSubtitle?: string
  userTrend?: number
  roleTrend?: number
  deptTrend?: number
  menuTrend?: number
  userPendingCount?: number
  userDisabledCount?: number
  todayLoginSuccess?: number
  todayLoginFail?: number
  fileCount?: number
  fileMaxSizeMb?: number
  fileAllowedExtensions?: string
  tokenExpireHours?: number
  loginCaptchaEnabled?: boolean
  loginCaptchaType?: string
  loginRememberMe?: boolean
  loginMaxRetryCount?: number
  loginLockTimeMinutes?: number
  registerEnabled?: boolean
  registerNeedAudit?: boolean
  ticketOpenCount?: number
  ticketOverdueCount?: number
  approvalPendingCount?: number
  jobTotalCount?: number
  jobRunningCount?: number
  jobPausedCount?: number
  chatUnreadCount?: number
  configGroupCount?: number
}

export interface RecentLogin {
  userId?: number
  username?: string
  nickname?: string
  avatar?: string
  ipaddr?: string
  loginLocation?: string
  loginTime?: string
  browser?: string
  os?: string
  status?: number
}

/** 工作台等非关键读接口：503 自动重试 2 次，最终失败也不弹红框 */
const DASHBOARD_REQUEST_CONFIG: WuRequestConfig = {
  silent503: true,
  retry503: 2,
  timeout: 8000,
}

/** 访问埋点：旁路请求，失败完全静默（偶发 BFF 重启/网关 lb 无实例会 503） */
const DASHBOARD_VISIT_CONFIG: WuRequestConfig = {
  silent: true,
  silent503: true,
  retry503: 1,
  timeout: 3000,
}

export function getDashboardStats() {
  return get<DashboardStats>('/admin/dashboard/stats', undefined, DASHBOARD_REQUEST_CONFIG)
}

export function recordVisit() {
  return get<unknown>('/admin/dashboard/visit', undefined, DASHBOARD_VISIT_CONFIG)
}

export function getRecentLogins() {
  return get<RecentLogin[]>('/admin/dashboard/recent-logins', undefined, DASHBOARD_REQUEST_CONFIG)
}
