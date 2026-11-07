<template>
  <view v-if="visible" class="security-modal" @click="emit('update:visible', false)">
    <view class="security-modal__card" @click.stop>
      <text class="security-modal__title">手动封禁风险主体</text>

      <text class="security-form__label">目标主体</text>
      <input
        v-model="form.principal"
        class="security-input"
        placeholder="用户 ID 或 IP，例如 user:12 / 1.2.3.100"
      />

      <text class="security-form__label">封禁时长</text>
      <view class="security-input security-input--picker" @click="pickDuration">
        <text>{{ blockDurationLabel }}</text>
        <text class="security-input__arrow">›</text>
      </view>

      <text class="security-form__label">封禁原因</text>
      <input v-model="form.reason" class="security-input" placeholder="例如：恶意扫描 / 批量刷接口" />

      <view class="security-modal__actions">
        <button class="security-action-btn" @click="emit('update:visible', false)">取消</button>
        <button
          class="security-action-btn security-action-btn--danger"
          :loading="blocking"
          @click="confirmBlock"
        >
          确认封禁
        </button>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { blockRiskSubject } from '@/api/system/security'
import { showActionSheet } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import { BLOCK_DURATION_OPTIONS } from '../constants/security'

const props = defineProps<{
  visible: boolean
  initialPrincipal?: string
}>()

const emit = defineEmits<{
  (e: 'update:visible', value: boolean): void
  (e: 'success'): void
}>()

const blocking = ref(false)
const form = reactive({
  principal: '',
  duration: 3600,
  reason: '',
})

watch(
  () => props.visible,
  (val) => {
    if (val) {
      form.principal = props.initialPrincipal || ''
      form.duration = 3600
      form.reason = props.initialPrincipal ? '管理员从事件明细中手动拉黑' : '管理员手动封禁'
    }
  },
  { immediate: true },
)

const blockDurationLabel = computed(
  () => BLOCK_DURATION_OPTIONS.find((item) => item.value === form.duration)?.label || '1 小时',
)

async function pickDuration() {
  try {
    const index = await showActionSheet({
      title: '选择封禁时长',
      items: BLOCK_DURATION_OPTIONS.map((item) => ({ label: item.label })),
    })
    const option = BLOCK_DURATION_OPTIONS[index]
    if (option) form.duration = option.value
  } catch {
    /* 取消选择 */
  }
}

async function confirmBlock() {
  const principal = form.principal.trim()
  if (!principal) {
    uni.showToast({ title: '请输入待封禁的主体或 IP', icon: 'none' })
    return
  }
  blocking.value = true
  try {
    await blockRiskSubject(principal, form.duration, form.reason.trim() || '管理员手动封禁')
    showSuccessToast('已加入封禁名单')
    emit('update:visible', false)
    emit('success')
  } finally {
    blocking.value = false
  }
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

.security-form__label {
  display: block;
  margin-top: 20rpx;
  margin-bottom: 8rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-input {
  display: flex;
  align-items: center;
  width: 100%;
  height: 72rpx;
  padding: 0 20rpx;
  background: #f8fafc;
  border: 1px solid $color-border-light;
  border-radius: $radius-sm;
  font-size: $font-size-xs;
  color: $color-text-primary;
  box-sizing: border-box;

  &--picker {
    justify-content: space-between;
  }
}

.security-input__arrow {
  color: $color-text-muted;
  font-size: 28rpx;
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

