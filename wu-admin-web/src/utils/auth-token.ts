const TOKEN_KEY = 'wu_admin_token'

/** Sa-Token 默认 Cookie 名与 token-name 一致 */
function readTokenFromCookie(): string | null {
  if (typeof document === 'undefined') return null
  const parts = document.cookie.split(';')
  for (const part of parts) {
    const [rawKey, ...rest] = part.trim().split('=')
    if (rawKey === 'Authorization' && rest.length > 0) {
      try {
        return decodeURIComponent(rest.join('=')).trim() || null
      } catch {
        return rest.join('=').trim() || null
      }
    }
  }
  return null
}

/** 登录 Token：localStorage 持久化；网关同时支持 Authorization 头与 Cookie */
export function setAccessToken(token: string | null | undefined): void {
  if (typeof window === 'undefined') return
  if (token?.trim()) {
    localStorage.setItem(TOKEN_KEY, token.trim())
  } else {
    localStorage.removeItem(TOKEN_KEY)
  }
}

export function getAccessToken(): string | null {
  if (typeof window === 'undefined') return null
  const token = localStorage.getItem(TOKEN_KEY)
  if (token?.trim()) return token.trim()
  return readTokenFromCookie()
}

/** 登录成功后：优先响应体 token，缺失时从 Sa-Token Cookie 同步 */
export function syncAccessTokenFromLogin(bodyToken?: string | null): void {
  const token = bodyToken?.trim() || readTokenFromCookie()
  setAccessToken(token)
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') return
  localStorage.removeItem(TOKEN_KEY)
}
