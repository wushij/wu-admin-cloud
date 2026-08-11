import { get, post, put, del } from '@/utils/request'
import { uploadFile } from '@/utils/upload'
import type { PageQuery, PageResult } from '@/types/api'
import type { FileRecord } from '@/api/system/file/index'
import type {
  AnnounceMyVO,
  AnnounceSaveDTO,
  AnnounceVO,
  ChatGroup,
  ChatGroupLogItem,
  ChatMessage,
  ChatUser,
  GroupMember,
} from '@/types/message'

export interface MessageSummary {
  inboxCount?: number
  announceCount?: number
  chatCount?: number
  noticeCount?: number
  total?: number
}

export interface AnnounceDetail {
  id: number
  title: string
  content: string
  noticeType?: number
  createName?: string
  createTime?: string
  createAvatar?: string
}

export interface MessagePageQuery extends PageQuery {
  title?: string
  noticeType?: number | null
  status?: number | null
  isRead?: number | null
}

export interface AnnounceSendLog {
  channel?: string
  targetCount?: number
  successCount?: number
  sendTime?: string
}

export function getMessageSummary() {
  return get<MessageSummary>('/message/summary')
}

export function getAnnouncePage(params: MessagePageQuery) {
  return get<PageResult<AnnounceVO>>('/message/announce/page', params)
}

export function getMyAnnounce(params?: MessagePageQuery) {
  return get<PageResult<AnnounceMyVO>>('/message/announce/my', params)
}

export function createAnnounce(data: AnnounceSaveDTO) {
  return post('/message/announce', data)
}

export function updateAnnounce(data: AnnounceSaveDTO) {
  return put('/message/announce', data)
}

export function deleteAnnounce(id: number) {
  return del(`/message/announce/${id}`)
}

export function publishAnnounce(id: number) {
  return post(`/message/announce/${id}/publish`)
}

export function getAnnounceSendLogs(id: number) {
  return get<AnnounceSendLog[]>(`/message/announce/${id}/send-logs`)
}

export function getRecycleAnnouncePage(params: { pageNo: number; pageSize: number; title?: string }) {
  return get<PageResult<AnnounceVO>>('/message/announce/recycle/page', params)
}

export function restoreAnnounce(id: number) {
  return put('/message/announce/restore', null, { params: { id } })
}

export function deleteAnnouncePermanent(id: number) {
  return del('/message/announce/delete-permanent', { params: { id } })
}

export function getAnnounceDetail(id: number) {
  return get<AnnounceDetail>(`/message/announce/${id}`)
}

export function getAnnounceAdminDetail(id: number) {
  return get<AnnounceVO>(`/message/announce/${id}`)
}

export function readAnnounce(id: number) {
  return post<boolean>(`/message/announce/${id}/read`)
}

export function readAllAnnounce() {
  return post<boolean>('/message/announce/read-all')
}

export function getChatUsers() {
  return get<ChatUser[]>('/message/chat/users')
}

export interface CreateGroupPayload {
  name: string
  memberIds: number[]
}

export interface UpdateGroupPayload {
  id: number
  name?: string
  announcement?: string
}

export function createChatGroup(data: CreateGroupPayload) {
  return post<ChatGroup>('/message/chat/group/create', data)
}

export function updateChatGroup(data: UpdateGroupPayload) {
  return put('/message/chat/group/update', data)
}

export function addGroupMembers(groupId: number, userIds: number[]) {
  return post(`/message/chat/group/${groupId}/members`, { userIds })
}

export function getChatGroups() {
  return get<ChatGroup[]>('/message/chat/group/list')
}

export function getChatHistory(targetId: number, params?: PageQuery) {
  return get<PageResult<ChatMessage>>(`/message/chat/history/${targetId}`, params)
}

export function getGroupMessages(groupId: number, params?: PageQuery) {
  return get<PageResult<ChatMessage>>(`/message/chat/group/${groupId}/messages`, params)
}

export function readChat(senderId: number) {
  return post<boolean>(`/message/chat/read/${senderId}`)
}

export interface ChatSendPayload {
  receiverId?: number
  content: string
  msgType?: number
  mentionIds?: number[]
}

export function sendChat(data: ChatSendPayload & { receiverId: number }) {
  return post<ChatMessage>('/message/chat/send', data)
}

export function sendGroupMessage(groupId: number, data: ChatSendPayload) {
  return post<ChatMessage>(`/message/chat/group/${groupId}/message`, data)
}

export function uploadChatImage(filePath: string) {
  return uploadFile<FileRecord>({ url: '/message/chat/upload/image', filePath })
}

export function uploadChatFile(filePath: string) {
  return uploadFile<FileRecord>({ url: '/message/chat/upload/file', filePath })
}

export function recallPrivateMessage(messageId: number) {
  return post<ChatMessage>(`/message/chat/recall/${messageId}`)
}

export function recallGroupMessage(groupId: number, messageId: number) {
  return post<ChatMessage>(`/message/chat/group/${groupId}/message/${messageId}/recall`)
}

export function sendTypingSignal(targetUserId: number) {
  return post(`/message/chat/typing/${targetUserId}`)
}

export function getGroupMembers(groupId: number) {
  return get<GroupMember[]>(`/message/chat/group/${groupId}/members`)
}

export function getGroupDetail(groupId: number) {
  return get<ChatGroup>(`/message/chat/group/${groupId}`)
}

export function markGroupAnnouncementRead(groupId: number) {
  return post<boolean>(`/message/chat/group/${groupId}/announcement/read`)
}

export function setGroupNotifyMuted(groupId: number, muted = true) {
  return post<boolean>(`/message/chat/group/${groupId}/notify-muted`, null, { params: { muted } })
}

export function getGroupLogs(groupId: number) {
  return get<ChatGroupLogItem[]>(`/message/chat/group/${groupId}/logs`)
}

export function removeGroupMember(groupId: number, memberUserId: number) {
  return del(`/message/chat/group/${groupId}/members/${memberUserId}`)
}

export function setGroupAdmin(groupId: number, memberUserId: number, admin = true) {
  return post(`/message/chat/group/${groupId}/admin/${memberUserId}`, null, { params: { admin } })
}

export function setGroupMuted(groupId: number, memberUserId: number, muted = true) {
  return post(`/message/chat/group/${groupId}/mute/${memberUserId}`, null, { params: { muted } })
}

export function transferGroupOwner(groupId: number, newOwnerId: number) {
  return post(`/message/chat/group/${groupId}/transfer/${newOwnerId}`)
}

export function quitGroup(groupId: number) {
  return post(`/message/chat/group/${groupId}/quit`)
}

export function dissolveGroup(groupId: number) {
  return del(`/message/chat/group/${groupId}`)
}

export function canCreateChatGroup() {
  return get<boolean>('/message/chat/can-create-group')
}
