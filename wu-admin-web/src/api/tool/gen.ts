import { del, get, post, put } from '@/utils/request'
import type { PageQuery, PageResult } from '@/types/api'

export interface DatabaseTable {
  tableName: string
  tableComment: string
  createTime?: string
  updateTime?: string
}

export interface GenTableColumn {
  id?: number
  tableId?: number
  columnName: string
  columnComment: string
  columnType: string
  javaType: string
  javaField: string
  isPk: number
  isIncrement: number
  isRequired: number
  isInsert: number
  isEdit: number
  isList: number
  isQuery: number
  queryType: string
  htmlType: string
  dictType?: string
  sort: number
}

export interface GenTable {
  id?: number
  tableName: string
  tableComment: string
  className: string
  packageName: string
  moduleName: string
  businessName: string
  functionName: string
  author: string
  genType: string
  genPath?: string
  frontType: string
  formLayout?: string
  parentMenuId?: number
  remark?: string
  createTime?: string
  updateTime?: string
  columns?: GenTableColumn[]
}

export interface GenPageQuery extends PageQuery {
  tableName?: string
}

export function dbTableList(params: { pageNo: number; pageSize: number; tableName?: string }) {
  return get<PageResult<DatabaseTable>>('/infra/codegen/db/list', params)
}

export function importGenTables(tableNames: string[]) {
  return post<boolean>('/infra/codegen/import', tableNames)
}

export function pageGenTable(params: GenPageQuery) {
  return get<PageResult<GenTable>>('/infra/codegen/page', params)
}

export function getGenTable(id: number) {
  return get<GenTable>(`/infra/codegen/${id}`)
}

export function updateGenTable(data: GenTable) {
  return put<boolean>('/infra/codegen', data)
}

export function deleteGenTable(ids: number[]) {
  return del<boolean>(`/infra/codegen/${ids.join(',')}`)
}

export function previewGenCode(id: number) {
  return get<Record<string, string>>(`/infra/codegen/preview/${id}`)
}

export function previewGenerateFiles(id: number) {
  return get<string[]>(`/infra/codegen/preview-generate/${id}`)
}

export function generateToProject(id: number, overwrite = false) {
  return post<string[]>(`/infra/codegen/generate/${id}?overwrite=${overwrite}`)
}

export function previewRemoveFiles(id: number) {
  return get<string[]>(`/infra/codegen/preview-remove/${id}`)
}

export function removeGeneratedCode(id: number) {
  return del<string[]>(`/infra/codegen/remove-code/${id}`)
}

export function syncGenTable(id: number) {
  return post<boolean>(`/infra/codegen/sync/${id}`)
}

export function downloadGenCodeUrl(ids: number[]) {
  // 不带 /api 前缀，由 axios baseURL 统一拼接，避免重复拼接为 /api/api/...
  return `/infra/codegen/download?ids=${ids.join(',')}`
}

export function getRecycleGenPage(params: GenPageQuery) {
  return get<PageResult<GenTable>>('/infra/codegen/recycle/page', params)
}

export function restoreGenTable(id: number) {
  return put<boolean>('/infra/codegen/restore', null, { params: { id } })
}

export function deleteGenTablePermanent(id: number) {
  return del<boolean>('/infra/codegen/delete-permanent', { params: { id } })
}
