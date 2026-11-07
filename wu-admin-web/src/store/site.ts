import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getConfig } from '@/api/system/auth'
import { setSecurityConfig } from '@/utils/security-config'

let configPromise: Promise<void> | null = null

export const useSiteStore = defineStore('site', () => {
  const disableDevtool = ref(false)
  const aiAssistantEnabled = ref(true)
  const configLoaded = ref(false)
  const siteConfig = ref<{
    platformName?: string
    platformSubtitle?: string
    loginWelcome?: string
    registerTitle?: string
    copyright?: string
    icpEnabled?: boolean
    icpNumber?: string
    icpUrl?: string
  }>({
    platformName: 'Admin Platform',
    platformSubtitle: '统一运维 · 高效管控',
    loginWelcome: 'Welcome',
    registerTitle: 'Sign Up',
    copyright: '',
    icpEnabled: false,
    icpNumber: '',
    icpUrl: 'https://beian.miit.gov.cn'
  })

  async function loadConfig() {
    configPromise = (async () => {
      try {
        const res = await getConfig()
        const data = res.data
        if (data?.site) {
          siteConfig.value = {
            ...siteConfig.value,
            ...data.site
          }
        }
        disableDevtool.value = data?.security?.disableDevtool === true
        aiAssistantEnabled.value = data?.ai?.assistantEnabled !== false

        // 从公开配置获取「签名/加密是否开启」标志与 SM2 公钥
        const sm3SignEnabled = data?.security?.sm3SignEnabled === true
        const sm4EncryptEnabled = data?.security?.sm4EncryptEnabled === true
        const sm2EncryptEnabled = data?.security?.sm2EncryptEnabled !== false
        const sm2PublicKey = data?.security?.sm2PublicKey || ''

        // 先将开关状态与 SM2 公钥写入内存（SM4/SM3会话密钥字段暂空，待登录后自动下发）
        setSecurityConfig({
          sm4EncryptEnabled,
          sm3SignEnabled,
          sm2SignEnabled: sm3SignEnabled,
          sm2EncryptEnabled,
          sm2PublicKey,
        })
      } catch {
        disableDevtool.value = false
        aiAssistantEnabled.value = true
      } finally {
        configLoaded.value = true
        configPromise = null
      }
    })()
    return configPromise
  }

  function ensureConfigLoaded() {
    if (configLoaded.value) return Promise.resolve()
    return loadConfig()
  }

  function setDisableDevtool(value: boolean) {
    disableDevtool.value = value
  }

  return {
    disableDevtool,
    aiAssistantEnabled,
    configLoaded,
    siteConfig,
    loadConfig,
    ensureConfigLoaded,
    setDisableDevtool,
  }
})
