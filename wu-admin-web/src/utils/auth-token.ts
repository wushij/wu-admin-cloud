const TOKEN_KEY = 'wu_admin_token'

/** 登录 Token：localStorage 持久化，同域所有标签页共享；网关从 Authorization 头读取 */
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
  return token?.trim() ? token : null
}

export function clearAccessToken(): void {
  if (typeof window === 'undefined') return
  localStorage.removeItem(TOKEN_KEY)
}
