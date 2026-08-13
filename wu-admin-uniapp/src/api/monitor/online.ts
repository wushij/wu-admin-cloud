import { get, del } from '@/utils/request'
import type { OnlineUser } from '@/types/system'

export function getOnlineUserList() {
  return get<OnlineUser[]>('/infra/online/list')
}

export function forceLogoutOnlineUser(userId: number) {
  return del(`/infra/online/${userId}`)
}
