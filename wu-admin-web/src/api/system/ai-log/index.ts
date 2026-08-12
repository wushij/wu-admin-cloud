import { get, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface AiChatLogVO {
  /** 后端 Long 可能以字符串返回，避免雪花 ID 精度丢失 */
  id: number | string
  userId: number | string
  username: string
  conversationId: string
  modelId: number | string
  provider: string
  modelName: string
  question: string
  answer: string
  promptTokens: number
  completionTokens: number
  totalTokens: number
  durationMs: number
  /** 1:成功 0:失败 2:用户中断 */
  chatStatus: number
  errorMsg?: string
  source: string
  createTime?: string
}

export interface AiChatLogPageQuery extends PageQuery {
  username?: string
  provider?: string
  chatStatus?: number | null
  conversationId?: string
}

export function pageAiChatLog(params: AiChatLogPageQuery) {
  return get<PageResult<AiChatLogVO>>('/system/ai-log/page', params)
}

export function deleteAiChatLog(id: number | string) {
  return del(`/system/ai-log/${id}`)
}

export function cleanAiChatLog() {
  return del('/system/ai-log/clean')
}
