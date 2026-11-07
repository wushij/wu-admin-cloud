// 最终验证：完整复刻前端签名流程，打网关真实存在的业务路径
const crypto = require('crypto')
const BASE = 'http://127.0.0.1:8080'
const clientId = 'final' + Date.now().toString(36)

// sm-crypto 只安装在 wu-admin-web/node_modules 下，脚本位于仓库根目录时无法直接 require。
// 这里显式从 PC 端依赖目录解析，避免静默降级成「空签名 → 全部 403」的假象。
function loadSm3 () {
  const candidates = [
    'sm-crypto',
    require('path').join(__dirname, '..', '..', 'wu-admin-web', 'node_modules', 'sm-crypto')
  ]
  for (const c of candidates) {
    try { return require(c).sm3 } catch (e) { /* 继续尝试下一个 */ }
  }
  throw new Error('无法加载 sm-crypto（尝试过: ' + candidates.join(', ') + '），请先在 wu-admin-web 执行 npm install')
}
const sm3 = loadSm3()

// 逐字复刻 wu-admin-web/src/utils/security/crypto.ts
function ensureSm4HexKey(rawKey) {
  if (rawKey.length === 32 && /^[0-9a-fA-F]{32}$/.test(rawKey)) return rawKey
  let hex = ''
  for (let i = 0; i < rawKey.length; i++) {
    hex += rawKey.charCodeAt(i).toString(16).padStart(2, '0')
  }
  return hex
}
function signHmacSm3(content, signKey) {
  if (!content || !signKey) return ''
  // 签名失败必须显式抛出：静默返回空串会让「带正确签名」的用例退化成 403，掩盖真实问题
  return sm3(content, { key: ensureSm4HexKey(signKey) })
}

async function call(path, method = 'GET', body = undefined) {
  const timestamp = Date.now().toString()
  const nonce = crypto.randomBytes(16).toString('hex')

  let fullPath = path
  if (fullPath.startsWith('/api/')) fullPath = fullPath.substring(4)
  else if (!fullPath.startsWith('/')) fullPath = '/' + fullPath
  try { fullPath = decodeURIComponent(fullPath) } catch {}

  let bodyStr = ''
  if (body !== undefined) {
    bodyStr = typeof body === 'string' ? body : JSON.stringify(body)
    bodyStr = bodyStr.trim()
    if (bodyStr.startsWith('"') && bodyStr.endsWith('"') && bodyStr.length > 2) {
      bodyStr = bodyStr.substring(1, bodyStr.length - 1)
    }
  }

  const signContent = `${method}\n${fullPath}\n${timestamp}\n${nonce}\n${bodyStr}`
  const signature = signHmacSm3(signContent, key)

  const headers = {
    'X-Timestamp': timestamp,
    'X-Nonce': nonce,
    'X-Client-Id': clientId,
    'X-Signature': signature,
  }
  const init = { method, headers }
  if (body !== undefined) {
    headers['Content-Type'] = 'application/json'
    init.body = typeof body === 'string' ? body : JSON.stringify(body)
  }

  const res = await fetch(`${BASE}/api${path}`, init)
  const text = await res.text()
  console.log(`${method} ${path}`.padEnd(42) + ` -> HTTP ${res.status}  ${text.slice(0, 110)}`)
  console.log(`   x-encrypted=${res.headers.get('x-encrypted')} payload=${res.headers.get('x-encrypted-payload')}`)
  return { res, text }
}

let key
async function main() {
  const initRes = await fetch(`${BASE}/api/auth/session-sign-init?clientId=${clientId}`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' },
  })
  const parsed = JSON.parse(await initRes.text())
  key = parsed?.data?.sm3SignKey
  console.log('会话密钥已下发:', !!key)
  console.log('')

  // 1) 无签名 —— 期望 403
  console.log('--- 无签名请求（期望 403 拦截）---')
  await (async () => {
    const r = await fetch(`${BASE}/api/system/user/page`, { method: 'GET' })
    const t = await r.text()
    console.log(`GET /system/user/page`.padEnd(42) + ` -> HTTP ${r.status}  ${t.slice(0, 110)}`)
  })()

  console.log('')
  console.log('--- 带正确签名（期望非 403）---')
  await call('/system/user/page?pageNo=1&pageSize=1')
  await call('/system/config-group/list')
  await call('/system/menu/list')

  // ===== 完整链路：登录 -> 带 Token + 签名 -> 调业务接口 =====
  console.log('')
  console.log('--- 完整链路（登录 + 签名 + Token）---')
  const loginBody = JSON.stringify({ username: 'admin', password: 'admin123', loginType: 'account' })
  const loginRes = await call('/auth/login', 'POST', loginBody)
  let token = ''
  try { token = JSON.parse(loginRes.text)?.data?.token || '' } catch (e) { /* 解析失败则保持空 */ }
  console.log('   token 长度:', token.length)

  if (token) {
    // 带 Token + 签名调用需要登录态的业务接口
    const ts = Date.now().toString()
    const nc = crypto.randomBytes(16).toString('hex')
    const p = 'system/user/page?pageNo=1&pageSize=1'
    const sig = signHmacSm3(`GET\n/${p}\n${ts}\n${nc}\n`, key)
    const r2 = await fetch(`${BASE}/api/${p}`, {
      method: 'GET',
      headers: {
        Authorization: token,
        'X-Timestamp': ts,
        'X-Nonce': nc,
        'X-Client-Id': clientId,
        'X-Signature': sig
      }
    })
    const t2 = await r2.text()
    console.log(`GET /${p}`.padEnd(42) + ` -> HTTP ${r2.status}  ${t2.slice(0, 110)}`)

    // 安全风控模块端点（本次修复的核心）
    for (const ep of ['/system/security/stats', '/system/security/events?pageNo=1&pageSize=5', '/system/security/risk-subjects?pageNo=1&pageSize=5']) {
      const ts2 = Date.now().toString()
      const nc2 = crypto.randomBytes(16).toString('hex')
      const sig2 = signHmacSm3(`GET\n/${ep}\n${ts2}\n${nc2}\n`, key)
      const r3 = await fetch(`${BASE}/api/${ep}`, {
        method: 'GET',
        headers: {
          Authorization: token,
          'X-Timestamp': ts2,
          'X-Nonce': nc2,
          'X-Client-Id': clientId,
          'X-Signature': sig2
        }
      })
      const t3 = await r3.text()
      console.log(`GET /${ep}`.padEnd(42) + ` -> HTTP ${r3.status}  ${t3.slice(0, 160)}`)
    }
  }
}

main().catch((e) => { console.error('ERR', e.message); process.exit(1) })
