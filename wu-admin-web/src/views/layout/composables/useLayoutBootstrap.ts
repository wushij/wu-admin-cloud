import { onMounted, onUnmounted, watch } from 'vue'
import { useRoute } from 'vue-router'
import { useUserStore } from '@/store/user'
import { useMessageStore } from '@/store/message'
import { preloadDicts } from '@/composables/useDict'
import {
  startMonitorBackground,
  stopMonitorBackground,
} from '@/composables/useMonitorBackground'
import { COMMON_DICT_TYPES } from '@/constants/dict'
import { FEATURE_BOOTSTRAP_LAYERED_LOAD } from '@/constants/features'

function runWhenIdle(fn: () => void, timeout = 2000) {
  if (typeof requestIdleCallback !== 'undefined') {
    requestIdleCallback(() => fn(), { timeout })
  } else {
    setTimeout(fn, 1)
  }
}

/** 仅缓存/服务监控页需要后台轮询 */
function isMonitorPollingRoute(path: string) {
  return path.includes('/monitor/cache') || path.includes('/monitor/server')
}

export function useLayoutBootstrap(options: {
  initTheme: () => void
  loadSiteConfig: () => Promise<void>
  loadMessages: () => Promise<void>
}) {
  const userStore = useUserStore()
  const messageStore = useMessageStore()
  const route = useRoute()

  function syncMonitorForRoute(path: string) {
    if (isMonitorPollingRoute(path)) {
      startMonitorBackground(userStore)
    } else {
      stopMonitorBackground()
    }
  }

  onMounted(async () => {
    // P0：主题 + 最小用户态
    options.initTheme()
    if (!userStore.menus || userStore.menus.length === 0) {
      try {
        await userStore.ensureUserReady()
      } catch (error) {
        console.error('获取用户信息失败', error)
      }
    }

    if (!FEATURE_BOOTSTRAP_LAYERED_LOAD) {
      options.loadMessages()
      messageStore.initWebSocket()
      options.loadSiteConfig()
      preloadDicts(COMMON_DICT_TYPES).catch(() => {})
      startMonitorBackground(userStore)
      return
    }

    // P1：首屏后立即 — 消息摘要、站点配置
    options.loadMessages()
    options.loadSiteConfig()

    // P2：空闲时 — 字典、WebSocket、监控（监控另按路由门禁）
    runWhenIdle(() => {
      preloadDicts(COMMON_DICT_TYPES).catch(() => {})
      messageStore.initWebSocket()
      syncMonitorForRoute(route.path)
    })
  })

  const stopWatch = watch(
    () => route.path,
    (path) => {
      if (!FEATURE_BOOTSTRAP_LAYERED_LOAD) return
      syncMonitorForRoute(path)
    },
  )

  onUnmounted(() => {
    stopWatch()
    messageStore.destroyWebSocket()
    stopMonitorBackground()
  })
}
