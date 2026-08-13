import { get, put, del } from '@/utils/request'
import type { NoticeVO } from '@/types/message'

export function getMyNoticeList() {
  return get<NoticeVO[]>('/notice/my-list')
}

export function readNotice(id: number) {
  return put<boolean>('/notice/read', { id })
}

export function readAllNotice() {
  return put<boolean>('/notice/read-all')
}

export function deleteNotice(id: number) {
  return del<boolean>(`/notice/${id}`)
}
