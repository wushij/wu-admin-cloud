import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  ApprovalCreateDTO,
  ApprovalPageQuery,
  ApprovalRecordVO,
  ApprovalVO,
} from '@/types/system'

export function getApprovalPage(params: ApprovalPageQuery) {
  return get<PageResult<ApprovalVO>>('/approval/page', params)
}

export function getApproval(id: number) {
  return get<ApprovalVO>('/approval/get', { id })
}

export function getApprovalRecords(formId: number) {
  return get<ApprovalRecordVO[]>('/approval/record/list', { formId })
}

export interface ApprovalApproverOptionVO {
  id: number
  username: string
  nickname?: string
}

export function getApprovalApproverOptions() {
  return get<ApprovalApproverOptionVO[]>('/approval/approver-options')
}

export function createApproval(data: ApprovalCreateDTO) {
  return post('/approval/create', data)
}

export function approveApproval(data: { id: number; action: string; remark?: string }) {
  return put('/approval/approve', data)
}

export function archiveApproval(data: { id: number; remark?: string }) {
  return put('/approval/archive', data)
}

export function deleteApproval(id: number) {
  return del('/approval/delete', { params: { id } })
}

export function getRecycleApprovalPage(params: ApprovalPageQuery) {
  return get<PageResult<ApprovalVO>>('/approval/recycle/page', params)
}

export function restoreApproval(id: number) {
  return put('/approval/restore', null, { params: { id } })
}

export function deleteApprovalPermanent(id: number) {
  return del('/approval/delete-permanent', { params: { id } })
}
