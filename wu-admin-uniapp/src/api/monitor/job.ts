import { get, post, put, del } from '@/utils/request'

import type { PageResult } from '@/types/api'

import type { JobPageQuery, JobOverview, SysJob, SysJobLog } from '@/types/system'

export function getJobOverview() {
  return get<JobOverview>('/system/monitor/job/overview')
}

export function getJobPage(params: JobPageQuery) {

  return get<PageResult<SysJob>>('/system/monitor/job/page', params)

}



export function getJobLogPage(params: JobPageQuery) {

  return get<PageResult<SysJobLog>>('/system/monitor/job/log/page', params)

}



export function createJob(data: SysJob) {

  return post<boolean>('/system/monitor/job', data)

}



export function updateJob(data: SysJob) {

  return put<boolean>('/system/monitor/job', data)

}



export function deleteJob(id: number) {

  return del<boolean>(`/system/monitor/job/${id}`)

}



export function changeJobStatus(id: number, status: number) {

  return put<boolean>('/system/monitor/job/changeStatus', { id, status })

}



export function runJob(id: number) {

  return post<boolean>(`/system/monitor/job/run/${id}`)

}



export function cleanJobLogs(params?: { jobName?: string; jobGroup?: string }) {
  return del<boolean>('/system/monitor/job/log/clean', { params })
}



export function getRecycleJobPage(params: JobPageQuery) {

  return get<PageResult<SysJob>>('/system/monitor/job/recycle/page', params)

}



export function restoreJob(id: number) {

  return put<boolean>('/system/monitor/job/restore', null, { params: { id } })

}



export function deleteJobPermanent(id: number) {

  return del<boolean>('/system/monitor/job/delete-permanent', { params: { id } })

}

export function getRecycleJobLogPage(params: Record<string, unknown>) {

  return get<PageResult<SysJobLog>>('/system/monitor/job/log/recycle/page', params)

}

export function restoreJobLog(id: number) {

  return put<boolean>('/system/monitor/job/log/restore', null, { params: { id } })

}

export function deleteJobLogPermanent(id: number) {

  return del<boolean>('/system/monitor/job/log/delete-permanent', { params: { id } })

}


