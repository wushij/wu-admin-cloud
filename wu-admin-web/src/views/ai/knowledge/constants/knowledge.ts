import type { AiKnowledgeCategory, AiKnowledgeSaveDTO } from '@/api/system/ai-knowledge'

/** 知识分类字典映射 */
export const CATEGORY_LABELS: Record<string, string> = {
  faq: '常见问题',
  manual: '操作手册',
  module: '模块说明',
  other: '其它',
}

/** 常用试炼提问预设 */
export const QUICK_TEST_QUERIES = [
  '忘记密码如何找回？',
  '怎么提交与跟进工单？',
  '系统支持哪些 AI 大模型？',
  '怎么修改个人中心资料？',
]

/** 表格对齐样式常量 */
export const TABLE_CELL_STYLE = { textAlign: 'center' as const }
export const TABLE_HEADER_STYLE = { textAlign: 'center' as const }

/** 初始表单默认值 */
export const createDefaultKnowledgeForm = (): AiKnowledgeSaveDTO => ({
  id: undefined,
  title: '',
  keywords: '',
  content: '',
  category: 'faq',
  sort: 0,
  status: 1,
})

/** 获取分类对应的 Tag 颜色类型 */
export function getCategoryTagType(cat?: string | AiKnowledgeCategory) {
  switch (cat) {
    case 'faq':
      return 'primary'
    case 'manual':
      return 'success'
    case 'module':
      return 'warning'
    default:
      return 'info'
  }
}

/** 截取正文纯文本摘要 */
export function getContentSnippet(content?: string, maxLen = 55) {
  if (!content) return '—'
  const text = content.replace(/[#*`>\-_~]/g, '').trim()
  return text.length > maxLen ? text.slice(0, maxLen) + '...' : text
}

/** 拆解逗号分隔的关键词列表 */
export function splitKeywords(kw?: string): string[] {
  if (!kw || !kw.trim()) return []
  return kw
    .split(/[,，]/)
    .map((s) => s.trim())
    .filter(Boolean)
}
