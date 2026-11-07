<template>
  <el-card class="search-card knowledge-filter-card" shadow="never">
    <el-form :model="queryParams" inline class="filter-form" @submit.prevent>
      <el-form-item label="文档检索">
        <el-input
          v-model="queryParams.keyword"
          placeholder="搜索 Markdown 文档名称或正文关键词"
          clearable
          style="width: 280px"
          @keyup.enter="$emit('search')"
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
        <el-button type="primary" :icon="Search" @click="$emit('search')">查询</el-button>
        <el-button :icon="Refresh" @click="$emit('reset')">重置</el-button>
      </el-form-item>
    </el-form>
  </el-card>
</template>

<script setup lang="ts">
import { Search, Refresh } from '@element-plus/icons-vue'
import type { AiKnowledgePageQuery } from '@/api/system/ai-knowledge'
import { CATEGORY_LABELS } from '../constants/knowledge'

defineProps<{
  queryParams: AiKnowledgePageQuery
}>()

defineEmits<{
  (e: 'search'): void
  (e: 'reset'): void
}>()
</script>

<style scoped lang="scss">
.knowledge-filter-card {
  margin-bottom: 16px;
  border-radius: 12px;
  border: 1px solid #e2e8f0;
  background: #ffffff;

  :deep(.el-card__body) {
    padding: 16px 20px 0;
  }
}

.filter-form {
  :deep(.el-form-item) {
    margin-bottom: 16px;
    margin-right: 20px;
  }
}
</style>
