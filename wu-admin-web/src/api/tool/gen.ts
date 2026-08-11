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
  return get<PageResult<DatabaseTable>>('/system/tool/gen/db/list', params)
}

export function importGenTables(tableNames: string[]) {
  return post<boolean>('/system/tool/gen/import', tableNames)
}

export function pageGenTable(params: GenPageQuery) {
  return get<PageResult<GenTable>>('/system/tool/gen/page', params)
}

export function getGenTable(id: number) {
  return get<GenTable>(`/system/tool/gen/${id}`)
}

export function updateGenTable(data: GenTable) {
  return put<boolean>('/system/tool/gen', data)
}

export function deleteGenTable(ids: number[]) {
  return del<boolean>(`/system/tool/gen/${ids.join(',')}`)
}

export function previewGenCode(id: number) {
  return get<Record<string, string>>(`/system/tool/gen/preview/${id}`)
}

export function previewGenerateFiles(id: number) {
  return get<string[]>(`/system/tool/gen/preview-generate/${id}`)
}

export function generateToProject(id: number, overwrite = false) {
  return post<string[]>(`/system/tool/gen/generate/${id}?overwrite=${overwrite}`)
}

export function previewRemoveFiles(id: number) {
  return get<string[]>(`/system/tool/gen/preview-remove/${id}`)
}

export function removeGeneratedCode(id: number) {
  return del<string[]>(`/system/tool/gen/remove-code/${id}`)
}

export function syncGenTable(id: number) {
  return post<boolean>(`/system/tool/gen/sync/${id}`)
}

export function downloadGenCodeUrl(ids: number[]) {
  // 不带 /api 前缀，由 axios baseURL 统一拼接，避免重复拼接为 /api/api/...
  return `/system/tool/gen/download?ids=${ids.join(',')}`
}

export function getRecycleGenPage(params: GenPageQuery) {
  return get<PageResult<GenTable>>('/system/tool/gen/recycle/page', params)
}

export function restoreGenTable(id: number) {
  return put<boolean>('/system/tool/gen/restore', null, { params: { id } })
}

export function deleteGenTablePermanent(id: number) {
  return del<boolean>('/system/tool/gen/delete-permanent', { params: { id } })
}
