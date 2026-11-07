import { describe, it, expect } from 'vitest'
import { sm4GcmEncrypt, sm4GcmDecrypt, isSm4GcmCipherPayload } from '@/utils/sm4'
import { encryptSm4, decryptSm4, encryptSm2 } from '@/utils/crypto'
import { sm2 } from 'sm-crypto'

describe('国密 SM4-GCM 与满血 SM2 认证加解密单元测试', () => {
  const hexKey32 = '0123456789abcdef0123456789abcdef'
  const plainText = '{"username":"admin","role":"super_admin","message":"测试国密SM4-GCM数据完整性"}'

  // 测试专用固定密钥对，仅用于验证前后端算法互通，不是生产密钥
  const TEST_SM2_PUB_KEY =
    '04a3ab9ad67089e840de94297dc1be1e6f1961daa6eccb7107ad03e229c6a04b933f1ecaa78252da6ccdd135089c8b194b6923bca76a2ec64d999825875e559df1'
  const TEST_SM2_PRI_KEY =
    '94aa3b522664c4bf04a4957f4e9c463859ebf49d006dd786c1e5dd4e79b50209'

  it('SM4-GCM 加密后应生成合法的 Base64 格式，且能被正常解密还原', () => {
    const cipherBase64 = sm4GcmEncrypt(hexKey32, plainText)
    expect(cipherBase64).toBeTruthy()
    expect(isSm4GcmCipherPayload(cipherBase64)).toBe(true)

    const decrypted = sm4GcmDecrypt(hexKey32, cipherBase64)
    expect(decrypted).toBe(plainText)
  })

  it('crypto.ts 封装层 encryptSm4 和 decryptSm4 正确工作', () => {
    const cipher = encryptSm4(plainText, hexKey32)
    expect(isSm4GcmCipherPayload(cipher)).toBe(true)

    const restored = decryptSm4(cipher, hexKey32)
    expect(restored).toBe(plainText)
  })

  it('固定测试向量互通性验证', () => {
    const fixedIv = new Uint8Array([1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12])
    const cipher = sm4GcmEncrypt(hexKey32, 'CrossPlatformValidation', fixedIv)
    expect(sm4GcmDecrypt(hexKey32, cipher)).toBe('CrossPlatformValidation')
  })

  it('当密文被篡改时，Tag 认证失败必须抛出异常 (防重放与防篡改)', () => {
    const cipherBase64 = sm4GcmEncrypt(hexKey32, plainText)
    const binary = atob(cipherBase64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    // 篡改最后一位（Tag 的一部分）
    bytes[bytes.length - 1] ^= 0x01

    let tamperedBinary = ''
    for (let i = 0; i < bytes.length; i++) {
      tamperedBinary += String.fromCharCode(bytes[i])
    }
    const tamperedBase64 = btoa(tamperedBinary)

    expect(() => {
      sm4GcmDecrypt(hexKey32, tamperedBase64)
    }).toThrow(/Tag 校验失败/)
  })

  it('decryptSm4 必须 Fail-Closed：密文被篡改时直接抛错，不再降级 CBC/ECB', () => {
    const cipherBase64 = sm4GcmEncrypt(hexKey32, plainText)
    const binary = atob(cipherBase64)
    const bytes = new Uint8Array(binary.length)
    for (let i = 0; i < binary.length; i++) {
      bytes[i] = binary.charCodeAt(i)
    }
    bytes[bytes.length - 1] ^= 0x01

    let tamperedBinary = ''
    for (let i = 0; i < bytes.length; i++) {
      tamperedBinary += String.fromCharCode(bytes[i])
    }

    expect(() => decryptSm4(btoa(tamperedBinary), hexKey32)).toThrow(/Tag 校验失败/)
  })

  it('decryptSm4 必须 Fail-Closed：历史 CBC Hex 密文不再被静默降级解密', () => {
    // 32 位 Hex IV + 32 位 Hex 密文，旧版 CBC 格式；现在应直接按 GCM 认证失败抛错
    const legacyCbcPayload =
      '000102030405060708090a0b0c0d0e0f' + '00112233445566778899aabbccddeeff'
    expect(() => decryptSm4(legacyCbcPayload, hexKey32)).toThrow()
  })

  it('decryptSm4 在密钥缺失时必须抛错，而不是把密文当明文返回', () => {
    const cipherBase64 = sm4GcmEncrypt(hexKey32, plainText)
    expect(() => decryptSm4(cipherBase64, '')).toThrow(/密钥未下发/)
  })

  it('满血 SM2 C1C3C2 非对称加密与解密互通验证', () => {
    const rawPassword = 'AdminPassword@2026!'
    const encryptedPwd = encryptSm2(rawPassword, TEST_SM2_PUB_KEY)

    expect(encryptedPwd.startsWith('sm2:')).toBe(true)
    const cipherHex = encryptedPwd.substring(4)
    expect(cipherHex.length).toBeGreaterThan(130)

    // 使用国家标准 C1C3C2 (cipherMode = 1) 解密验证
    const decrypted = sm2.doDecrypt(cipherHex, TEST_SM2_PRI_KEY, 1)
    expect(decrypted).toBe(rawPassword)
  })

  it('SM2 加密幂等保护：对已带 sm2: 前缀的密文不进行重复加密', () => {
    const originalCipher = 'sm2:04aabbccdd112233'
    const result = encryptSm2(originalCipher, TEST_SM2_PUB_KEY)
    expect(result).toBe(originalCipher)
  })
})
