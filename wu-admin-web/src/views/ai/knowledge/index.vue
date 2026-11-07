<template>
  <div class="app-container module-page ai-knowledge-page">
    <!-- 顶部统一风格深色 Hero 头图卡片（对齐系统全局与 AI 模型配置规范） -->
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.aiKnowledge" />
            <span>AI 知识库</span>
          </div>
          <p class="module-hero-desc">
            管理 AI wu助手检索的企业知识文档与问答库，问答时优先基于本知识库内容精准回答
          </p>
        </div>
        <div class="hero-stats-group">
          <div class="module-hero-stats">
            <div class="stat-num">{{ total }}</div>
            <div class="stat-label">文档总数</div>
          </div>
          <div class="module-hero-stats">
            <div class="stat-num text-success">{{ activeCount }}</div>
            <div class="stat-label">生效启用</div>
          </div>
          <div class="module-hero-stats">
            <div class="stat-num text-muted">{{ disabledCount }}</div>
            <div class="stat-label">已停用</div>
          </div>
          <div class="module-hero-stats">
            <div class="stat-num text-primary">{{ formattedWords }}</div>
            <div class="stat-label">知识总字数</div>
          </div>
        </div>
      </div>
    </el-card>

    <!-- 文档检索与状态筛选条（系统统一 inline 表单规范） -->
    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form" @submit.prevent>
        <el-form-item label="文档检索">
          <el-input
            v-model="queryParams.keyword"
            placeholder="搜索 Markdown 文档名称或正文关键词"
            clearable
            style="width: 260px"
            @keyup.enter="handleQuery"
          >
            <template #prefix>
              <el-icon><Search /></el-icon>
            </template>
          </el-input>
        </el-form-item>
        <el-form-item label="分类">
          <el-select v-model="queryParams.category" placeholder="全部分类" clearable style="width: 140px">
            <el-option v-for="(label, key) in CATEGORY_LABELS" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="已生效" :value="1" />
            <el-option label="已停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- Markdown 文档清单列表主卡片 -->
    <el-card shadow="never">
      <template #header>
        <div class="card-header">
          <div class="header-left">
            <span>Markdown 文档清单</span>
            <transition name="el-fade-in">
              <div v-if="selectedRows.length > 0" class="batch-bar">
                <span class="batch-tip">已选中 <b>{{ selectedRows.length }}</b> 篇文档</span>
                <el-button
                  v-permission="'system:ai-knowledge:update'"
                  size="small"
                  type="success"
                  plain
                  @click="handleBatchStatus(1)"
                >
                  批量启用
                </el-button>
                <el-button
                  v-permission="'system:ai-knowledge:update'"
                  size="small"
                  type="warning"
                  plain
                  @click="handleBatchStatus(0)"
                >
                  批量停用
                </el-button>
                <el-button
                  v-permission="'system:ai-knowledge:delete'"
                  size="small"
                  type="danger"
                  plain
                  @click="handleBatchDelete"
                >
                  批量删除
                </el-button>
              </div>
            </transition>
          </div>
          <div class="header-actions">
            <el-button
              v-permission="'system:ai-knowledge:create'"
              type="primary"
              :icon="Upload"
              @click="openUploadDialog"
            >
              上传 MD 文档
            </el-button>
            <el-button
              v-permission="'system:ai-knowledge:create'"
              :icon="Plus"
              @click="openCreate"
            >
              新建文档
            </el-button>
          </div>
        </div>
      </template>

      <!-- Markdown 文档表格 -->
      <el-table
        v-loading="loading"
        :data="tableData"
        row-key="id"
        class="doc-table"
        @selection-change="handleSelectionChange"
      >
        <el-table-column type="selection" width="46" align="center" />

        <!-- 文档名称列：MD 图标徽章 + 标题 + 正文轻量摘要 -->
        <el-table-column label="文档名称" min-width="320" align="left">
          <template #default="{ row }">
            <div class="doc-title-row" @click="openView(row)">
              <div class="md-badge-icon">
                <span>MD</span>
              </div>
              <div class="doc-text-group">
                <span class="doc-title-text">{{ row.title }}</span>
                <span class="doc-summary-text">{{ getCleanSnippet(row.content) }}</span>
              </div>
            </div>
          </template>
        </el-table-column>

        <!-- 规章分类 -->
        <el-table-column label="规章分类" width="130" align="center">
          <template #default="{ row }">
            <el-tag
              size="small"
              :type="getCategoryTagType(row.category)"
              effect="plain"
              class="category-tag"
            >
              {{ CATEGORY_LABELS[row.category] || row.category || '通用制度' }}
            </el-tag>
          </template>
        </el-table-column>

        <!-- 文档字数 -->
        <el-table-column label="文档字数" width="120" align="center">
          <template #default="{ row }">
            <span class="words-pill">{{ (row.content || '').length.toLocaleString() }} 字</span>
          </template>
        </el-table-column>

        <!-- 检索生效 Switch 开关 -->
        <el-table-column label="检索生效" width="110" align="center">
          <template #default="{ row }">
            <el-switch
              :model-value="row.status === 1"
              :loading="row._statusLoading"
              inline-prompt
              active-text="生效"
              inactive-text="停用"
              active-color="#10b981"
              @change="(val) => handleStatusChange(row, val ? 1 : 0)"
            />
          </template>
        </el-table-column>

        <!-- 更新时间 -->
        <el-table-column prop="updateTime" label="更新时间" width="170" align="center">
          <template #default="{ row }">
            <span class="time-text">{{ row.updateTime || row.createTime || '—' }}</span>
          </template>
        </el-table-column>

        <!-- 操作按钮列 -->
        <el-table-column label="操作" width="190" align="center" fixed="right">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button
                type="primary"
                link
                size="small"
                :icon="View"
                @click="openView(row)"
              >
                预览
              </el-button>
              <el-button
                v-permission="'system:ai-knowledge:update'"
                type="primary"
                link
                size="small"
                :icon="Edit"
                @click="openEdit(row)"
              >
                编辑
              </el-button>
              <el-button
                v-permission="'system:ai-knowledge:delete'"
                type="danger"
                link
                size="small"
                :icon="Delete"
                @click="handleDelete(row)"
              >
                删除
              </el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 分页栏 -->
      <div class="table-pagination">
        <el-pagination
          v-model:current-page="queryParams.pageNo"
          v-model:page-size="queryParams.pageSize"
          :total="total"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next, jumper"
          @size-change="loadData"
          @current-change="loadData"
        />
      </div>
    </el-card>

    <!-- Markdown (.md) 文档多文件批量拖拽上传对话框 -->
    <KnowledgeMdUploadDialog
      v-model="uploadDialogVisible"
      @success="loadData"
    />

    <!-- Markdown 文档沉浸式阅读与排版预览抽屉 -->
    <KnowledgeDocPreviewDrawer
      v-model="viewVisible"
      :view-row="viewRow"
      @edit="handlePreviewEdit"
    />

    <!-- Markdown 文档在线创建与实时分屏编辑抽屉 -->
    <KnowledgeEditDrawer
      v-model="editVisible"
      v-model:form-ref="formRef"
      v-model:editor-layout="editorLayout"
      :form="form"
      :rules="rules"
      :saving="saving"
      :fence-count="fenceCount"
      @auto-keywords="autoExtractKeywords"
      @insert-template="insertTemplate"
      @insert-fence="insertFence"
      @submit="handleSubmit"
    />
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import { Search, Refresh, Plus, Upload, View, Edit, Delete } from '@element-plus/icons-vue'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import type { AiKnowledgeVO } from '@/api/system/ai-knowledge'
import { CATEGORY_LABELS, getCategoryTagType } from './constants/knowledge'
import { useKnowledgeList } from './composables/useKnowledgeList'
import { useKnowledgeEdit } from './composables/useKnowledgeEdit'
import KnowledgeMdUploadDialog from './components/KnowledgeMdUploadDialog.vue'
import KnowledgeDocPreviewDrawer from './components/KnowledgeDocPreviewDrawer.vue'
import KnowledgeEditDrawer from './components/KnowledgeEditDrawer.vue'

// 列表数据流、统计指标与批量操作
const {
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
} = useKnowledgeList()

// Markdown 文档在线新建/编辑
const {
  form,
  editVisible,
  saving,
  formRef,
  rules,
  fenceCount,
  editorLayout,
  openCreate,
  openEdit,
  handleSubmit,
  autoExtractKeywords,
  insertTemplate,
  insertFence,
} = useKnowledgeEdit(() => loadData())

// 上传 Markdown 对话框
const uploadDialogVisible = ref(false)
function openUploadDialog() {
  uploadDialogVisible.value = true
}

// 预览查看抽屉
const viewVisible = ref(false)
const viewRow = ref<AiKnowledgeVO | null>(null)

function openView(row: AiKnowledgeVO) {
  viewRow.value = row
  viewVisible.value = true
}

function handlePreviewEdit(row: AiKnowledgeVO) {
  viewVisible.value = false
  openEdit(row)
}

// 格式化总字数显示
const formattedWords = computed(() => {
  const words = totalWords.value || 0
  if (words >= 10000) {
    return (words / 10000).toFixed(1) + 'w'
  }
  if (words >= 1000) {
    return (words / 1000).toFixed(1) + 'k'
  }
  return String(words)
})

// 提取清洗后的 Markdown 纯文本摘要
function getCleanSnippet(content?: string): string {
  if (!content) return '暂无正文描述'
  const text = content
    .replace(/^#+\s+/gm, '')
    .replace(/[*_`~>\[\]\(\)]/g, '')
    .replace(/```[\s\S]*?```/g, ' [代码块] ')
    .replace(/\s+/g, ' ')
    .trim()
  if (text.length > 70) {
    return text.slice(0, 70) + '...'
  }
  return text || '暂无正文描述'
}

onMounted(() => {
  loadData()
})
</script>

<style scoped lang="scss">
/* 统计指标胶囊组 */
.hero-stats-group {
  display: flex;
  align-items: center;
  gap: 24px;

  .module-hero-stats {
    text-align: center;
    min-width: 72px;

    .stat-num {
      font-size: 26px;
      font-weight: 700;
      line-height: 1.2;

      &.text-success {
        color: #34d399;
      }

      &.text-muted {
        color: #94a3b8;
      }

      &.text-primary {
        color: #60a5fa;
      }
    }

    .stat-label {
      margin-top: 4px;
      font-size: 12px;
      opacity: 0.82;
    }
  }
}

.card-header {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .header-left {
    display: flex;
    align-items: center;
    gap: 16px;
    font-weight: 600;
  }
}

.batch-bar {
  display: flex;
  align-items: center;
  gap: 8px;
  background: var(--el-color-primary-light-9, #eff6ff);
  border: 1px solid var(--el-color-primary-light-7, #bfdbfe);
  padding: 3px 12px;
  border-radius: 6px;

  .batch-tip {
    font-size: 12.5px;
    color: var(--el-color-primary, #2563eb);
    margin-right: 4px;

    b {
      font-weight: 700;
    }
  }
}

.doc-title-row {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
  padding: 4px 0;

  .md-badge-icon {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    background: #f1f5f9;
    border: 1px solid #e2e8f0;
    color: #475569;
    display: flex;
    align-items: center;
    justify-content: center;
    font-weight: 700;
    font-size: 11px;
    flex-shrink: 0;
    transition: all 0.2s ease;
  }

  &:hover .md-badge-icon {
    background: #e2e8f0;
    color: #1e293b;
    border-color: #cbd5e1;
  }

  .doc-text-group {
    display: flex;
    flex-direction: column;
    gap: 3px;
    overflow: hidden;

    .doc-title-text {
      font-size: 14px;
      font-weight: 600;
      color: #0f172a;
      transition: color 0.15s ease;

      &:hover {
        color: var(--el-color-primary, #2563eb);
      }
    }

    .doc-summary-text {
      font-size: 12px;
      line-height: 1.5;
      color: #64748b;
      white-space: nowrap;
      overflow: hidden;
      text-overflow: ellipsis;
    }
  }
}

.category-tag {
  font-weight: 500;
  border-radius: 6px;
}

.words-pill {
  font-size: 12.5px;
  font-family: ui-monospace, SFMono-Regular, Menlo, Monaco, Consolas, monospace;
  color: #64748b;
  background: #f8fafc;
  padding: 3px 8px;
  border-radius: 6px;
  border: 1px solid #f1f5f9;
}

.time-text {
  font-size: 12.5px;
  color: #64748b;
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
  white-space: nowrap;
}
</style>
