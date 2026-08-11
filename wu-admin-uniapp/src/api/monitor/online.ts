import { get, del } from '@/utils/request'
import type { OnlineUser } from '@/types/system'

export function getOnlineUserList() {
  return get<OnlineUser[]>('/system/monitor/online/list')
}

export function forceLogoutOnlineUser(userId: number) {
  return del(`/system/monitor/online/${userId}`)
}
