import axios, { AxiosInstance, AxiosResponse, InternalAxiosRequestConfig, AxiosRequestConfig } from 'axios'
import { ElMessage } from 'element-plus'
import router from '@/router'
import type { ApiResult } from '@/types/api'
import { isApiSuccessCode } from '@/utils/api-response'
import { getErrorMessage, markErrorToastShown } from '@/utils/axiosError'
import { useUserStore } from '@/store/user'
import { generateNonce, getTimestamp, encryptSm4, decryptSm4, signHmacSm3 } from '@/utils/crypto'
import { getSecurityConfig, getClientId, requestSessionSignKey, clearSignKeys } from '@/utils/security-config'

export type { ApiResult } from '@/types/api'
export { getErrorMessage, isErrorToastShown } from '@/utils/axiosError'

let lastForbiddenToastAt = 0
function showForbiddenOnce(message: string) {
  const now = Date.now()
  if (now - lastForbiddenToastAt < 2000) return
  lastForbiddenToastAt = now
  ElMessage.error(message)
}

function rejectWithToast(message: string, source?: unknown): Promise<never> {
  const err = source instanceof Error ? source : new Error(message)
  if (!err.message) {
    err.message = message
  }
  markErrorToastShown(err)
  return Promise.reject(err)
}

function resolveBodyMessage(message?: string, fallback = '请求失败') {
  return message || fallback
}

let isRedirectingLogin = false
function clearSessionAndRedirectLogin() {
  if (isRedirectingLogin) return
  isRedirectingLogin = true
  try {
    useUserStore().logout()
  } catch {
    /* store 可能尚未初始化 */
  }
  Promise.resolve(router.push('/login')).finally(() => {
    setTimeout(() => { isRedirectingLogin = false }, 2000)
  })
}

/** 公开认证接口（登录 Cookie 写入前不依赖会话） */
const AUTH_PUBLIC_SUFFIXES = [
  '/auth/login',
  '/auth/register',
  '/auth/captcha',
  '/auth/config',
  '/auth/sms-code',
]

function isAuthPublicUrl(url?: string): boolean {
  if (!url) return false
  const path = url.split('?')[0]
  return AUTH_PUBLIC_SUFFIXES.some((suffix) => path === suffix || path.endsWith(suffix))
}

/** 扩展 axios 配置：静默错误 / 503 重试（工作台等非关键读接口使用） */
export type WuRequestConfig = AxiosRequestConfig & {
  silent403?: boolean
  /** 401 不触发全局登出跳转（后台轮询等旁路请求） */
  silent401?: boolean
  /** 仅 503 静默 */
  silent503?: boolean
  /** 任意错误均不弹窗（埋点等旁路请求） */
  silent?: boolean
  retry503?: number
  _retry503Count?: number
  _isRetrySign?: boolean
  _rawBody?: unknown
}

function isSilentRequest(cfg?: WuRequestConfig | null): boolean {
  if (!cfg) return false
  if (cfg.silent) return true
  const url = cfg.url?.toString() || ''
  // 访问埋点旁路：偶发 BFF/网关 503 不应打扰用户
  return url.includes('/dashboard/visit')
}

function retry503IfNeeded(_error: AxiosErrorLike, cfg: WuRequestConfig | undefined) {
  if (!cfg || cfg.method?.toLowerCase() !== 'get') {
    return null
  }
  const maxRetry = cfg.retry503 ?? 0
  const count = cfg._retry503Count ?? 0
  if (count >= maxRetry) {
    return null
  }
  cfg._retry503Count = count + 1
  const delayMs = 400 * cfg._retry503Count
  return new Promise((resolve) => setTimeout(resolve, delayMs)).then(() => service(cfg))
}

interface AxiosErrorLike {
  response?: { status?: number; data?: { msg?: string; message?: string } }
  config?: WuRequestConfig
  message?: string
}

const service: AxiosInstance = axios.create({
  // 微服务化：所有请求统一走网关 wu-cloud-gateway(:8080)，由网关按 /api/<域>/** 前缀分发
  // 开发态可通过 .env.development 的 VITE_API_BASE 覆盖；缺省走网关
  baseURL: import.meta.env.VITE_API_BASE || '/api',
  timeout: 15000,
  withCredentials: true,
  headers: {
    'Content-Type': 'application/json;charset=UTF-8',
  },
})

function isSignKeyExpiredMessage(text?: string): boolean {
  if (!text) return false
  return text.includes('签名密钥') || text.includes('签名验证失败') || text.includes('X-Signature')
}

type RetryableRequestConfig = InternalAxiosRequestConfig & {
  _rawBody?: unknown
  _isRetrySign?: boolean
}

async function retryRequestWithNewSignKey(cfg: RetryableRequestConfig): Promise<AxiosResponse> {
  cfg._isRetrySign = true
  clearSignKeys()
  try {
    await requestSessionSignKey(service)
    if (cfg._rawBody !== undefined) {
      cfg.data = cfg._rawBody
    }
    return service(cfg)
  } catch (err) {
    return Promise.reject(err)
  }
}

service.interceptors.request.use(
  async (config) => {
    const customCfg = config as RetryableRequestConfig
    if (customCfg._rawBody === undefined) {
      customCfg._rawBody = config.data
    } else {
      config.data = customCfg._rawBody
    }

    // 自动植入时间戳、Nonce、ClientId 防重放 Request Headers
    const timestamp = getTimestamp()
    const nonce = generateNonce()

    // 竞态保护：针对一般业务请求，若密钥还未获取，主动触发并等待单例密钥获取
    if (
      !isAuthPublicUrl(config.url) &&
      !config.url?.includes('/auth/session-sign-init') &&
      !config.url?.includes('/auth/logout')
    ) {
      const currentSec = getSecurityConfig()
      if (!currentSec.sm3SignKey && !currentSec.sm4Key) {
        try {
          await requestSessionSignKey(service)
        } catch {
          /* 忽略异常 */
        }
      }
    }

    config.headers['X-Timestamp'] = timestamp
    config.headers['X-Nonce'] = nonce
    // 【安全加固 P0】携带会话 clientId，服务端据此从 Redis 查找临时签名密钥
    config.headers['X-Client-Id'] = getClientId()

    // 读取运行时下发的安全策略（闭包保存，不挂 window，密钥未下发时不启用加密/签名）
    const secConfig = getSecurityConfig()
    const isFormData = config.data instanceof FormData

    if (secConfig.sm4EncryptEnabled && secConfig.sm4Key) {
      config.headers['X-Accept-Encrypted'] = '1'
      if (config.data && !isFormData) {
        const plainStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        config.data = encryptSm4(plainStr, secConfig.sm4Key)
        config.headers['X-Encrypted'] = '1'
      }
    }

    const isSignEnabled = (secConfig.sm3SignEnabled || secConfig.sm2SignEnabled) && secConfig.sm3SignKey
    if (isSignEnabled) {
      let bodyStr = ''
      if (config.data && !isFormData) {
        bodyStr = typeof config.data === 'string' ? config.data : JSON.stringify(config.data)
        bodyStr = bodyStr.trim()
        if (bodyStr.startsWith('"') && bodyStr.endsWith('"') && bodyStr.length > 2) {
          bodyStr = bodyStr.substring(1, bodyStr.length - 1)
        }
      }
      // 提取完整的 URI (包含经过 Axios 序列化后的 params QueryString)
      let fullPath = config.url || ''
      if (fullPath.startsWith('/api/')) {
        fullPath = fullPath.substring(4)
      } else if (!fullPath.startsWith('/')) {
        fullPath = '/' + fullPath
      }
      if (config.params && typeof config.params === 'object') {
        const queryParts: string[] = []
        Object.entries(config.params).forEach(([key, val]) => {
          if (val !== undefined && val !== null) {
            queryParts.push(`${encodeURIComponent(key)}=${encodeURIComponent(String(val))}`)
          }
        })
        const qs = queryParts.join('&')
        if (qs) {
          fullPath += (fullPath.includes('?') ? '&' : '?') + qs
        }
      }
      try {
        fullPath = decodeURIComponent(fullPath)
      } catch {}

      const signContent = `${config.method?.toUpperCase()}\n${fullPath}\n${timestamp}\n${nonce}\n${bodyStr}`
      const signKey = secConfig.sm3SignKey || ''
      const signature = signHmacSm3(signContent, signKey)
      if (signature) {
        config.headers['X-Signature'] = signature
      }
    }

    return config
  },
  (error) => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  },
)


service.interceptors.response.use(
  (response: AxiosResponse) => {
    const secConfig = getSecurityConfig()
    // 若响应标明 SM4 加密，且密钥已下发，自动解密 (兼容 CORS 限制：如果启用加密且返回的是非 JSON 字符串，也尝试解密)
    const isEncrypted = response?.headers?.['x-encrypted'] === '1' || 
      (secConfig.sm4EncryptEnabled && typeof response.data === 'string' && !response.data.trim().startsWith('{') && !response.data.trim().startsWith('['));
    if (isEncrypted && typeof response.data === 'string') {
      const { sm4Key } = secConfig
      if (sm4Key) {
        const plainJson = decryptSm4(response.data, sm4Key)
        try {
          response.data = JSON.parse(plainJson)
        } catch {
          response.data = plainJson
        }
      }
    }

    const res = response.data
    const { code, message } = res

    if (isApiSuccessCode(code)) {
      return res
    }

    const cfg = response.config as InternalAxiosRequestConfig & { silent403?: boolean; _isRetrySign?: boolean }
    const text = resolveBodyMessage(message)

    if (code === 401) {
      // 主动退出登录：如果 token 已过期/已被清理，后端可能返回 401
      // 但这不应该再弹“登录已过期”，否则点退出就会看到提示框再跳转。
      if (cfg.url?.toString().includes('/auth/logout')) {
        return Promise.reject(new Error(text || '已退出'))
      }
      if (isAuthPublicUrl(cfg.url)) {
        ElMessage.error(text || '认证失败')
        return rejectWithToast(text || '认证失败')
      }
      ElMessage.error('登录已过期，请重新登录')
      clearSessionAndRedirectLogin()
      return rejectWithToast(text || '未授权')
    }

    if (code === 403) {
      // 🔐 自动静默恢复：签名密钥过期时自动重新协商并发起一次重试
      if (isSignKeyExpiredMessage(text) && !cfg._isRetrySign && !cfg.url?.includes('/auth/session-sign-init')) {
        return retryRequestWithNewSignKey(cfg)
      }
      if (!cfg?.silent403) {
        showForbiddenOnce(text || '权限不足，无法操作')
      }
      return rejectWithToast('权限不足')
    }

    if (code === 429) {
      ElMessage.warning(text || '操作过于频繁，请稍后再试')
      return rejectWithToast(text || '操作过于频繁，请稍后再试')
    }

    if (cfg?.url?.includes('/auth/session-sign-init')) {
      return Promise.reject(new Error(text || '获取签名密钥失败'))
    }

    ElMessage.error(text)
    return rejectWithToast(text)
  },
  (error) => {
    console.error('响应错误:', error)

    if (error.response) {
      const secConfig = getSecurityConfig()
      // 若错误响应标明 SM4 加密，且密钥已下发，自动解密 (CORS 容错)
      const isEncrypted = error.response.headers?.['x-encrypted'] === '1' || 
        (secConfig.sm4EncryptEnabled && typeof error.response.data === 'string' && !error.response.data.trim().startsWith('{') && !error.response.data.trim().startsWith('['));
      if (isEncrypted && typeof error.response.data === 'string') {
        const { sm4Key } = secConfig
        if (sm4Key) {
          const plainJson = decryptSm4(error.response.data, sm4Key)
          try {
            error.response.data = JSON.parse(plainJson)
          } catch {
            error.response.data = plainJson
          }
        }
      }

      const { status } = error.response
      const cfg = error.config as WuRequestConfig
      const text = getErrorMessage(error) || error.message || '请求失败'

      // 🔐 静默排除：如果是初始化会话签名密钥失败，一律静默 Reject 不弹窗
      if (cfg?.url?.includes('/auth/session-sign-init')) {
        return Promise.reject(error)
      }

      // 旁路请求（访问埋点等）：失败只记日志，不弹「服务暂时不可用」
      if (isSilentRequest(cfg)) {
        markErrorToastShown(error)
        return Promise.reject(error)
      }

      if (status === 401) {
        if (cfg?.silent401) {
          markErrorToastShown(error)
          return Promise.reject(error)
        }
        if (isAuthPublicUrl(cfg?.url)) {
          ElMessage.error(text || '认证失败')
        } else {
          // 未认证或 Token 过期时，静默清理 Token 并平滑跳转至登录页，不在页面上弹出多余的红色吐司
          clearSessionAndRedirectLogin()
        }
      } else if (status === 403) {
        // 🔐 自动静默恢复：签名密钥过期时自动重新协商并发起一次重试
        if (isSignKeyExpiredMessage(text) && !cfg?._isRetrySign && !cfg?.url?.includes('/auth/session-sign-init')) {
          return retryRequestWithNewSignKey(cfg as RetryableRequestConfig)
        }
        if (!cfg?.silent403) {
          showForbiddenOnce(text || '权限不足，无法访问')
        }
      } else if (status === 429) {
        ElMessage.warning(text || '操作过于频繁，请稍后再试')
      } else if (status === 503) {
        const retry = retry503IfNeeded(error, cfg)
        if (retry) {
          return retry
        }
        if (cfg?.silent503) {
          markErrorToastShown(error)
          return Promise.reject(error)
        }
        ElMessage.error(text || '服务暂时不可用，请稍后重试')
      } else {
        ElMessage.error(text)
      }
      markErrorToastShown(error)
    } else {
      if (isSilentRequest(error.config as WuRequestConfig)) {
        markErrorToastShown(error)
        return Promise.reject(error)
      }
      ElMessage.error('网络异常，请检查网络连接')
      markErrorToastShown(error)
    }

    return Promise.reject(error)
  },
)

export default service

export type HttpQueryParamValue = string | number | boolean | null | undefined
export type HttpQueryParams = Record<string, HttpQueryParamValue>

export const get = <T = unknown>(url: string, params?: object, config?: AxiosRequestConfig) => {
  return service.get(url, { ...config, params }) as Promise<ApiResult<T>>
}

export const post = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => {
  return service.post(url, data, config) as Promise<ApiResult<T>>
}

export const put = <T = unknown>(url: string, data?: unknown, config?: AxiosRequestConfig) => {
  return service.put(url, data, config) as Promise<ApiResult<T>>
}

export const del = <T = unknown>(url: string, config?: AxiosRequestConfig) => {
  return service.delete(url, config) as Promise<ApiResult<T>>
}
