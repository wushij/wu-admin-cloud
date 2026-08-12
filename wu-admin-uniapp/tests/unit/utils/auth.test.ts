import { describe, expect, it } from 'vitest'
import { getToken, hasToken, removeToken, setToken } from '@/utils/auth'
import { STORAGE_KEYS } from '@/constants/storage-keys'

describe('auth token', () => {
  it('set/get/remove token via uni storage', () => {
    expect(hasToken()).toBe(false)
    setToken('tok-1')
    expect(getToken()).toBe('tok-1')
    expect(hasToken()).toBe(true)
    expect(uni.getStorageSync(STORAGE_KEYS.TOKEN)).toBe('tok-1')
    removeToken()
    expect(getToken()).toBe('')
    expect(hasToken()).toBe(false)
  })
})
