import { beforeEach, describe, expect, it } from 'vitest'
import { clearAccessToken, getAccessToken, setAccessToken } from '@/utils/auth-token'

describe('auth-token', () => {
  beforeEach(() => {
    localStorage.clear()
  })

  it('set/get/clear access token', () => {
    expect(getAccessToken()).toBeNull()
    setAccessToken('  abc  ')
    expect(getAccessToken()).toBe('abc')
    clearAccessToken()
    expect(getAccessToken()).toBeNull()
  })

  it('blank token clears storage', () => {
    setAccessToken('x')
    setAccessToken('   ')
    expect(getAccessToken()).toBeNull()
  })
})
