<template>
  <view v-if="visible && event" class="security-modal" @click="emit('update:visible', false)">
    <view class="security-modal__card" @click.stop>
      <text class="security-modal__title">安全拦截详情</text>

      <view class="security-detail__row">
        <text class="security-detail__label">事件类型</text>
        <DictTag :label="formatEventTypeName(event.eventType)" :effect="eventTypeTone(event.eventType)" />
      </view>
      <view class="security-detail__row">
        <text class="security-detail__label">客户端 IP</text>
        <text class="security-detail__value">{{ event.ipaddr }}</text>
      </view>
      <view class="security-detail__row">
        <text class="security-detail__label">请求端点</text>
        <text class="security-detail__value">{{ event.requestMethod }} {{ event.requestUri }}</text>
      </view>
      <view class="security-detail__row">
        <text class="security-detail__label">风险分值</text>
        <text class="security-detail__value">本次 +{{ event.riskDelta }}，当前 {{ event.totalRiskScore }} 分</text>
      </view>
      <view class="security-detail__row">
        <text class="security-detail__label">拦截时间</text>
        <text class="security-detail__value">{{ formatDateTime(event.createTime, true) }}</text>
      </view>

      <text class="security-detail__label security-detail__label--block">触发原因</text>
      <text class="security-detail__reason">{{ event.reason || '—' }}</text>

      <text class="security-detail__label security-detail__label--block">User-Agent</text>
      <view class="security-detail__ua">
        <text>{{ event.userAgent || '无 User-Agent 头' }}</text>
      </view>

      <view class="security-modal__actions">
        <button v-if="canBlock" class="security-action-btn security-action-btn--danger" @click="emit('block', event.ipaddr)">
          拉黑主体
        </button>
        <button v-if="canDelete" class="security-action-btn" @click="emit('delete', event.id)">删除</button>
        <button class="security-action-btn" @click="emit('update:visible', false)">关闭</button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import DictTag from '@/components/base/DictTag/index.vue'
import type { SecurityEventVO } from '@/api/system/security'
import { formatDateTime } from '@/utils/display/format'
import { formatEventTypeName, eventTypeTone } from '../constants/security'

defineProps<{
  visible: boolean
  event: SecurityEventVO | null
  canBlock: boolean
  canDelete: boolean
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'block', ip: string): void
  (e: 'delete', id: number): void
}>()
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

.security-detail__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14rpx 0;
  border-bottom: 1px solid $color-border-light;
}

.security-detail__label {
  font-size: $font-size-xs;
  color: $color-text-secondary;

  &--block {
    display: block;
    margin-top: 20rpx;
    margin-bottom: 8rpx;
  }
}

.security-detail__value {
  font-size: $font-size-xs;
  color: $color-text-primary;
  word-break: break-all;
}

.security-detail__reason {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-primary;
  background: #f8fafc;
  padding: 16rpx;
  border-radius: $radius-sm;
  word-break: break-all;
}

.security-detail__ua {
  font-size: 20rpx;
  color: $color-text-secondary;
  background: #f8fafc;
  padding: 14rpx;
  border-radius: $radius-sm;
  word-break: break-all;
  max-height: 160rpx;
  overflow-y: auto;
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

  &--danger {
    color: #ffffff;
    background: $color-danger;
  }
}
</style>

