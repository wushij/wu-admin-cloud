import { describe, expect, it } from 'vitest'
import {
  buildUnlockLoginConfirm,
  buildUsernameLockTooltip,
  canUnlockLoginLock,
  formatLoginLockRemain,
  hasLoginLockRisk,
} from '@/utils/login-lock'

describe('login-lock', () => {
  it('formatLoginLockRemain', () => {
    expect(formatLoginLockRemain(undefined)).toBe('即将自动解除')
    expect(formatLoginLockRemain(90)).toBe('约 2 分钟后自动解除')
  })

  it('hasLoginLockRisk / canUnlockLoginLock', () => {
    expect(hasLoginLockRisk({ loginFailCount: 2 })).toBe(true)
    expect(canUnlockLoginLock({ loginIpLocked: true })).toBe(true)
    expect(canUnlockLoginLock({})).toBe(false)
  })

  it('buildUsernameLockTooltip and confirm', () => {
    const tip = buildUsernameLockTooltip({
      loginLocked: true,
      loginLockRemainSeconds: 60,
      loginIpLocked: true,
      loginRecentIp: '8.8.8.8',
      loginIpLockRemainSeconds: 30,
    })
    expect(tip).toContain('账号登录失败锁定')
    expect(tip).toContain('8.8.8.8')

    const confirm = buildUnlockLoginConfirm({
      username: 'bob',
      loginLocked: true,
    })
    expect(confirm.title).toBe('解除登录锁定')
    expect(confirm.content).toContain('bob')
  })
})
