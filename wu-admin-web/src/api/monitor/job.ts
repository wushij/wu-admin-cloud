import { del, get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface SysJob {
  id?: number
  jobName: string
  jobGroup?: string
  invokeTarget: string
  cronExpression: string
  misfirePolicy?: number
  concurrent?: number
  status?: number
  remark?: string
  createTime?: string
  nextFireTime?: string
  cronHint?: string
}

export interface JobTemplate {
  key: string
  name: string
  category?: string
  description?: string
  invokeTarget: string
  cronExpression: string
  jobGroup?: string
  relatedModule?: string
  icon?: string
}

export interface JobOverview {
  totalJobs?: number
  runningJobs?: number
  pausedJobs?: number
  totalCount?: number
  successCount?: number
  failCount?: number
  successRate?: number
  dailyStats?: Array<Record<string, unknown>>
}

export interface CronCheckResult {
  valid?: boolean
  hint?: string
  nextFireTimes?: string[]
}

export interface SysJobLog {
  id: number
  jobName?: string
  jobGroup?: string
  invokeTarget?: string
  jobMessage?: string
  status?: number
  exceptionInfo?: string
  startTime?: string
  stopTime?: string
  durationMs?: number
  updateTime?: string
}

export interface JobPageQuery extends PageQuery {
  jobName?: string
  jobGroup?: string
  status?: number | null
}

export interface JobLogPageQuery extends PageQuery {
  jobName?: string
  jobGroup?: string
  status?: number | null
}

export const getJobOverview = () => get<JobOverview>('/infra/job/overview')

export const getJobTemplates = () => get<JobTemplate[]>('/infra/job/templates')

export const checkJobCron = (cronExpression: string) =>
  get<CronCheckResult>('/infra/job/checkCron', { cronExpression })

export const getJobPage = (params: JobPageQuery) =>
  get<PageResult<SysJob>>('/infra/job/page', params)

export const createJob = (data: SysJob) => post<boolean>('/infra/job', data)

export const updateJob = (data: SysJob) => put<boolean>('/infra/job', data)

export const deleteJob = (id: number) => del<boolean>(`/infra/job/${id}`)

export const changeJobStatus = (id: number, status: number) =>
  put<boolean>('/infra/job/changeStatus', { id, status })

export const runJob = (id: number) => post<boolean>(`/infra/job/run/${id}`)

export const getJobLogPage = (params: JobLogPageQuery) =>
  get<PageResult<SysJobLog>>('/infra/job/log/page', params)

export const cleanJobLogs = (params?: { jobName?: string; jobGroup?: string }) =>
  del<boolean>('/infra/job/log/clean', { params })

export function getRecycleJobPage(params: {
  pageNo: number
  pageSize: number
  jobName?: string
  jobGroup?: string
}) {
  return get<PageResult<SysJob>>('/infra/job/recycle/page', params)
}

export function restoreJob(id: number) {
  return put<boolean>('/infra/job/restore', null, { params: { id } })
}

export function deleteJobPermanent(id: number) {
  return del<boolean>('/infra/job/delete-permanent', { params: { id } })
}

export function getRecycleJobLogPage(params: {
  pageNo: number
  pageSize: number
  jobName?: string
  jobGroup?: string
}) {
  return get<PageResult<SysJobLog>>('/infra/job/log/recycle/page', params)
}

export function restoreJobLog(id: number) {
  return put<boolean>('/infra/job/log/restore', null, { params: { id } })
}

export function deleteJobLogPermanent(id: number) {
  return del<boolean>('/infra/job/log/delete-permanent', { params: { id } })
}
