import { get, del } from '@/utils/request'

import type { CacheInfo, CacheKeysResult, CacheStats, CacheValueDetail } from '@/types/system'



export function getCacheStats() {

  return get<CacheStats>('/system/monitor/cache/stats')

}



export function getCacheInfo() {

  return get<CacheInfo>('/system/monitor/cache/info')

}



export function scanCacheKeys(pattern = '*', limit = 200) {

  return get<CacheKeysResult>('/system/monitor/cache/keys', { pattern, limit })

}



export function getCacheValue(key: string) {

  return get<CacheValueDetail>('/system/monitor/cache/value', { key })

}



export function deleteCacheKey(key: string) {

  return del<boolean>('/system/monitor/cache', { params: { key } })

}


