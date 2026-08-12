import { describe, expect, it } from 'vitest'
import {
  clearLoginRemember,
  loadLoginRemember,
  LOGIN_REMEMBER_STORAGE_KEY,
  saveLoginRemember,
} from '@/utils/loginRemember'

describe('loginRemember', () => {
  it('saves and loads account credentials', () => {
    saveLoginRemember({
      mode: 'account',
      rememberMe: true,
      username: 'admin',
      password: 'secret',
    })
    expect(loadLoginRemember()).toEqual({
      mode: 'account',
      rememberMe: true,
      username: 'admin',
      password: 'secret',
    })
  })

  it('clears stored credentials', () => {
    saveLoginRemember({ mode: 'account', rememberMe: true, username: 'a', password: 'b' })
    clearLoginRemember()
    expect(uni.getStorageSync(LOGIN_REMEMBER_STORAGE_KEY)).toBe('')
    expect(loadLoginRemember()).toBeNull()
  })

  it('returns null for invalid payload', () => {
    uni.setStorageSync(LOGIN_REMEMBER_STORAGE_KEY, JSON.stringify({ rememberMe: false }))
    expect(loadLoginRemember()).toBeNull()
  })
})
