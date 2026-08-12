import { describe, expect, it } from 'vitest'
import { decryptSm4, encryptSm4, generateNonce, getTimestamp, signHmacSm3 } from '@/utils/crypto'

const SM4_KEY = '0123456789abcdef0123456789abcdef'
const SIGN_KEY = '0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef'

describe('crypto', () => {
  it('generateNonce returns 32 hex-like chars', () => {
    const nonce = generateNonce()
    expect(nonce).toHaveLength(32)
    expect(nonce).toMatch(/^[a-f0-9]+$/)
  })

  it('getTimestamp returns millisecond string', () => {
    expect(getTimestamp()).toMatch(/^\d+$/)
  })

  it('encryptSm4 / decryptSm4 round-trip', () => {
    const plain = '{"ok":true}'
    const cipher = encryptSm4(plain, SM4_KEY)
    expect(cipher.length).toBeGreaterThan(32)
    expect(decryptSm4(cipher, SM4_KEY)).toBe(plain)
  })

  it('encryptSm4 rejects empty key', () => {
    expect(() => encryptSm4('x', '')).toThrow(/密钥/)
  })

  it('signHmacSm3 is deterministic', () => {
    const content = 'GET\n/auth/info\n1\nn1\n'
    const sig = signHmacSm3(content, SIGN_KEY)
    expect(sig).toBeTruthy()
    expect(signHmacSm3(content, SIGN_KEY)).toBe(sig)
    expect(signHmacSm3('', SIGN_KEY)).toBe('')
  })
})
