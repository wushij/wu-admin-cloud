<template>
  <view v-if="visible" class="security-modal" @click="emit('update:visible', false)">
    <view class="security-modal__card" @click.stop>
      <text class="security-modal__title">最近安全事件</text>

      <view v-if="subject" class="security-subject-summary">
        <DictTag
          :label="subject.principalType === 'user' ? '账号' : 'IP'"
          :effect="subject.principalType === 'user' ? 'primary' : 'default'"
        />
        <text class="security-risk__ip">{{ formatSubjectTitle(subject) }}</text>
        <text class="security-risk__score">{{ subject.riskScore }} 分 · {{ subject.riskLevelText }}</text>
      </view>

      <view v-for="item in events" :key="item.id" class="security-subject-event">
        <view class="security-subject-event__head">
          <DictTag :label="formatEventTypeName(item.eventType)" :effect="eventTypeTone(item.eventType)" />
          <text class="security-card__score">+{{ item.riskDelta }}</text>
        </view>
        <text class="security-card__reason">{{ item.reason || '—' }}</text>
        <view class="security-card__row">
          <text class="security-card__time">{{ formatDateTime(item.createTime, true) }}</text>
          <text class="security-card__time">累计 {{ item.totalRiskScore }} 分</text>
        </view>
      </view>

      <text v-if="loading" class="security-subject-loading">加载中…</text>
      <EmptyState
        v-if="!loading && !events.length"
        title="该主体最近没有安全事件"
        icon="shield-o"
      />

      <view class="security-modal__actions">
        <button class="security-action-btn" @click="emit('update:visible', false)">关闭</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import DictTag from '@/components/base/DictTag/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import type { SecurityEventVO, RiskSubjectVO } from '@/api/system/security'
import { formatDateTime } from '@/utils/display/format'
import { formatEventTypeName, eventTypeTone } from '../constants/security'

defineProps<{
  visible: boolean
  subject: RiskSubjectVO | null
  events: SecurityEventVO[]
  loading: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
}>()

function formatSubjectTitle(item: RiskSubjectVO) {
  if (item.principalType === 'user' && (item.username || item.nickname)) {
    const name = item.nickname ? `${item.nickname} (${item.username || ''})` : item.username
    return `${name} · ID: ${item.principalId}`
  }
  return item.principal
}
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.security-modal {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 32rpx;
  background: rgba(0, 0, 0, 0.55);
}

.security-modal__card {
  width: 100%;
  max-width: 620rpx;
  max-height: 80vh;
  padding: 36rpx;
  background: #ffffff;
  border-radius: $radius-lg;
  box-sizing: border-box;
  overflow-y: auto;
}

.security-modal__title {
  display: block;
  margin-bottom: 24rpx;
  font-size: $font-size-base;
  font-weight: 600;
  color: $color-text-primary;
}

.security-subject-summary {
  display: flex;
  align-items: center;
  gap: 12rpx;
  padding: 16rpx 0;
  border-bottom: 1px solid $color-border-light;
  margin-bottom: 20rpx;
}

.security-risk__ip {
  flex: 1;
  font-size: $font-size-xs;
  font-weight: 600;
  color: $color-text-primary;
}

.security-risk__score {
  font-size: $font-size-xs;
  font-weight: 600;
  color: $color-danger;
}

.security-subject-event {
  padding: 16rpx 0;
  border-bottom: 1px dashed $color-border-light;
}

.security-subject-event__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8rpx;
}

.security-card__score {
  font-size: $font-size-xs;
  font-weight: 700;
  color: $color-danger;
}

.security-card__reason {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-primary;
  margin-bottom: 8rpx;
}

.security-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.security-card__time {
  font-size: 20rpx;
  color: $color-text-muted;
}

.security-subject-loading {
  display: block;
  text-align: center;
  padding: 30rpx 0;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-modal__actions {
  display: flex;
  gap: 16rpx;
  margin-top: 32rpx;
}

.security-action-btn {
  flex: 1;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 68rpx;
  padding: 0 16rpx;
  margin: 0;
  font-size: $font-size-xs;
  color: $color-text-primary;
  background: #f1f5f9;
  border: none;
  border-radius: 999rpx;

  &::after {
    border: none;
  }
}
</style>

