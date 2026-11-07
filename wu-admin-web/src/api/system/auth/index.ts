import { get, post } from '@/utils/request'
import type { AuthInfo, LoginForm, LoginResult, RegisterForm } from '@/types/api'
import type { AuthPublicConfig } from '@/types/config'
import type { SliderTrackEvent } from '@/types/slider-captcha'

export type AuthConfig = AuthPublicConfig

export interface CaptchaResult {
  uuid: string
  img?: string
  image?: string
}

/**
 * 滑块挑战：服务端已把答案（缺口 X）渲染进背景图，响应中不再包含 targetX。
 */
export interface SliderChallengeResult {
  token: string
  /** 已挖出缺口的背景图（data:image/jpeg;base64） */
  background: string
  /** 缺口位置的拼图块（data:image/png;base64） */
  piece: string
  width: number
  height: number
  pieceSize: number
  pieceTop: number
  expireAt?: number
}

export interface SliderVerifyResult {
  /** 一次性验证票据，业务接口以 uuid 字段提交 */
  ticket: string
  expireAt?: number
}

/** 会话签名密钥初始化返回结果 */
export interface SessionSignResult {
  /** 服务端签名功能是否已开启 */
  enabled: boolean
  /** HMAC-SM3 签名密钥（enabled=true 且签名开启时有值，仅存内存） */
  sm3SignKey?: string
  /** 会话 SM4 对称密钥（32 位 Hex，enabled=true 且加密开启时有值） */
  sm4Key?: string
  /** 临时密钥有效期（分钟） */
  ttlMinutes?: number
  ttlHours?: number
}

export function getSliderChallenge(scene: string = 'login') {
  return get<SliderChallengeResult>('/auth/slider-challenge', { scene })
}

/**
 * 提交拖动结果，服务端校验轨迹与缺口位置后签发一次性票据。
 * 票据仅在 2 分钟内有效且只能消费一次。
 */
export function verifySlider(data: {
  token: string
  offsetX: number
  durationMs: number
  events: SliderTrackEvent[]
}) {
  return post<SliderVerifyResult>('/auth/slider-verify', data)
}

export function login(data: LoginForm) {
  return post<LoginResult>('/auth/login', data)
}

export function getInfo() {
  return get<AuthInfo>('/auth/info')
}

export function getCaptcha(scene: string = 'login') {
  return get<CaptchaResult>('/auth/captcha', { scene })
}

export function register(data: RegisterForm) {
  return post<unknown>('/auth/register', data)
}

export function getConfig() {
  return get<AuthConfig>('/auth/config')
}

export function sendSmsCode(phone: string, slider?: { uuid: string; code: string }) {
  const payload: { phone: string; uuid?: string; code?: string } = { phone }
  if (slider) {
    payload.uuid = slider.uuid
    payload.code = slider.code
  }
  return post<boolean>('/auth/sms-code', payload)
}

export function sendEmailCode(email: string, slider?: { uuid: string; code: string }) {
  const payload: { email: string; uuid?: string; code?: string } = { email }
  if (slider) {
    payload.uuid = slider.uuid
    payload.code = slider.code
  }
  return post<boolean>('/auth/email-code', payload)
}

export function logout() {
  return post<unknown>('/auth/logout')
}

/**
 * 初始化会话签名密钥（安全加固 P0）。
 * 携带内存随机 clientId 向服务端申请一个 30 分钟有效期的临时 HMAC-SM3 签名密钥，
 * 该密钥仅存于内存闭包，不写入任何持久化存储。
 */
export function sessionSignInit(clientId: string) {
  return post<SessionSignResult>(`/auth/session-sign-init?clientId=${encodeURIComponent(clientId)}`)
}
