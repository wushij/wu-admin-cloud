import { get, post, put, del } from '@/utils/request'
import type { PageResult } from '@/types/api'
import type {
  AssigneeOptionVO,
  TicketCommentCreateDTO,
  TicketCommentVO,
  TicketPageQuery,
  TicketSaveDTO,
  TicketVO,
} from '@/types/system'

export function getTicketPage(params: TicketPageQuery) {
  return get<PageResult<TicketVO>>('/ticket/page', params)
}

export function getTicketAssigneeOptions() {
  return get<AssigneeOptionVO[]>('/ticket/assignee-options')
}

export function createTicket(data: TicketSaveDTO) {
  return post('/ticket/create', data)
}

export function getTicket(id: number) {
  return get<TicketVO>('/ticket/get', { id })
}

export function transitionTicket(data: { id: number; status: string }) {
  return put('/ticket/transition', data)
}

export function getTicketComments(ticketId: number) {
  return get<TicketCommentVO[]>('/ticket/comment/list', { ticketId })
}

export function createTicketComment(data: TicketCommentCreateDTO) {
  return post('/ticket/comment/create', data)
}

export function deleteTicket(id: number) {
  return del('/ticket/delete', { params: { id } })
}

export function getRecycleTicketPage(params: TicketPageQuery) {
  return get<PageResult<TicketVO>>('/ticket/recycle/page', params)
}

export function restoreTicket(id: number) {
  return put('/ticket/restore', null, { params: { id } })
}

export function deleteTicketPermanent(id: number) {
  return del('/ticket/delete-permanent', { params: { id } })
}
