import { ref, reactive, computed } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  pageAiKnowledge,
  deleteAiKnowledge,
  updateAiKnowledgeStatus,
  batchUpdateAiKnowledgeStatus,
  batchDeleteAiKnowledge,
  exportAllAiKnowledge,
  getAiKnowledgeStats,
  type AiKnowledgeVO,
  type AiKnowledgePageQuery,
} from '@/api/system/ai-knowledge'

export interface KnowledgeRowItem extends AiKnowledgeVO {
  _statusLoading?: boolean
}

export function useKnowledgeList() {
  const loading = ref(false)
  const total = ref(0)
  const tableData = ref<KnowledgeRowItem[]>([])
  const selectedRows = ref<KnowledgeRowItem[]>([])

  // 全量统计指标
  const stats = reactive({
    total: 0,
    activeCount: 0,
    disabledCount: 0,
    totalWords: 0,
  })

  const queryParams = reactive<AiKnowledgePageQuery>({
    pageNo: 1,
    pageSize: 10,
    keyword: '',
    category: '',
    status: null,
  })

  const activeCount = computed(() => stats.activeCount)
  const disabledCount = computed(() => stats.disabledCount)
  const totalWords = computed(() => stats.totalWords)

  async function fetchStats() {
    try {
      const res = await getAiKnowledgeStats()
      if (res.data) {
        stats.total = res.data.total ?? 0
        stats.activeCount = res.data.activeCount ?? 0
        stats.disabledCount = res.data.disabledCount ?? 0
        stats.totalWords = res.data.totalWords ?? 0
      }
    } catch {
      // 容错降级
      stats.total = total.value
      stats.activeCount = tableData.value.filter((k) => k.status === 1).length
      stats.disabledCount = tableData.value.filter((k) => k.status === 0).length
    }
  }

  async function loadData() {
    loading.value = true
    try {
      const [pageRes] = await Promise.all([
        pageAiKnowledge({
          pageNo: queryParams.pageNo,
          pageSize: queryParams.pageSize,
          keyword: queryParams.keyword || undefined,
          category: queryParams.category || undefined,
          status: queryParams.status,
        }),
        fetchStats(),
      ])
      tableData.value = (pageRes.data?.list || []).map((item) => ({ ...item, _statusLoading: false }))
      total.value = Number(pageRes.data?.total) || 0
      if (!stats.total) stats.total = total.value
    } finally {
      loading.value = false
    }
  }

  function handleQuery() {
    queryParams.pageNo = 1
    loadData()
  }

  function resetQuery() {
    queryParams.keyword = ''
    queryParams.category = ''
    queryParams.status = null
    handleQuery()
  }

  function handleSelectionChange(rows: KnowledgeRowItem[]) {
    selectedRows.value = rows
  }

  // 行内快速启停开关
  async function handleStatusChange(row: KnowledgeRowItem, newStatus: number) {
    row._statusLoading = true
    try {
      await updateAiKnowledgeStatus(row.id, newStatus)
      ElMessage.success(newStatus === 1 ? '已生效启用' : '已停用该知识')
    } catch {
      // 失败回滚
      row.status = newStatus === 1 ? 0 : 1
    } finally {
      row._statusLoading = false
    }
  }

  // 批量修改状态
  async function handleBatchStatus(status: number) {
    const ids = selectedRows.value.map((r) => r.id)
    if (ids.length === 0) return
    await ElMessageBox.confirm(
      `确定要将选中的 ${ids.length} 项知识批量设置为「${status === 1 ? '启用' : '停用'}」吗？`,
      '提示',
      { type: 'warning' }
    )
    await batchUpdateAiKnowledgeStatus(ids, status)
    ElMessage.success('批量修改成功')
    loadData()
  }

  // 批量删除
  async function handleBatchDelete() {
    const ids = selectedRows.value.map((r) => r.id)
    if (ids.length === 0) return
    await ElMessageBox.confirm(`确定要彻底删除选中的 ${ids.length} 项知识吗？`, '警告', {
      type: 'warning',
      confirmButtonText: '确定删除',
    })
    await batchDeleteAiKnowledge(ids)
    ElMessage.success('批量删除成功')
    loadData()
  }

  // 单条删除
  async function handleDelete(row: AiKnowledgeVO) {
    await ElMessageBox.confirm(`确定要删除知识「${row.title}」吗？`, '提示', { type: 'warning' })
    await deleteAiKnowledge(row.id)
    ElMessage.success('删除成功')
    loadData()
  }

  // 导出全部知识
  async function handleExport() {
    try {
      const res = await exportAllAiKnowledge()
      const list = res.data || []
      const blob = new Blob([JSON.stringify(list, null, 2)], { type: 'application/json' })
      const url = URL.createObjectURL(blob)
      const a = document.createElement('a')
      a.href = url
      a.download = `ai-knowledge-export-${new Date().toISOString().slice(0, 10)}.json`
      a.click()
      URL.revokeObjectURL(url)
      ElMessage.success(`成功导出 ${list.length} 条知识`)
    } catch {
      ElMessage.error('导出失败')
    }
  }

  return {
    loading,
    total,
    tableData,
    selectedRows,
    queryParams,
    activeCount,
    disabledCount,
    totalWords,
    loadData,
    handleQuery,
    resetQuery,
    handleSelectionChange,
    handleStatusChange,
    handleBatchStatus,
    handleBatchDelete,
    handleDelete,
    handleExport,
  }
}
