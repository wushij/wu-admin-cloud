<template>
  <PermissionBlock v-if="!canAccess" />
  <view v-else class="page-padded page-list security-page">
    <ModuleDarkHero
      title="安全风控"
      subtitle="反爬拦截 · 防重放 · 风险封禁与一键解封"
      icon="shield-o"
      theme="log"
      :count="stats.todayTotal"
      count-label="今日拦截"
    />

    <scroll-view scroll-y class="page-list__scroll" @scrolltolower="onScrollLower">
      <FadeIn :show="hasFetched">
        <StatGrid :items="statItems" :columns="3" />

        <SegmentTabs v-model="activeTab" :tabs="SECURITY_TABS" theme="teal" class="security-tabs" />

        <!-- 安全拦截事件 -->
        <template v-if="activeTab === 'events'">
          <MonitorPanel title="筛选条件">
            <SearchBar v-model="filters.ipaddr" placeholder="搜索客户端 IP" @search="applyFilters" />

            <view class="security-filter">
              <text class="security-filter__label">事件类型</text>
              <view class="security-chips security-chips--row">
                <text
                  v-for="item in PRIMARY_EVENT_TYPE_OPTIONS"
                  :key="item.value"
                  class="security-chip"
                  :class="{ 'security-chip--active': filters.eventType === item.value }"
                  @click="setEventType(item.value)"
                >
                  {{ item.label }}
                </text>
                <text
                  class="security-chip security-chip--more"
                  :class="{ 'security-chip--active': isExtraEventTypeActive }"
                  @click="pickExtraEventType"
                >
                  {{ moreEventTypeLabel }}
                </text>
              </view>
            </view>
          </MonitorPanel>

          <MonitorPanel title="拦截事件明细">
            <template #extra>
              <view class="security-panel-extra">
                <text class="security-log-meta">{{ list.length }} 条已加载</text>
                <button v-if="canClear" class="security-toolbar-btn security-toolbar-btn--danger" @click="confirmClear">
                  清空
                </button>
              </view>
            </template>

            <ListCard v-for="item in list" :key="item.id" class="security-card" @click="openDetail(item)">
              <view class="security-card__head">
                <view class="security-card__tags">
                  <DictTag :label="formatEventTypeName(item.eventType)" :effect="eventTypeTone(item.eventType)" />
                  <DictTag :label="formatAction(item.action)" :effect="actionTone(item.action)" />
                </view>
                <text class="security-card__score">+{{ item.riskDelta }}</text>
              </view>

              <view class="security-card__row">
                <text class="security-card__ip">{{ item.ipaddr }}</text>
                <text class="security-card__time">{{ formatDateTime(item.createTime, true) }}</text>
              </view>

              <text class="security-card__reason">{{ item.reason || '—' }}</text>
              <text class="security-card__endpoint">{{ item.requestMethod }} {{ item.requestUri }}</text>
              <text class="security-card__total">当前风险分 {{ item.totalRiskScore }} 分</text>
            </ListCard>

            <EmptyState v-if="empty" title="暂无安全拦截事件" icon="shield-o" />
            <ListFooter v-else :loading="loading" :finished="finished" :empty="empty" />
          </MonitorPanel>
        </template>

        <!-- 风险主体 -->
        <template v-else>
          <MonitorPanel title="筛选条件">
            <SearchBar v-model="riskFilters.keyword" placeholder="搜索用户名 / 昵称 / ID / IP" @search="applyRiskFilters" />

            <view class="security-filter">
              <text class="security-filter__label">风险等级</text>
              <view class="security-chips">
                <text
                  v-for="item in RISK_LEVEL_OPTIONS"
                  :key="String(item.value)"
                  class="security-chip"
                  :class="{ 'security-chip--active': riskFilters.level === item.value }"
                  @click="setRiskLevel(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
            </view>

            <view class="security-filter">
              <text class="security-filter__label">封禁状态</text>
              <view class="security-chips">
                <text
                  v-for="item in RISK_BLOCKED_OPTIONS"
                  :key="String(item.value)"
                  class="security-chip"
                  :class="{ 'security-chip--active': riskFilters.blocked === item.value }"
                  @click="setRiskBlocked(item.value)"
                >
                  {{ item.label }}
                </text>
              </view>
            </view>
          </MonitorPanel>

          <MonitorPanel title="风险主体列表">
            <template #extra>
              <view class="security-panel-extra">
                <button v-if="canBlock" class="security-toolbar-btn" @click="openBlock('')">
                  手动封禁
                </button>
              </view>
            </template>

            <ListCard v-for="item in riskList" :key="item.principal" class="security-card">
              <view class="security-card__head">
                <view class="security-card__tags">
                  <DictTag
                    :label="item.principalType === 'user' ? '用户主体' : 'IP主体'"
                    :effect="item.principalType === 'user' ? 'primary' : 'default'"
                  />
                  <DictTag
                    v-if="item.blocked"
                    :label="item.remainingSeconds ? `封禁中 (${formatRemainingSeconds(item.remainingSeconds)})` : '已封禁'"
                    effect="danger"
                  />
                  <DictTag v-else label="正常监控" effect="default" />
                </view>
              </view>

              <view class="security-risk__subject">
                <text class="security-risk__title">{{ formatSubjectTitle(item) }}</text>
                <text v-if="item.principalType === 'user' && item.principalId" class="security-risk__sub">
                  用户 ID: {{ item.principalId }}
                </text>
              </view>

              <view class="security-risk__bar-wrap">
                <view class="security-risk__bar-track">
                  <view
                    class="security-risk__bar-fill"
                    :class="`security-risk__bar-fill--level-${item.riskLevel ?? 0}`"
                    :style="{ width: `${Math.min(100, Math.round((item.riskScore / 120) * 100))}%` }"
                  />
                </view>
                <text class="security-risk__score">{{ item.riskScore }} 分 · {{ item.riskLevelText }}</text>
              </view>

              <view class="security-risk__meta">
                <text class="security-risk__meta-item">窗口内事件 {{ item.eventCount ?? 0 }} 次</text>
                <text class="security-risk__meta-item">最近触发 {{ item.lastEventTime || '—' }}</text>
              </view>

              <text v-if="item.ipaddr && item.principalType === 'user'" class="security-risk__meta-item">
                来源 IP：{{ item.ipaddr }}
              </text>
              <text v-if="item.reason" class="security-risk__reason">{{ item.reason }}</text>

              <view class="security-card__actions">
                <button class="outline-btn" @click="openSubjectEvents(item)">事件</button>
                <button
                  v-if="item.blocked && canUnblock"
                  class="outline-btn outline-btn--primary"
                  @click="handleUnblock(item)"
                >
                  解封
                </button>
                <button
                  v-else-if="canBlock"
                  class="outline-btn outline-btn--danger"
                  @click="openBlock(item.principal)"
                >
                  拉黑
                </button>
              </view>
            </ListCard>

            <EmptyState
              v-if="!riskLoading && !riskList.length"
              title="当前没有处于风险窗口中的主体"
              icon="shield-o"
            />
            <ListFooter v-else :loading="riskLoading" :finished="riskFinished" :empty="riskEmpty" />
          </MonitorPanel>
        </template>
      </FadeIn>
    </scroll-view>

    <!-- 弹窗子组件拆分 -->
    <SecurityEventDetailModal
      v-model:visible="detailVisible"
      :event="activeEvent"
      :can-block="canBlock"
      :can-delete="canDelete"
      @block="openBlock"
      @delete="confirmDelete"
    />

    <SecuritySubjectEventsModal
      v-model:visible="subjectEventsVisible"
      :subject="activeSubject"
      :events="subjectEvents"
      :loading="subjectEventsLoading"
    />

    <SecurityBlockModal
      v-model:visible="blockVisible"
      :initial-principal="targetPrincipal"
      @success="handleBlockSuccess"
    />

    <AppDialogHost />
  </view>
</template>

<script setup lang="ts">
import { computed, reactive, ref, onMounted, watch } from 'vue'
import { onPullDownRefresh } from '@dcloudio/uni-app'
import ModuleDarkHero from '@/components/layout/ModuleDarkHero/index.vue'
import MonitorPanel from '@/components/layout/MonitorPanel/index.vue'
import SearchBar from '@/components/base/SearchBar/index.vue'
import ListFooter from '@/components/base/ListFooter/index.vue'
import ListCard from '@/components/base/ListCard/index.vue'
import DictTag from '@/components/base/DictTag/index.vue'
import FadeIn from '@/components/base/FadeIn/index.vue'
import EmptyState from '@/components/base/EmptyState/index.vue'
import SegmentTabs from '@/components/base/SegmentTabs/index.vue'
import StatGrid from '@/components/base/StatGrid/index.vue'
import type { StatItem } from '@/components/base/StatGrid/index.vue'
import PermissionBlock from '@/components/base/PermissionBlock/index.vue'
import AppDialogHost from '@/components/base/AppDialogHost/index.vue'
import { usePageList } from '@/composables/base/usePageList'
import { useModulePermission } from '@/composables/base/useModulePermission'
import {
  getSecurityEvents,
  getSecurityStats,
  getRiskSubjects,
  getRiskSubjectEvents,
  unblockRiskSubject,
  deleteSecurityEvent,
  clearSecurityEvents,
  type SecurityEventVO,
  type SecurityStatsVO,
  type RiskSubjectVO,
  type RiskSubjectPageQuery,
} from '@/api/system/security'
import { formatDateTime } from '@/utils/display/format'
import { showActionSheet, showConfirm } from '@/utils/app-dialog'
import { showSuccessToast } from '@/utils/app-toast'
import {
  SECURITY_TABS,
  PRIMARY_EVENT_TYPE_OPTIONS,
  EXTRA_EVENT_TYPE_OPTIONS,
  RISK_LEVEL_OPTIONS,
  RISK_BLOCKED_OPTIONS,
  formatEventTypeName,
  eventTypeTone,
  formatAction,
  actionTone,
  formatRemainingSeconds,
} from './constants/security'
import SecurityEventDetailModal from './components/SecurityEventDetailModal.vue'
import SecuritySubjectEventsModal from './components/SecuritySubjectEventsModal.vue'
import SecurityBlockModal from './components/SecurityBlockModal.vue'

const { allowed, hasPerm } = useModulePermission('system:security:query')
const canAccess = computed(() => allowed.value || hasPerm('system:security:list'))
const canUnblock = computed(() => hasPerm('system:security:unblock'))
const canBlock = computed(() => hasPerm('system:security:block'))
const canDelete = computed(() => hasPerm('system:security:delete'))
const canClear = computed(() => hasPerm('system:security:clear'))

const activeTab = ref<'events' | 'risk'>('events')
const hasFetched = ref(false)

const stats = reactive<SecurityStatsVO>({
  todayTotal: 0,
  todayBlocked: 0,
  todayForbidden: 0,
  todayWarning: 0,
  todayChallenge: 0,
  todayUaBlock: 0,
  todayReplayBlock: 0,
  todayHoneypot: 0,
  todayPermissionDenied: 0,
  activeBlockedCount: 0,
  activeRiskCount: 0,
  riskLevelDistribution: {},
  topRiskSubjects: [],
})

const statItems = computed<StatItem[]>(() => [
  { label: '今日拦截', value: stats.todayTotal, tone: 'primary' },
  { label: '爬虫嗅探', value: stats.todayUaBlock, tone: 'warning' },
  { label: '重放攻击', value: stats.todayReplayBlock, tone: 'info' },
  { label: '蜜罐击中', value: stats.todayHoneypot, tone: 'success' },
  { label: '自动封禁', value: stats.todayForbidden, tone: 'danger' },
  { label: '当前封禁', value: stats.activeBlockedCount, tone: 'danger' },
])

const filters = reactive({
  ipaddr: '',
  eventType: '',
})

const isExtraEventTypeActive = computed(() => {
  return EXTRA_EVENT_TYPE_OPTIONS.some((item) => item.value === filters.eventType)
})

const moreEventTypeLabel = computed(() => {
  const match = EXTRA_EVENT_TYPE_OPTIONS.find((item) => item.value === filters.eventType)
  return match ? `${match.label} ▾` : '更多 ▾'
})

async function pickExtraEventType() {
  try {
    const index = await showActionSheet({
      title: '选择细分事件类型',
      items: EXTRA_EVENT_TYPE_OPTIONS.map((item) => ({ label: item.label })),
    })
    const selected = EXTRA_EVENT_TYPE_OPTIONS[index]
    if (selected) {
      setEventType(selected.value)
    }
  } catch {
    /* 用户取消选择 */
  }
}

const {
  list,
  loading,
  finished,
  empty,
  refresh,
  loadMore,
} = usePageList<SecurityEventVO>(async (pageNo, pageSize) => {
  const res = await getSecurityEvents({
    pageNo,
    pageSize,
    ipaddr: filters.ipaddr.trim() || undefined,
    eventType: filters.eventType || undefined,
  })
  return { list: res.data?.list || [], total: res.data?.total || 0 }
})

const riskFilters = reactive<RiskSubjectPageQuery>({
  keyword: '',
  level: undefined,
  blocked: undefined,
})

const {
  list: riskList,
  loading: riskLoading,
  finished: riskFinished,
  empty: riskEmpty,
  refresh: refreshRisk,
  loadMore: loadMoreRisk,
} = usePageList<RiskSubjectVO>(async (pageNo, pageSize) => {
  const res = await getRiskSubjects({
    pageNo,
    pageSize,
    keyword: (riskFilters.keyword || '').trim() || undefined,
    level: riskFilters.level,
    blocked: riskFilters.blocked,
  })
  return { list: res.data?.list || [], total: res.data?.total || 0 }
})

const subjectEventsVisible = ref(false)
const subjectEventsLoading = ref(false)
const subjectEvents = ref<SecurityEventVO[]>([])
const activeSubject = ref<RiskSubjectVO | null>(null)

const detailVisible = ref(false)
const activeEvent = ref<SecurityEventVO | null>(null)

const blockVisible = ref(false)
const targetPrincipal = ref('')

async function loadEvents() {
  await refresh()
  hasFetched.value = true
}

function applyFilters() {
  activeTab.value = 'events'
  void loadEvents()
}

function setEventType(value: string) {
  filters.eventType = value
  void loadEvents()
}

function onScrollLower() {
  if (activeTab.value === 'events') {
    loadMore()
  } else {
    loadMoreRisk()
  }
}

watch(activeTab, (tab) => {
  if (tab === 'events') {
    void loadEvents()
  } else {
    void loadRiskList()
  }
})

function openDetail(item: SecurityEventVO) {
  activeEvent.value = item
  detailVisible.value = true
}

async function confirmDelete(id: number) {
  const { confirmed } = await showConfirm({
    title: '删除事件',
    content: '确定删除这条安全拦截事件记录吗？',
    confirmText: '删除',
    tone: 'danger',
  })
  if (!confirmed) return
  await deleteSecurityEvent(id)
  showSuccessToast('删除成功')
  detailVisible.value = false
  await loadEvents()
  await loadStats()
}

async function confirmClear() {
  const { confirmed } = await showConfirm({
    title: '清空安全事件',
    content: '确定清空全部安全拦截事件审计日志吗？清空后不可恢复。',
    confirmText: '确认清空',
    tone: 'danger',
  })
  if (!confirmed) return
  await clearSecurityEvents()
  showSuccessToast('安全事件已清空')
  await loadEvents()
  await loadStats()
}

function formatSubjectTitle(item: RiskSubjectVO) {
  if (item.principalType === 'user' && (item.username || item.nickname)) {
    const name = item.nickname ? `${item.nickname} (${item.username || ''})` : item.username
    return `${name} · ID: ${item.principalId}`
  }
  return item.principal
}

async function handleUnblock(item: RiskSubjectVO) {
  const title = formatSubjectTitle(item)
  const { confirmed } = await showConfirm({
    title: '解除封禁',
    content: `确认解除对风险主体「${title}」的封禁并清零风险分吗？`,
    confirmText: '立即解封',
  })
  if (!confirmed) return
  await unblockRiskSubject(item.principal, true)
  showSuccessToast(`【${title}】已解封并清零风险分`)
  await loadRiskList()
  await loadStats()
}

function openBlock(principal = '') {
  targetPrincipal.value = principal
  blockVisible.value = true
}

async function handleBlockSuccess() {
  detailVisible.value = false
  await loadRiskList()
  await loadStats()
}

async function loadStats() {
  try {
    const res = await getSecurityStats()
    if (res.data) Object.assign(stats, res.data)
  } catch {
    /* 忽略错误 */
  }
}

async function loadRiskList() {
  await refreshRisk({ silent: true })
}

function applyRiskFilters() {
  void loadRiskList()
}

function setRiskLevel(value: number | undefined) {
  riskFilters.level = value
  void loadRiskList()
}

function setRiskBlocked(value: boolean | undefined) {
  riskFilters.blocked = value
  void loadRiskList()
}

async function openSubjectEvents(item: RiskSubjectVO) {
  activeSubject.value = item
  subjectEvents.value = []
  subjectEventsVisible.value = true
  subjectEventsLoading.value = true
  try {
    const res = await getRiskSubjectEvents(item.principal, 50)
    subjectEvents.value = res.data || []
  } finally {
    subjectEventsLoading.value = false
  }
}

onMounted(async () => {
  await loadEvents()
  await Promise.all([loadStats(), loadRiskList()])
})

onPullDownRefresh(async () => {
  if (activeTab.value === 'events') {
    await loadEvents()
  } else {
    await loadRiskList()
  }
  await loadStats()
  uni.stopPullDownRefresh()
})
</script>

<style lang="scss" scoped>
@use '@/styles/common.scss' as *;

.security-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  /* #ifdef H5 */
  height: calc(100vh - var(--window-top, 0px));
  /* #endif */
  box-sizing: border-box;
  overflow: hidden;
}

.page-list__scroll {
  flex: 1;
  min-height: 0;

  :deep(.uni-scroll-view-content) {
    padding-bottom: calc(160rpx + env(safe-area-inset-bottom));
  }
}

.security-tabs {
  margin-bottom: 16rpx;
}

.security-toolbar-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 56rpx;
  padding: 0 24rpx;
  margin: 0;
  font-size: 22rpx;
  color: $color-primary;
  background: rgba(99, 102, 241, 0.08);
  border: 1px solid rgba(99, 102, 241, 0.28);
  border-radius: 999rpx;

  &::after {
    border: none;
  }
}

.security-toolbar-btn--danger {
  color: $color-danger;
  background: rgba(245, 108, 108, 0.08);
  border-color: rgba(245, 108, 108, 0.28);
}

.security-filter {
  margin-top: 20rpx;
}

.security-filter__label {
  display: block;
  margin-bottom: 12rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
}

.security-chips {
  display: flex;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 4rpx;
}

.security-chips--row {
  flex-wrap: nowrap;
}

.security-chips--row .security-chip {
  flex: 1;
  padding: 10rpx 4rpx;
}

.security-chip {
  padding: 10rpx 20rpx;
  font-size: $font-size-xs;
  color: $color-text-secondary;
  background: #f8fafc;
  border: 1px solid $color-border-light;
  border-radius: $radius-full;
  text-align: center;
  box-sizing: border-box;

  &--active {
    color: #ffffff;
    background: #0d9488;
    border-color: #0d9488;
    font-weight: 500;
  }
}

.security-panel-extra {
  display: flex;
  align-items: center;
  gap: 16rpx;
}

.security-log-meta {
  font-size: 20rpx;
  color: $color-text-muted;
}

.security-card {
  margin-bottom: 16rpx;
}

.security-card__head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12rpx;
}

.security-card__tags {
  display: flex;
  align-items: center;
  gap: 8rpx;
}

.security-card__score {
  font-size: $font-size-xs;
  font-weight: 700;
  color: $color-danger;
}

.security-card__row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 10rpx;
}

.security-card__ip {
  font-size: $font-size-xs;
  font-weight: 600;
  color: $color-text-primary;
}

.security-card__time {
  font-size: 20rpx;
  color: $color-text-muted;
}

.security-card__reason {
  display: block;
  font-size: $font-size-xs;
  color: $color-text-primary;
  margin-bottom: 8rpx;
  line-height: 1.4;
}

.security-card__endpoint {
  display: block;
  font-size: 20rpx;
  color: $color-text-muted;
  margin-bottom: 8rpx;
  word-break: break-all;
}

.security-card__total {
  display: block;
  font-size: 20rpx;
  color: #0d9488;
  font-weight: 500;
}

.security-card__actions {
  display: flex;
  justify-content: flex-end;
  gap: 12rpx;
  margin-top: 16rpx;
  padding-top: 16rpx;
  border-top: 1px dashed $color-border-light;
}

.outline-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  height: 52rpx;
  padding: 0 20rpx;
  margin: 0;
  font-size: 22rpx;
  color: $color-text-primary;
  background: #ffffff;
  border: 1px solid $color-border;
  border-radius: $radius-full;

  &::after {
    border: none;
  }

  &--primary {
    color: #0d9488;
    border-color: #0d9488;
  }

  &--danger {
    color: $color-danger;
    border-color: $color-danger;
  }
}

.security-risk__subject {
  margin-bottom: 14rpx;
}

.security-risk__title {
  display: block;
  font-size: $font-size-xs;
  font-weight: 600;
  color: $color-text-primary;
}

.security-risk__sub {
  display: block;
  margin-top: 4rpx;
  font-size: 20rpx;
  color: $color-text-muted;
}

.security-risk__bar-wrap {
  display: flex;
  align-items: center;
  gap: 16rpx;
  margin-bottom: 12rpx;
}

.security-risk__bar-track {
  flex: 1;
  height: 12rpx;
  background: #f1f5f9;
  border-radius: 6rpx;
  overflow: hidden;
}

.security-risk__bar-fill {
  height: 100%;
  border-radius: 6rpx;
  transition: width 0.3s ease;

  &--level-0,
  &--level-1 {
    background: #0d9488;
  }

  &--level-2 {
    background: #f59e0b;
  }

  &--level-3 {
    background: #f97316;
  }

  &--level-4,
  &--level-5 {
    background: #ef4444;
  }
}

.security-risk__score {
  font-size: 20rpx;
  font-weight: 600;
  color: $color-text-secondary;
}

.security-risk__meta {
  display: flex;
  align-items: center;
  gap: 20rpx;
  margin-bottom: 8rpx;
}

.security-risk__meta-item {
  display: block;
  font-size: 20rpx;
  color: $color-text-muted;
}

.security-risk__reason {
  display: block;
  margin-top: 6rpx;
  font-size: $font-size-xs;
  color: $color-danger;
}
</style>
