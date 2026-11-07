<template>
  <el-card class="knowledge-header-card" shadow="never">
    <div class="header-card-row">
      <!-- 模块标题与业务说明 -->
      <div class="header-main-info">
        <div class="header-title-box">
          <div class="icon-wrapper">
            <el-icon class="header-icon"><Reading /></el-icon>
          </div>
          <span class="header-title">AI 知识库</span>
          <el-tag size="small" type="primary" effect="light" class="header-badge">
            Markdown 业务文档
          </el-tag>
        </div>
        <p class="header-desc">
          支持上传企业制度、操作手册等 Markdown (.md) 格式文档；AI 问答助手在回答时将优先检索本知识库内容并依此精准答复。
        </p>
      </div>

      <!-- 核心指标统计胶囊 -->
      <div class="header-stats-group">
        <div class="stat-pill">
          <div class="stat-icon-wrap bg-blue-subtle">
            <el-icon><Document /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value">{{ total }}</div>
            <div class="stat-label">文档总数</div>
          </div>
        </div>

        <div class="stat-pill">
          <div class="stat-icon-wrap bg-green-subtle">
            <el-icon><CircleCheckFilled /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value text-success">{{ activeCount }}</div>
            <div class="stat-label">生效启用</div>
          </div>
        </div>

        <div class="stat-pill">
          <div class="stat-icon-wrap bg-purple-subtle">
            <el-icon><Tickets /></el-icon>
          </div>
          <div class="stat-content">
            <div class="stat-value text-primary">{{ formattedWords }}</div>
            <div class="stat-label">知识总字数</div>
          </div>
        </div>
      </div>

      <!-- 操作动作按钮 -->
      <div class="header-actions">
        <el-button
          v-permission="'system:ai-knowledge:create'"
          type="primary"
          class="action-btn-primary"
          @click="$emit('open-upload')"
        >
          <el-icon><Upload /></el-icon>
          <span>上传 MD 文档</span>
        </el-button>

        <el-button
          v-permission="'system:ai-knowledge:create'"
          class="action-btn-default"
          @click="$emit('open-create')"
        >
          <el-icon><Plus /></el-icon>
          <span>新建文档</span>
        </el-button>
      </div>
    </div>
  </el-card>
</template>

<script setup lang="ts">
import { computed } from 'vue'
import {
  Reading,
  Upload,
  Plus,
  Document,
  CircleCheckFilled,
  Tickets,
} from '@element-plus/icons-vue'

const props = defineProps<{
  total: number
  activeCount: number
  disabledCount: number
  totalWords?: number
}>()

defineEmits<{
  (e: 'open-upload'): void
  (e: 'open-create'): void
}>()

const formattedWords = computed(() => {
  const words = props.totalWords || 0
  if (words >= 10000) {
    return (words / 10000).toFixed(1) + 'w'
  }
  if (words >= 1000) {
    return (words / 1000).toFixed(1) + 'k'
  }
  return String(words)
})
</script>

<style scoped lang="scss">
.knowledge-header-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.04);
  transition: all 0.2s ease;

  :deep(.el-card__body) {
    padding: 20px 24px;
  }
}

.header-card-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 24px;
  flex-wrap: wrap;
}

.header-main-info {
  flex: 1;
  min-width: 320px;
}

.header-title-box {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 8px;

  .icon-wrapper {
    width: 38px;
    height: 38px;
    border-radius: 10px;
    background: linear-gradient(135deg, #eff6ff 0%, #dbeafe 100%);
    border: 1px solid #bfdbfe;
    display: flex;
    align-items: center;
    justify-content: center;

    .header-icon {
      font-size: 20px;
      color: #2563eb;
    }
  }

  .header-title {
    font-size: 19px;
    font-weight: 700;
    color: #0f172a;
    letter-spacing: -0.01em;
  }

  .header-badge {
    font-weight: 500;
    border-radius: 6px;
  }
}

.header-desc {
  font-size: 13.5px;
  line-height: 1.6;
  color: #64748b;
  margin: 0;
  max-width: 680px;
}

.header-stats-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.stat-pill {
  display: flex;
  align-items: center;
  gap: 10px;
  padding: 8px 16px;
  border-radius: 10px;
  background: #f8fafc;
  border: 1px solid #f1f5f9;

  .stat-icon-wrap {
    width: 32px;
    height: 32px;
    border-radius: 8px;
    display: flex;
    align-items: center;
    justify-content: center;
    font-size: 16px;

    &.bg-blue-subtle {
      background: #eff6ff;
      color: #3b82f6;
    }
    &.bg-green-subtle {
      background: #ecfdf5;
      color: #10b981;
    }
    &.bg-purple-subtle {
      background: #f5f3ff;
      color: #8b5cf6;
    }
  }

  .stat-content {
    display: flex;
    flex-direction: column;

    .stat-value {
      font-size: 17px;
      font-weight: 700;
      color: #1e293b;
      line-height: 1.2;

      &.text-success {
        color: #10b981;
      }
      &.text-primary {
        color: #3b82f6;
      }
    }

    .stat-label {
      font-size: 11.5px;
      color: #94a3b8;
      margin-top: 2px;
    }
  }
}

.header-actions {
  display: flex;
  align-items: center;
  gap: 10px;

  .action-btn-primary {
    height: 38px;
    padding: 0 18px;
    font-weight: 600;
    border-radius: 8px;
    box-shadow: 0 2px 6px rgba(37, 99, 235, 0.2);
  }

  .action-btn-default {
    height: 38px;
    padding: 0 16px;
    font-weight: 500;
    border-radius: 8px;
  }
}

@media (max-width: 1024px) {
  .header-card-row {
    flex-direction: column;
    align-items: flex-start;
  }
  .header-actions {
    width: 100%;
    justify-content: flex-end;
  }
}
</style>
