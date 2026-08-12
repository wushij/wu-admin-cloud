import { beforeEach, vi } from 'vitest'

const storage = new Map<string, string>()

function arrayBufferToBase64(buffer: ArrayBuffer): string {
  const bytes = new Uint8Array(buffer)
  let binary = ''
  for (let i = 0; i < bytes.length; i++) {
    binary += String.fromCharCode(bytes[i]!)
  }
  return btoa(binary)
}

function base64ToArrayBuffer(base64: string): ArrayBuffer {
  const binary = atob(base64)
  const bytes = new Uint8Array(binary.length)
  for (let i = 0; i < binary.length; i++) {
    bytes[i] = binary.charCodeAt(i)
  }
  return bytes.buffer
}

const uniMock = {
  getStorageSync: (key: string) => storage.get(key) ?? '',
  setStorageSync: (key: string, value: unknown) => {
    storage.set(key, typeof value === 'string' ? value : String(value))
  },
  removeStorageSync: (key: string) => {
    storage.delete(key)
  },
  arrayBufferToBase64,
  base64ToArrayBuffer,
}

vi.stubGlobal('uni', uniMock)

beforeEach(() => {
  storage.clear()
})
