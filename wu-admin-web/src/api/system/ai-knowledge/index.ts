import { get, post, put, del } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

/** 知识分类 */
export type AiKnowledgeCategory = 'faq' | 'manual' | 'module' | 'other'

export interface AiKnowledgeVO {
  id: number
  title: string
  keywords?: string
  content: string
  category?: AiKnowledgeCategory | string
  sort?: number
  status?: number
  creator?: string
  updater?: string
  createTime?: string
  updateTime?: string
}

export interface AiKnowledgeSaveDTO {
  id?: number
  title: string
  keywords?: string
  content: string
  category?: string
  sort?: number
  status?: number
}

export interface AiKnowledgePageQuery extends PageQuery {
  keyword?: string
  category?: string
  status?: number | null
}

export interface AiKnowledgeTestRetrieveResult {
  query: string
  searchTerms: string[]
  matchMode: 'FULLTEXT' | 'LIKE' | 'NONE'
  hitList: AiKnowledgeVO[]
  referenceBlock: string
  charCount: number
  estimatedTokens: number
}

export function pageAiKnowledge(params: AiKnowledgePageQuery) {
  return get<PageResult<AiKnowledgeVO>>('/ai/knowledge/page', params)
}

export function getAiKnowledge(id: number) {
  return get<AiKnowledgeVO>(`/ai/knowledge/${id}`)
}

export function createAiKnowledge(data: AiKnowledgeSaveDTO) {
  return post('/ai/knowledge', data)
}

export function updateAiKnowledge(data: AiKnowledgeSaveDTO) {
  return put('/ai/knowledge', data)
}

export function deleteAiKnowledge(id: number) {
  return del(`/ai/knowledge/${id}`)
}

/** 单条修改启用/停用状态 */
export function updateAiKnowledgeStatus(id: number, status: number) {
  return put(`/ai/knowledge/${id}/status?status=${status}`)
}

/** 批量修改状态 */
export function batchUpdateAiKnowledgeStatus(ids: number[], status: number) {
  return put('/ai/knowledge/batch-status', { ids, status })
}

/** 批量删除 */
export function batchDeleteAiKnowledge(ids: number[]) {
  return del('/ai/knowledge/batch', { data: ids })
}

/** 知识库检索演练与召回测试 */
export function testRetrieveAiKnowledge(query: string) {
  return post<AiKnowledgeTestRetrieveResult>('/ai/knowledge/test-retrieve', { query })
}

/** 批量导入知识条目 */
export function batchImportAiKnowledge(list: AiKnowledgeSaveDTO[]) {
  return post('/ai/knowledge/batch-import', list)
}

/** 导出全量知识列表 */
export function exportAllAiKnowledge() {
  return get<AiKnowledgeVO[]>('/ai/knowledge/export')
}

/** 上传 Markdown (.md) 文件批量入库 */
export function uploadAiKnowledgeMd(files: File[]) {
  const formData = new FormData()
  for (const f of files) {
    formData.append('files', f)
  }
  return post<{ total: number; created: number; updated: number; files: string[] }>('/ai/knowledge/upload-md', formData, {
    headers: { 'Content-Type': 'multipart/form-data' }
  })
}

/** 获取知识库统计指标 */
export function getAiKnowledgeStats() {
  return get<{ total: number; activeCount: number; disabledCount: number; totalWords: number }>('/ai/knowledge/stats')
}

