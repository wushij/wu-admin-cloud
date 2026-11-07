const fs = require('fs')
const path = require('path')

function walk (d, acc = []) {
  for (const e of fs.readdirSync(d, { withFileTypes: true })) {
    const p = path.join(d, e.name)
    if (e.isDirectory()) {
      if (['node_modules', 'dist', 'unpackage', '.git', 'target'].includes(e.name)) continue
      walk(p, acc)
    } else acc.push(p)
  }
  return acc
}

const javaRoots = []
for (const root of ['wu-cloud-service', 'wu-cloud-gateway', 'wu-cloud-auth', 'wu-cloud-infra', 'wu-admin']) {
  const stack = [root]
  while (stack.length) {
    const cur = stack.pop()
    for (const e of fs.readdirSync(cur, { withFileTypes: true })) {
      if (!e.isDirectory()) continue
      const p = path.join(cur, e.name)
      if (['node_modules', 'dist', 'unpackage', '.git', 'target'].includes(e.name)) continue
      if (e.name === 'java' && path.basename(path.dirname(p)) === 'main') javaRoots.push(p)
      else stack.push(p)
    }
  }
}
const ends = []
for (const base of javaRoots) {
  for (const f of walk(base)) {
    if (!f.endsWith('.java')) continue
    const src = fs.readFileSync(f, 'utf8')
    const classRe = /\b(class|interface|enum)\s+(\w+)[^{]*\{/g
    let cm
    while ((cm = classRe.exec(src))) {
      let depth = 1
      let i = cm.index + cm[0].length
      while (i < src.length && depth > 0) {
        if (src[i] === '{') depth++
        else if (src[i] === '}') depth--
        i++
      }
      const body = src.slice(cm.index + cm[0].length, i - 1)
      const head = src.slice(0, cm.index)
      const rmm = [...head.matchAll(/@RequestMapping\(\s*(?:value\s*=\s*)?"([^"]*)"/g)]
      const pre = rmm.length ? rmm[rmm.length - 1][1] : ''
      const re = /@(Get|Post|Put|Delete|Patch)Mapping(?:\(\s*(?:value\s*=\s*)?"([^"]*)"[^)]*\))?/g
      let m
      while ((m = re.exec(body))) ends.push({ m: m[1].toUpperCase(), p: pre + (m[2] || ''), f })
    }
  }
}
function svcOfFile (f) {
  const s = f.replace(/\\/g, '/')
  const m = /wu-service-([a-z]+)\//.exec(s)
  if (m && s.includes('wu-cloud-service/')) return 'wu-service-' + m[1]
  if (s.includes('wu-cloud-gateway/')) return 'wu-cloud-gateway'
  if (s.includes('wu-cloud-auth/')) return 'wu-cloud-auth'
  if (s.includes('wu-cloud-infra/')) return 'wu-service-infra'
  if (s.includes('/wu-admin/') || s.startsWith('wu-admin/')) return 'wu-admin'
  return 'other'
}
const toPat = p => p.split('?')[0].replace(/\$\{[^}]*\}/g, '*').replace(/\{[^}]*\}/g, '*').replace(/\/+$/, '') || '/'
function patMatch (pat, p) {
  const a = pat.split('/')
  const b = p.split('/')
  if (a.length !== b.length) return false
  for (let i = 0; i < a.length; i++) {
    if (a[i] === '*' || a[i] === '**') continue
    if (a[i] !== b[i]) return false
  }
  return true
}
const globalEnds = ends.map(e => ({ ...e, pat: toPat(e.p) }))
const endIndex = new Map()
for (const e of ends) {
  const svc = svcOfFile(e.f)
  if (!endIndex.has(svc)) endIndex.set(svc, [])
  endIndex.get(svc).push({ m: e.m, pat: toPat(e.p), raw: e.p })
}

const gw = fs.readFileSync('wu-cloud-gateway/src/main/resources/application.yml', 'utf8')
const gwRoutes = [...gw.matchAll(/- id:\s*(\S+)[\s\S]*?- Path=([^\n]+)/g)].map(m => ({
  id: m[1], paths: m[2].split(',').map(s => s.trim())
}))
const ROUTE_SVC = {
  'wu-cloud-auth': 'wu-cloud-auth', 'wu-admin': 'wu-admin',
  'wu-service-ticket-export': 'wu-service-ticket', 'wu-service-ticket-biz': 'wu-service-ticket',
  'wu-service-system': 'wu-service-system', 'wu-service-file-group': 'wu-service-file',
  'wu-service-file': 'wu-service-file', 'wu-service-file-api-static': 'wu-service-file',
  'wu-service-file-static': 'wu-service-file', 'wu-service-infra': 'wu-service-infra',
  'wu-service-ai': 'wu-service-ai', 'wu-service-message': 'wu-service-message',
  'wu-service-message-ws': 'wu-service-message', 'wu-service-pay': 'wu-service-pay',
  'wu-service-order': 'wu-service-order', 'api-doc': 'wu-service-system'
}
function matchRoute (p) {
  for (const r of gwRoutes) {
    for (const seg of r.paths) {
      if (seg.endsWith('/**')) {
        const pre = seg.slice(0, -3)
        if (p === pre || p.startsWith(pre + '/')) return r
      } else if (seg === p) return r
    }
  }
  return null
}

// ===== 采集候选路径 =====
// 只认「真正的 API 调用点」，避免把 Vue Router 的 path 字段、
// 组件内部路由跳转（router.push）等误当成接口路径而产生大量假阳性。
// 判据：路径字面量出现在 API 调用包装函数里（get/post/put/del(...)、$http.xxx），
//       或出现在 axios/request 的 url 位置。
const API_CALL_RE = /\b(get|post|put|del|delete|patch)\s*[<(]\s*[`'"](\/[^`'"]*)[`'"]/g
// 兜底：url: '/xxx' / url = '/xxx' 形式（request 配置）
const URL_FIELD_RE = /\burl\s*[:=]\s*[`'"](\/[^`'"]*)[`'"]/g
// 明确排除：前端路由路径声明（path: '/xxx'）与跳转（router.push('/xxx')）
const ROUTER_PATH_RE = /\bpath\s*:\s*[`'"](\/[^`'"]*)[`'"]/g
const ROUTER_PUSH_RE = /router\.push\s*\(\s*[`'"](\/[^`'"]*)[`'"]/g

const lits = []
for (const root of ['wu-admin-uniapp/src/api', 'wu-admin-web/src/api', 'wu-admin-uniapp/src/composables',
  'wu-admin-web/src/views', 'wu-admin-web/src/utils', 'wu-admin-uniapp/src/utils',
  'wu-admin-web/src/components', 'wu-admin-uniapp/src/core']) {
  if (!fs.existsSync(root)) continue
  for (const f of walk(root)) {
    if (!/\.(ts|vue)$/.test(f)) continue
    const src = fs.readFileSync(f, 'utf8')
    // 记录被判定为「前端路由」的路径，后续排除
    const routerPaths = new Set()
    for (const re of [ROUTER_PATH_RE, ROUTER_PUSH_RE]) {
      re.lastIndex = 0
      let m
      while ((m = re.exec(src))) routerPaths.add(m[1])
    }
    let m
    API_CALL_RE.lastIndex = 0
    while ((m = API_CALL_RE.exec(src))) lits.push({ f, p: m[2], verb: m[1].toUpperCase(), via: 'call' })
    URL_FIELD_RE.lastIndex = 0
    while ((m = URL_FIELD_RE.exec(src))) {
      if (!routerPaths.has(m[1])) lits.push({ f, p: m[1], verb: null, via: 'url' })
    }
  }
}

const results = []
for (const l of lits) {
  const raw = l.p
  if (!/^\/(api|auth|system|infra|file|files|ai|message|ticket|approval|notice|pay|order|monitor|dashboard|admin|storage|export|doc|v3|webjars|swagger-ui)/.test(raw)) continue
  const full = raw.startsWith('/api/') ? raw : '/api' + raw
  // 纯运行时拼接（如 `/api${path}`、路径由调用方传入）无法静态判定，
  // 交给运行期验证，不作为「疑似缺失」报出，避免淹没真实问题。
  // 判据：去掉 /api 前缀后，路径的第一段就是变量，即整个路径由运行时决定。
  const afterApi = raw.replace(/^\/api\/?/, '')
  if (/^\$\{[^}]+\}/.test(afterApi)) continue
  if (full.includes('*')) {
    const pat = toPat(full)
    if (!globalEnds.find(e => patMatch(e.pat, pat))) {
      results.push({ ...l, full, reason: '动态路径无匹配端点' })
    }
    continue
  }
  const r = matchRoute(full)
  if (!r) {
    const globalHit = globalEnds.find(e => patMatch(e.pat, full))
    results.push({ ...l, full, reason: globalHit ? '网关无路由(端点存在于 ' + globalHit.f + ')' : '网关无路由且无端点' })
    continue
  }
  const svc = ROUTE_SVC[r.id]
  const list = endIndex.get(svc) || []
  const pat = toPat(full)
  const pathHit = list.filter(e => patMatch(e.pat, pat))
  if (!pathHit.length) {
    results.push({ ...l, full, reason: '服务 ' + svc + ' 内无此端点' })
    continue
  }
  // 已知 HTTP 方法时校验方法是否匹配
  if (l.verb && l.via === 'call') {
    const normVerb = l.verb === 'DEL' ? 'DELETE' : l.verb
    if (!pathHit.find(e => e.m === normVerb)) {
      results.push({
        ...l, full,
        reason: '方法不匹配(实际 ' + normVerb + '，后端为 ' + pathHit.map(e => e.m).join(',') + ')'
      })
    }
  }
}

const uniq = new Map()
for (const m of results) {
  const k = m.full + '|' + m.reason
  if (!uniq.has(k)) uniq.set(k, m)
}
console.log('扫描路径字面量', lits.length, '/ 疑似缺失', uniq.size, '\n')
for (const m of uniq.values()) {
  console.log(`${m.full}  [${m.reason}]`)
  console.log(`     <- ${m.f.replace(/\\/g, '/')}`)
}
