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
    const ts = getTimestamp()
    expect(ts).toMatch(/^\d+$/)
    expect(Number(ts)).toBeGreaterThan(1_700_000_000_000)
  })

  it('encryptSm4 / decryptSm4 round-trip', () => {
    const plain = '{"hello":"world"}'
    const cipher = encryptSm4(plain, SM4_KEY)
    expect(cipher.length).toBeGreaterThan(32)
    expect(decryptSm4(cipher, SM4_KEY)).toBe(plain)
  })

  it('encryptSm4 rejects empty key', () => {
    expect(() => encryptSm4('x', '')).toThrow(/密钥/)
  })

  it('encryptSm4 empty plain returns empty', () => {
    expect(encryptSm4('', SM4_KEY)).toBe('')
  })

  it('signHmacSm3 is stable for same input', () => {
    const content = 'POST\n/system/user/list\n1\nn1\n{}'
    const a = signHmacSm3(content, SIGN_KEY)
    const b = signHmacSm3(content, SIGN_KEY)
    expect(a).toBeTruthy()
    expect(a).toBe(b)
    expect(signHmacSm3('', SIGN_KEY)).toBe('')
    expect(signHmacSm3(content, '')).toBe('')
  })
})
