<template>
  <div class="job-hero">
    <div class="job-hero-left">
      <h2 class="job-hero-title">
        <ModulePageIcon :icon="MODULE_PAGE_ICON.job" class="job-hero-title-icon" />
        定时任务调度中心
      </h2>
      <p class="job-hero-desc">基于 Quartz 管理维护任务：日志归档、聊天消息清理、通知与回收站清理等</p>
      <div class="job-hero-tags">
        <el-tag type="info" effect="plain">任务 {{ overview.totalJobs ?? 0 }}</el-tag>
        <el-tag type="success" effect="plain">运行中 {{ overview.runningJobs ?? 0 }}</el-tag>
        <el-tag type="warning" effect="plain">已暂停 {{ overview.pausedJobs ?? 0 }}</el-tag>
      </div>
    </div>
    <el-row :gutter="12" class="job-hero-stats">
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat">
          <div class="job-hero-stat-value">{{ overview.totalCount ?? 0 }}</div>
          <div class="job-hero-stat-label">累计执行</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat success">
          <div class="job-hero-stat-value">{{ overview.successCount ?? 0 }}</div>
          <div class="job-hero-stat-label">成功</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat fail">
          <div class="job-hero-stat-value">{{ overview.failCount ?? 0 }}</div>
          <div class="job-hero-stat-label">失败</div>
        </div>
      </el-col>
      <el-col :span="6" :xs="12">
        <div class="job-hero-stat rate">
          <div class="job-hero-stat-value">{{ overview.successRate ?? 100 }}%</div>
          <div class="job-hero-stat-label">成功率</div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup lang="ts">
import type { JobOverview } from '@/api/monitor/job'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'

defineProps<{ overview: JobOverview }>()
</script>

<style scoped>
.job-hero {
  display: flex;
  flex-wrap: wrap;
  gap: 20px;
  justify-content: space-between;
  align-items: center;
  padding: 24px 28px;
  margin-bottom: 16px;
  border-radius: 12px;
  background: linear-gradient(135deg, var(--theme-primary, #111827) 0%, #374151 100%);
  color: #fff;
}
.job-hero-left {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  flex: 1;
  min-width: 280px;
}
.job-hero-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin: 0 0 8px;
  font-size: 22px;
  font-weight: 700;
}
.job-hero-title-icon {
  flex-shrink: 0;
}
.job-hero-desc {
  margin: 0 0 12px;
  font-size: 13px;
  opacity: 0.85;
  max-width: 480px;
  line-height: 1.5;
}
.job-hero-tags {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}
.job-hero-stats {
  flex: 1;
  min-width: 280px;
  max-width: 520px;
}
.job-hero-stat {
  text-align: center;
  padding: 12px 8px;
  border-radius: 10px;
  background: rgba(255, 255, 255, 0.12);
}
.job-hero-stat-value {
  font-size: 24px;
  font-weight: 700;
}
.job-hero-stat-label {
  font-size: 12px;
  opacity: 0.8;
  margin-top: 4px;
}
.job-hero-stat.success .job-hero-stat-value { color: #95de64; }
.job-hero-stat.fail .job-hero-stat-value { color: #ff9c9c; }
.job-hero-stat.rate .job-hero-stat-value { color: #91d5ff; }
</style>
