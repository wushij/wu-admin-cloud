import { describe, expect, it } from 'vitest'
import {
  buildUnlockLoginConfirm,
  canUnlockLoginLock,
  formatLoginLockRemain,
  hasAccountLoginLock,
  hasIpLoginLock,
} from '@/utils/login-lock'

describe('login-lock', () => {
  it('formatLoginLockRemain', () => {
    expect(formatLoginLockRemain(null)).toBe('即将自动解除')
    expect(formatLoginLockRemain(0)).toBe('即将自动解除')
    expect(formatLoginLockRemain(30)).toBe('约 1 分钟后自动解除')
    expect(formatLoginLockRemain(120)).toBe('约 2 分钟后自动解除')
  })

  it('lock flags', () => {
    expect(hasAccountLoginLock({ loginLocked: true })).toBe(true)
    expect(hasIpLoginLock({ loginIpLocked: true })).toBe(true)
    expect(canUnlockLoginLock({ loginLocked: true })).toBe(true)
    expect(canUnlockLoginLock({})).toBe(false)
  })

  it('buildUnlockLoginConfirm covers dual lock', () => {
    const dual = buildUnlockLoginConfirm({
      username: 'alice',
      nickname: '爱丽丝',
      loginLocked: true,
      loginIpLocked: true,
      loginRecentIp: '1.1.1.1',
    })
    expect(dual.title).toBe('解除登录锁定')
    expect(dual.content).toContain('爱丽丝')
    expect(dual.content).toContain('1.1.1.1')
  })
})
