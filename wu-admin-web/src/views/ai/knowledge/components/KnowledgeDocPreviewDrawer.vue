<template>
  <el-drawer
    :model-value="modelValue"
    :title="viewRow?.title || '文档阅读预览'"
    size="720px"
    destroy-on-close
    class="doc-preview-drawer"
    @update:model-value="(val) => $emit('update:modelValue', val)"
  >
    <div v-if="viewRow" class="preview-drawer-content">
      <!-- 顶部属性条 -->
      <div class="doc-meta-banner">
        <div class="meta-tags-left">
          <el-tag size="small" :type="getCategoryTagType(viewRow.category)" effect="light">
            {{ CATEGORY_LABELS[viewRow.category || ''] || viewRow.category || '规章手册' }}
          </el-tag>
          <span class="meta-pill">
            <el-icon><Tickets /></el-icon>
            {{ (viewRow.content || '').length.toLocaleString() }} 字符
          </span>
          <span class="meta-pill">
            <el-icon><Clock /></el-icon>
            更新于 {{ viewRow.updateTime || viewRow.createTime || '近期' }}
          </span>
        </div>
        <div class="meta-status-right">
          <el-tag size="small" :type="viewRow.status === 1 ? 'success' : 'info'" effect="dark">
            {{ viewRow.status === 1 ? '检索生效中' : '已停用' }}
          </el-tag>
        </div>
      </div>

      <!-- 快速动作条 -->
      <div class="doc-quick-toolbar">
        <el-button size="small" :icon="DocumentCopy" @click="copyContent">
          复制 Markdown 原文
        </el-button>
        <el-button
          v-permission="'system:ai-knowledge:update'"
          size="small"
          type="primary"
          plain
          :icon="Edit"
          @click="$emit('edit', viewRow)"
        >
          编辑该文档
        </el-button>
      </div>

      <!-- Markdown 渲染核心区 -->
      <div class="doc-markdown-render-box">
        <AiWuMarkdown :content="viewRow.content" />
      </div>
    </div>
  </el-drawer>
</template>

<script setup lang="ts">
import { ElMessage } from 'element-plus'
import { DocumentCopy, Edit, Tickets, Clock } from '@element-plus/icons-vue'
import AiWuMarkdown from '@/components/AiWu/AiWuMarkdown.vue'
import type { AiKnowledgeVO } from '@/api/system/ai-knowledge'
import { CATEGORY_LABELS, getCategoryTagType } from '../constants/knowledge'

const props = defineProps<{
  modelValue: boolean
  viewRow: AiKnowledgeVO | null
}>()

defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'edit', row: AiKnowledgeVO): void
}>()

async function copyContent() {
  if (!props.viewRow?.content) return
  try {
    await navigator.clipboard.writeText(props.viewRow.content)
    ElMessage.success('已复制 Markdown 原文到剪贴板')
  } catch {
    ElMessage.error('复制失败，请手动选择复制')
  }
}
</script>

<style scoped lang="scss">
.preview-drawer-content {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding: 0 4px;
}

.doc-meta-banner {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 12px 16px;
  background: #f8fafc;
  border-radius: 8px;
  border: 1px solid #e2e8f0;

  .meta-tags-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex-wrap: wrap;

    .meta-pill {
      display: inline-flex;
      align-items: center;
      gap: 4px;
      font-size: 12px;
      color: #64748b;
    }
  }
}

.doc-quick-toolbar {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
}

.doc-markdown-render-box {
  background: #ffffff;
  border: 1px solid #f1f5f9;
  border-radius: 10px;
  padding: 24px;
  min-height: 400px;
  line-height: 1.7;
  color: #1e293b;
}
</style>
