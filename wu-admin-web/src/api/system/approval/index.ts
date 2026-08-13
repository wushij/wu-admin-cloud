import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult, RecyclePageQuery } from '@/types/api'

export interface ApprovalVO {
  id: number
  formNo?: string
  formType?: string
  title?: string
  content?: string
  status?: string
  applicantUserId?: number
  approverUserId?: number
  applicantName?: string
  approverName?: string
  resultRemark?: string
  createTime?: string
  updateTime?: string
}

export interface ApprovalRecordVO {
  id: number
  formId?: number
  operatorUserId?: number
  operatorName?: string
  action?: string
  remark?: string
  createTime?: string
}

export interface ApprovalPageQuery extends PageQuery {
  title?: string
  formType?: string
  status?: string
}

export interface ApprovalCreateDTO {
  formType: string
  title: string
  approverUserId: number | null
  content: string
}

export interface ApprovalActionDTO {
  id: number
  action: string
  remark?: string
}

export interface ApprovalArchiveDTO {
  id: number
  remark?: string
}

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

export function approveApproval(data: ApprovalActionDTO) {
  return put('/approval/approve', data)
}

export function archiveApproval(data: ApprovalArchiveDTO) {
  return put('/approval/archive', data)
}

export function deleteApproval(id: number) {
  return del('/approval/delete', { params: { id } })
}

export function getApprovalRecyclePage(params: RecyclePageQuery) {
  return get<PageResult<ApprovalVO>>('/approval/recycle/page', params)
}

export function restoreApproval(id: number) {
  return put('/approval/restore', null, { params: { id } })
}

export function deleteApprovalPermanent(id: number) {
  return del('/approval/delete-permanent', { params: { id } })
}
