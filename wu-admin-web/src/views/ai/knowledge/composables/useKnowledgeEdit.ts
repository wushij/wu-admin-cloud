import { ref, reactive, computed } from 'vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import {
  getAiKnowledge,
  createAiKnowledge,
  updateAiKnowledge,
  type AiKnowledgeVO,
  type AiKnowledgeSaveDTO,
} from '@/api/system/ai-knowledge'
import { createDefaultKnowledgeForm, splitKeywords } from '../constants/knowledge'

export function useKnowledgeEdit(onSuccess?: () => void) {
  const editVisible = ref(false)
  const saving = ref(false)
  const formRef = ref<FormInstance>()
  const editorLayout = ref<'split' | 'editOnly' | 'previewOnly'>('split')

  const form = reactive<AiKnowledgeSaveDTO>(createDefaultKnowledgeForm())

  const fenceCount = computed(() => {
    return ((form.content || '').match(/```/g) || []).length
  })

  function validateFence(_r: unknown, value: string, callback: (e?: Error) => void) {
    if (!value) {
      callback()
      return
    }
    const count = (value.match(/```/g) || []).length
    if (count % 2 !== 0) {
      callback(new Error('正文中的代码块围栏 ``` 未成对闭合'))
    } else {
      callback()
    }
  }

  const rules: FormRules = {
    title: [{ required: true, message: '请输入知识标题', trigger: 'blur' }],
    content: [
      { required: true, message: '请输入知识正文', trigger: 'blur' },
      { validator: validateFence, trigger: 'blur' },
    ],
  }

  function openCreate() {
    Object.assign(form, createDefaultKnowledgeForm())
    editVisible.value = true
  }

  async function openEdit(row: AiKnowledgeVO) {
    const res = await getAiKnowledge(row.id)
    const data = res.data
    Object.assign(form, {
      id: data.id,
      title: data.title,
      keywords: data.keywords || '',
      content: data.content,
      category: data.category || 'faq',
      sort: data.sort ?? 0,
      status: data.status ?? 1,
    })
    editVisible.value = true
  }

  function handleClone(row: AiKnowledgeVO) {
    Object.assign(form, {
      id: undefined,
      title: `${row.title} (副本)`,
      keywords: row.keywords || '',
      content: row.content,
      category: row.category || 'faq',
      sort: row.sort ?? 0,
      status: 1,
    })
    editVisible.value = true
  }

  async function handleSubmit() {
    if (!formRef.value) return
    await formRef.value.validate()
    saving.value = true
    try {
      if (form.id) {
        await updateAiKnowledge(form)
        ElMessage.success('修改成功')
      } else {
        await createAiKnowledge(form)
        ElMessage.success('新增成功')
      }
      editVisible.value = false
      onSuccess?.()
    } finally {
      saving.value = false
    }
  }

  // 智能提炼关键词
  function autoExtractKeywords() {
    const source = `${form.title} ${form.content}`
    if (!source.trim()) {
      ElMessage.warning('请先输入知识标题或正文')
      return
    }
    const matched = source.match(/[\u4e00-\u9fa5]{2,6}|[a-zA-Z0-9_-]{3,12}/g) || []
    const stopWords = new Set(['可以', '如何', '进行', '用户', '点击', '页面', '如果', '请在', '通过', '完成', '之后', '并且'])
    const freqMap: Record<string, number> = {}
    for (const word of matched) {
      if (!stopWords.has(word)) {
        freqMap[word] = (freqMap[word] || 0) + 1
      }
    }
    const topWords = Object.keys(freqMap)
      .sort((a, b) => freqMap[b] - freqMap[a])
      .slice(0, 6)

    if (topWords.length > 0) {
      const existing = splitKeywords(form.keywords)
      const combined = Array.from(new Set([...existing, ...topWords]))
      form.keywords = combined.join(',')
      ElMessage.success(`已提炼补充 ${topWords.length} 个关键词`)
    } else {
      ElMessage.info('未提取到明显特征词，可手动输入')
    }
  }

  // 快捷插入模板
  function insertTemplate(type: 'faq' | 'manual' | 'rule') {
    let templateText = ''
    if (type === 'faq') {
      templateText = `## 问题现象\n描述用户在何种场景下遇到了该问题。\n\n## 解决步骤\n1. 进入对应操作页面；\n2. 检查相关配置参数是否正确；\n3. 点击保存并重新触发验证。\n\n> 提示：若依然无法解决，请提交工单联系系统管理员。`
    } else if (type === 'manual') {
      templateText = `## 操作指南\n本功能用于协助管理员或操作员完成日常业务。\n\n### 前置条件\n- 已拥有对应功能的操作权限。\n\n### 步骤说明\n1. 第一步：打开功能入口；\n2. 第二步：填写关键参数；\n3. 第三步：提交审批或确认生效。`
    } else {
      templateText = `## 业务规则说明\n\n| 规则名称 | 适用条件 | 动作与处理机制 |\n| :--- | :--- | :--- |\n| 规则 A | 条件 1 | 执行系统默认动作 |\n| 规则 B | 条件 2 | 触发人工审批审核 |\n\n> 注意事项：规则按从上到下优先级依次匹配。`
    }
    form.content = form.content ? `${form.content}\n\n${templateText}` : templateText
    ElMessage.success('已插入模板')
  }

  function insertFence() {
    const fence = '```text\n\n```'
    form.content = form.content ? `${form.content}\n${fence}\n` : `${fence}\n`
  }

  return {
    form,
    editVisible,
    saving,
    formRef,
    rules,
    fenceCount,
    editorLayout,
    openCreate,
    openEdit,
    handleClone,
    handleSubmit,
    autoExtractKeywords,
    insertTemplate,
    insertFence,
  }
}
