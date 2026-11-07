export const SECURITY_TABS = [
  { key: 'events', label: '拦截事件' },
  { key: 'risk', label: '风险主体' },
]

export const PRIMARY_EVENT_TYPE_OPTIONS = [
  { label: '全部', value: '' },
  { label: '爬虫风控', value: 'UA_BLOCK' },
  { label: '蜜罐诱导', value: 'HONEYPOT' },
]

export const EXTRA_EVENT_TYPE_OPTIONS = [
  { label: '重放攻击', value: 'ANTI_REPLAY' },
  { label: '自动封禁', value: 'RISK_BLOCK' },
  { label: '越权探测', value: 'PERMISSION_DENIED' },
  { label: '签名篡改', value: 'SIGN_FAIL' },
  { label: '登录失败', value: 'LOGIN_FAIL' },
  { label: '滑块失败', value: 'CAPTCHA_FAIL' },
  { label: '时间戳超窗', value: 'TIMESTAMP_INVALID' },
  { label: '超大请求体', value: 'BODY_TOO_LARGE' },
  { label: '翻页遍历', value: 'PAGE_SCAN' },
  { label: '批量导出', value: 'BULK_EXPORT' },
  { label: '风控挑战', value: 'RISK_CHALLENGE' },
]

export const RISK_LEVEL_OPTIONS: Array<{ label: string; value: number | undefined }> = [
  { label: '全部', value: undefined },
  { label: '关注', value: 1 },
  { label: '警告', value: 2 },
  { label: '挑战', value: 3 },
  { label: '临时封禁', value: 4 },
  { label: '最高档封禁', value: 5 },
]

export const RISK_BLOCKED_OPTIONS: Array<{ label: string; value: boolean | undefined }> = [
  { label: '全部', value: undefined },
  { label: '已封禁', value: true },
  { label: '未封禁', value: false },
]

export const BLOCK_DURATION_OPTIONS = [
  { label: '1 小时', value: 3600 },
  { label: '6 小时', value: 21600 },
  { label: '24 小时', value: 86400 },
  { label: '7 天', value: 604800 },
  { label: '30 天', value: 2592000 },
]

export function formatEventTypeName(type: string): string {
  switch (type) {
    case 'UA_BLOCK':
      return '爬虫工具拦截'
    case 'ANTI_REPLAY':
      return '重放攻击拦截'
    case 'SIGN_FAIL':
      return '数字签名篡改'
    case 'TIMESTAMP_INVALID':
      return '时间戳超窗'
    case 'BODY_TOO_LARGE':
      return '超大请求体'
    case 'RISK_BLOCK':
      return '风控累计封禁'
    case 'HONEYPOT':
      return '蜜罐诱捕触发'
    case 'LOGIN_FAIL':
      return '登录失败'
    case 'CAPTCHA_FAIL':
      return '滑块验证失败'
    case 'CAPTCHA_ABUSE':
      return '滑块连续失败'
    case 'PERMISSION_DENIED':
      return '越权探测'
    case 'PAGE_SCAN':
      return '翻页遍历'
    case 'BULK_EXPORT':
      return '批量导出'
    case 'RISK_CHALLENGE':
      return '风控挑战'
    default:
      return type || '未知事件'
  }
}

export function eventTypeTone(type: string): 'warning' | 'danger' | 'primary' | 'default' {
  switch (type) {
    case 'UA_BLOCK':
    case 'CAPTCHA_FAIL':
    case 'LOGIN_FAIL':
      return 'warning'
    case 'TIMESTAMP_INVALID':
    case 'BODY_TOO_LARGE':
    case 'RISK_CHALLENGE':
      return 'primary'
    default:
      return 'danger'
  }
}

export function formatAction(action: string): string {
  switch (action) {
    case 'FORBIDDEN':
      return '触发封禁'
    case 'CHALLENGE':
      return '要求验证'
    case 'WARNING':
      return '安全告警'
    case 'BLOCKED':
      return '已阻断(403)'
    default:
      return action || '未知'
  }
}

export function actionTone(action: string): 'warning' | 'danger' | 'primary' | 'default' {
  switch (action) {
    case 'FORBIDDEN':
      return 'danger'
    case 'CHALLENGE':
    case 'WARNING':
      return 'warning'
    default:
      return 'default'
  }
}

export function formatRemainingSeconds(sec: number): string {
  if (!sec || sec <= 0) return '已到期'
  if (sec < 60) return `${sec} 秒`
  const minutes = Math.floor(sec / 60)
  if (minutes < 60) return `${minutes} 分钟`
  const hours = Math.floor(minutes / 60)
  const remMinutes = minutes % 60
  if (hours < 24) return `${hours} 小时 ${remMinutes} 分`
  const days = Math.floor(hours / 24)
  const remHours = hours % 24
  return `${days} 天 ${remHours} 小时`
}

