import { defineStore } from 'pinia'
import { computed, ref } from 'vue'
import type { MenuTreeNode } from '@/types/api'
import { login, getInfo, logout as logoutApi } from '@/api/system/auth'
import type { LoginForm, AuthInfo } from '@/types/api'
import { useMessageStore } from '@/store/message'
import { useTagsViewStore } from '@/store/tagsView'
import { resetMonitorBackground } from '@/composables/useMonitorBackground'
import { resetSecurityConfig, requestSessionSignKey } from '@/utils/security-config'
import http from '@/utils/request'
import { clearAccessToken, syncAccessTokenFromLogin } from '@/utils/auth-token'

/** 登录后内存中的用户信息（与 /auth/info 字段子集一致） */
export type UserInfo = Partial<
  Pick<AuthInfo, 'userId' | 'username' | 'nickname' | 'avatar' | 'roles' | 'permissions'>
>

export type { MenuTreeNode as MenuItem } from '@/types/api'

export const useUserStore = defineStore('user', () => {
  const userInfo = ref<UserInfo>({})
  const menus = ref<MenuTreeNode[]>([])

  const isLoggedIn = computed(() => userInfo.value.userId != null)

  const initSessionKey = async () => {
    try {
      await requestSessionSignKey(http)
    } catch {
      // 忽略
    }
  }

  const loginAction = async (loginForm: LoginForm) => {
    const res = await login(loginForm)
    // 登录成功后，立刻尝试初始化签名密钥。即使获取失败也绝不打断登录主流程（由后续拦截器自愈机制兜底）
    try {
      await initSessionKey()
    } catch (e) {
      console.warn('初始化会话密钥失败，由后续拦截器自愈机制兜底:', e)
    }
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
    }
    // 显式将 token 写入本地，避免 Cookie 因跨域/CORS 未落地时导致 /auth/info 无鉴权信息
    syncAccessTokenFromLogin(res.data.token)
    return res
  }

  const getUserInfo = async () => {
    // 签名密钥初始化与用户信息并行发起：axios 拦截器已有竞态保护，
    // getInfo 的实际 HTTP 请求会在密钥就绪后才真正发出
    const [, res] = await Promise.all([initSessionKey(), getInfo()])
    userInfo.value = {
      userId: res.data.userId,
      username: res.data.username,
      nickname: res.data.nickname,
      avatar: res.data.avatar,
      roles: res.data.roles || [],
      permissions: res.data.permissions || [],
    }
    menus.value = res.data.menus || []
    return res
  }

  const refreshUserStore = async () => {
    try {
      await getUserInfo()
    } catch (error) {
      console.error('刷新用户信息失败:', error)
      logout()
      throw error
    }
  }

  /** 短时效 in-flight 去重：守卫 / Layout / Dashboard 共用同一轮刷新 */
  let loadingPromise: Promise<void> | null = null
  const ensureUserReady = async () => {
    if (userInfo.value.userId != null && menus.value.length > 0) {
      return
    }
    if (!loadingPromise) {
      loadingPromise = refreshUserStore().finally(() => {
        loadingPromise = null
      })
    }
    return loadingPromise
  }

  const logout = () => {
    // 登出时，清空签名密钥闭包配置！
    resetSecurityConfig()
    try {
      useMessageStore().destroyWebSocket()
    } catch {
      /* store 可能尚未初始化 */
    }
    resetMonitorBackground()
    try {
      useTagsViewStore().resetTags()
    } catch {
      /* store 可能尚未初始化 */
    }
    userInfo.value = {}
    menus.value = []
    // 清理本地 token，避免其它页面仍带旧 Authorization 访问导致“看似登录又出现”的竞态
    clearAccessToken()
  }

  const logoutAction = async () => {
    try {
      if (isLoggedIn.value) {
        await logoutApi()
      }
    } catch (error) {
      console.warn('调用退出接口失败，执行本地清理', error)
    } finally {
      logout()
    }
  }

  const patchUserInfo = (partial: Partial<UserInfo>) => {
    userInfo.value = { ...userInfo.value, ...partial }
  }

  return {
    userInfo,
    menus,
    isLoggedIn,
    loginAction,
    getUserInfo,
    refreshUserStore,
    ensureUserReady,
    logout,
    logoutAction,
    patchUserInfo,
  }
})
