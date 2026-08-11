/** 是否为仅开发环境可用的 API 地址（本机 / 局域网 IP） */
function isDevOnlyApiUrl(url: string): boolean {
  if (!url.startsWith('http://') && !url.startsWith('https://')) return false
  try {
    const { hostname } = new URL(url)
    if (hostname === 'localhost' || hostname === '127.0.0.1') return true
    if (hostname.startsWith('10.')) return true
    if (hostname.startsWith('192.168.')) return true
    const m = /^172\.(\d+)\./.exec(hostname)
    if (m) {
      const second = Number(m[1])
      if (second >= 16 && second <= 31) return true
    }
    return false
  } catch {
    return false
  }
}

/**
 * 微服务化：所有请求统一走网关 wu-cloud-gateway(:8080)，由网关按 /api/<域>/** 前缀分发。
 * - H5 开发态：保留相对 /api，配合 vite proxy 转发到网关（同源、避免跨域凭证问题）。
 * - 其他端：默认指向网关全地址 http://localhost:8080/api，可用 VITE_API_BASE_URL 覆盖。
 */
export function resolveApiBaseUrl(): string {
  // #ifdef H5
  return '/api'
  // #endif

  const configured = import.meta.env.VITE_API_BASE_URL || 'http://localhost:8080/api'

  // #ifdef MP-WEIXIN
  if (import.meta.env.PROD && configured.startsWith('/')) {
    const origin = (import.meta.env.VITE_API_PRODUCTION_ORIGIN || '').replace(/\/$/, '')
    if (origin) return `${origin}${configured}`
  }
  // #endif

  return configured
}
