import { ref } from 'vue'
import { getConfigGroup } from '@/api/system/config'

export function useLayoutSite() {
  const sitePlatformName = ref('Admin Platform')
  const sitePlatformSubtitle = ref('Management System')

  async function loadSiteConfig() {
    try {
      const res = await getConfigGroup('site', { silent403: true } as any)
      if (res.data?.configValue) {
        const parsed = JSON.parse(res.data.configValue)
        if (parsed.platformName) sitePlatformName.value = parsed.platformName
        if (parsed.platformSubtitle) sitePlatformSubtitle.value = parsed.platformSubtitle
      }
    } catch {
      /* 使用默认值 */
    }
  }

  return { sitePlatformName, sitePlatformSubtitle, loadSiteConfig }
}
