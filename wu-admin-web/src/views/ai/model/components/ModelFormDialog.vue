<template>
  <el-dialog
    v-model="visible"
    :title="form.id ? '编辑模型' : '新增模型'"
    width="640px"
    top="6vh"
    class="ai-model-dialog"
    destroy-on-close
    @closed="resetForm"
  >
    <el-form ref="formRef" :model="form" :rules="rules" :validate-on-rule-change="false" label-width="100px">
      <el-form-item label="供应商" prop="provider">
        <div class="provider-select">
          <div
            v-for="p in PROVIDER_OPTIONS"
            :key="p.value"
            class="provider-item"
            :class="{ active: form.provider === p.value }"
            @click="selectProvider(p.value)"
          >
            <span class="provider-item-badge" :style="{ background: providerMeta(p.value).bg }">
              <AiProviderIcon :provider="p.value" :size="16" />
            </span>
            <span>{{ p.label }}</span>
          </div>
        </div>
      </el-form-item>
      <el-form-item v-if="form.provider === 'custom'" label="接入协议" prop="protocol">
        <el-select
          v-model="form.protocol"
          placeholder="请选择接入协议"
          style="width: 100%"
          @change="onProtocolChange"
        >
          <el-option
            v-for="p in PROTOCOL_OPTIONS"
            :key="p.value"
            :label="p.label"
            :value="p.value"
          />
        </el-select>
      </el-form-item>
      <el-form-item label="配置名称" prop="name">
        <el-input v-model="form.name" :placeholder="form.provider === 'custom' ? '如：公司网关 / 本地 Ollama' : '如：' + providerMeta(form.provider).label + ' 官方'" maxlength="50" show-word-limit />
      </el-form-item>
      <el-form-item prop="modelName">
        <template #label>
          <span class="model-name-label">
            模型名称
            <el-tooltip content="点击「获取模型」从接口拉取可用模型；拉取失败时也可直接手动输入" placement="top">
              <el-icon><QuestionFilled /></el-icon>
            </el-tooltip>
          </span>
        </template>
        <div class="model-name-row">
          <el-select
            v-model="form.modelName"
            class="model-name-select"
            filterable
            allow-create
            default-first-option
            clearable
            :validate-event="false"
            placeholder="选择或输入模型名称"
          >
            <el-option v-for="m in modelOptions" :key="m" :label="m" :value="m" />
          </el-select>
          <el-button
            :loading="fetchingModels"
            :icon="Refresh"
            v-permission="'system:ai-model:test'"
            @click="handleFetchModels"
          >
            获取模型
          </el-button>
        </div>
      </el-form-item>
      <el-form-item label="接口地址" prop="baseUrl">
        <el-input v-model="form.baseUrl" :placeholder="effectiveBaseUrlPlaceholder" />
      </el-form-item>
      <el-form-item v-if="form.provider === 'custom'" label="路径模板">
        <el-input
          v-model="form.pathTemplate"
          maxlength="500"
          placeholder="留空使用协议默认路径，支持 {model} 占位"
        />
      </el-form-item>
      <el-form-item v-if="form.provider === 'custom'" label="额外请求头">
        <el-input
          v-model="form.extraHeaders"
          type="textarea"
          :rows="2"
          maxlength="1000"
          placeholder='可选，JSON 对象，如 {"HTTP-Referer":"https://example.com"}'
        />
      </el-form-item>
      <el-form-item prop="apiKey">
        <template #label>
          <span class="api-key-label" :class="{ required: !form.id }">API Key</span>
        </template>
        <el-input
          v-model="form.apiKey"
          type="password"
          show-password
          :placeholder="form.id && editHasKey ? '留空表示不修改已保存的 Key' : '请输入 API Key'"
          autocomplete="new-password"
        />
        <el-link
          v-if="providerMeta(form.provider).portalUrl"
          type="primary"
          :href="providerMeta(form.provider).portalUrl"
          target="_blank"
          class="key-portal-link"
        >
          获取 {{ providerMeta(form.provider).label }} API Key
        </el-link>
      </el-form-item>
      <el-form-item label="温度">
        <div class="slider-row">
          <el-slider v-model="form.temperature" :min="0" :max="2" :step="0.1" style="flex: 1" />
          <span class="slider-value">{{ form.temperature?.toFixed(1) }}</span>
          <el-tooltip content="控制 AI 回答的随机性与创造力：0 偏严谨确定，1 偏均衡，2 偏富有想象力" placement="top">
            <el-icon style="color: var(--el-text-color-secondary); cursor: pointer;"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </el-form-item>
      <el-form-item label="最大 Tokens">
        <div style="display: flex; align-items: center; gap: 8px;">
          <el-input-number v-model="form.maxTokens" :min="128" :max="32768" :step="256" style="width: 180px" />
          <el-tooltip content="限制 AI 单次回答生成的最大文本长度（1000 Tokens ≈ 700 汉字），默认 8192；推理类模型建议 16384 以上" placement="top">
            <el-icon style="color: var(--el-text-color-secondary); cursor: pointer;"><QuestionFilled /></el-icon>
          </el-tooltip>
        </div>
      </el-form-item>
      <el-form-item label="系统提示词">
        <el-input
          v-model="form.systemPrompt"
          type="textarea"
          :rows="3"
          maxlength="2000"
          show-word-limit
          placeholder="可选，留空使用内置人设"
        />
      </el-form-item>
      <div class="form-inline-row">
        <el-form-item label="设为默认" class="inline-item">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" :disabled="editIsDefault" />
        </el-form-item>
        <el-form-item label="状态" class="inline-item">
          <el-radio-group v-model="form.status">
            <el-radio :value="1">启用</el-radio>
            <el-radio :value="0">停用</el-radio>
          </el-radio-group>
        </el-form-item>
      </div>
      <el-form-item label="备注">
        <el-input v-model="form.remark" placeholder="可选" maxlength="200" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button :loading="formTesting" v-permission="'system:ai-model:test'" @click="handleFormTest">测试连接</el-button>
      <el-button @click="visible = false">取消</el-button>
      <el-button type="primary" :loading="submitting" @click="handleSubmit">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, reactive, computed, nextTick } from 'vue'
import { Refresh, QuestionFilled } from '@element-plus/icons-vue'
import { ElMessage, type FormInstance, type FormRules } from 'element-plus'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import {
  createAiModel,
  updateAiModel,
  testAiModel,
  listAiModels,
  type AiModelVO,
  type AiModelSaveDTO,
  type AiProvider,
} from '@/api/system/ai-model'
import {
  PROVIDER_META,
  PROVIDER_OPTIONS,
  PROTOCOL_OPTIONS,
  providerMeta,
} from '../constants/model'

const emit = defineEmits<{
  (e: 'saved'): void
}>()

const visible = ref(false)
const submitting = ref(false)
const formTesting = ref(false)
const fetchingModels = ref(false)
const fetchedModels = ref<string[]>([])
const formRef = ref<FormInstance>()
const editHasKey = ref(false)
const editIsDefault = ref(false)

const defaultForm = (): AiModelSaveDTO & { temperature: number; maxTokens: number } => ({
  id: undefined,
  name: '',
  provider: 'deepseek',
  protocol: 'openai',
  modelName: PROVIDER_META.deepseek.models[0] || 'deepseek-v4.1-flash',
  baseUrl: PROVIDER_META.deepseek.baseUrl,
  pathTemplate: '',
  extraHeaders: '',
  apiKey: '',
  temperature: 0.7,
  maxTokens: 8192,
  systemPrompt: '',
  isDefault: 0,
  status: 1,
  remark: '',
})

const form = reactive(defaultForm())

const effectiveBaseUrlPlaceholder = computed(() => {
  if (form.provider === 'custom') {
    return form.protocol === 'anthropic'
      ? '如：https://api.anthropic.com'
      : '如：https://api.example.com/v1'
  }
  return `如：${providerMeta(form.provider).baseUrl || 'https://api.example.com/v1'}`
})

const rules: FormRules = {
  provider: [{ required: true, message: '请选择供应商', trigger: 'change' }],
  name: [{ required: true, message: '请输入配置名称', trigger: 'blur' }],
  modelName: [{ required: true, message: '请输入模型名称', trigger: 'change' }],
  baseUrl: [{ required: true, message: '请输入接口地址', trigger: 'blur' }],
}

const modelOptions = computed(() => {
  return Array.from(new Set([...fetchedModels.value, ...providerMeta(form.provider).models].filter(Boolean)))
})

function selectProvider(provider: AiProvider) {
  if (form.provider === provider) return
  form.provider = provider
  const meta = providerMeta(provider)
  form.baseUrl = meta.baseUrl
  form.modelName = meta.models[0] || ''
  form.protocol = 'openai'
  form.pathTemplate = ''
  form.extraHeaders = ''
  fetchedModels.value = []
  nextTick(() => formRef.value?.clearValidate())
}

function onProtocolChange() {
  nextTick(() => formRef.value?.clearValidate())
}

function open(row?: AiModelVO) {
  Object.assign(form, defaultForm())
  editHasKey.value = false
  editIsDefault.value = false
  fetchedModels.value = []
  if (row) {
    Object.assign(form, {
      id: row.id,
      name: row.name,
      provider: row.provider,
      protocol: row.protocol || 'openai',
      modelName: row.modelName,
      baseUrl: row.baseUrl,
      pathTemplate: row.pathTemplate || '',
      extraHeaders: row.extraHeaders || '',
      apiKey: '',
      temperature: row.temperature ?? 0.7,
      maxTokens: row.maxTokens ?? 8192,
      systemPrompt: row.systemPrompt || '',
      isDefault: row.isDefault ?? 0,
      status: row.status ?? 1,
      remark: row.remark || '',
    })
    editHasKey.value = !!row.hasApiKey
    editIsDefault.value = row.isDefault === 1
  }
  visible.value = true
  nextTick(() => formRef.value?.clearValidate())
}

function resetForm() {
  formRef.value?.clearValidate()
}

async function handleSubmit() {
  await formRef.value?.validate()
  if (!form.id && !form.apiKey?.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  submitting.value = true
  try {
    const data: AiModelSaveDTO = { ...form, apiKey: form.apiKey || undefined }
    if (form.id) {
      await updateAiModel(data)
      ElMessage.success('修改成功')
    } else {
      await createAiModel(data)
      ElMessage.success('新增成功')
    }
    visible.value = false
    emit('saved')
  } finally {
    submitting.value = false
  }
}

async function handleFormTest() {
  const valid = await formRef.value?.validateField(['provider', 'modelName', 'baseUrl']).catch(() => false)
  if (valid === false) return
  if (!form.id && !form.apiKey?.trim()) {
    ElMessage.warning('请输入 API Key')
    return
  }
  formTesting.value = true
  try {
    const res = await testAiModel({
      id: form.id,
      provider: form.provider,
      protocol: form.protocol,
      modelName: form.modelName,
      baseUrl: form.baseUrl,
      pathTemplate: form.pathTemplate,
      extraHeaders: form.extraHeaders,
      apiKey: form.apiKey || undefined,
    })
    ElMessage.success(`连接正常，延迟 ${res.data}ms`)
  } finally {
    formTesting.value = false
  }
}

async function handleFetchModels() {
  const valid = await formRef.value?.validateField(['provider', 'baseUrl']).catch(() => false)
  if (valid === false) return
  if (!form.id && !form.apiKey?.trim()) {
    ElMessage.warning('请先输入 API Key')
    return
  }
  fetchingModels.value = true
  try {
    const res = await listAiModels({
      id: form.id,
      provider: form.provider,
      protocol: form.protocol,
      baseUrl: form.baseUrl,
      pathTemplate: form.pathTemplate,
      extraHeaders: form.extraHeaders,
      apiKey: form.apiKey || undefined,
    })
    const models = (res.data || []).filter((item) => !!item)
    if (models.length === 0) {
      ElMessage.warning('接口未返回可用模型，请手动输入模型名称')
      return
    }
    fetchedModels.value = models
    if (!form.modelName || !models.includes(form.modelName)) {
      form.modelName = models[0]
      nextTick(() => formRef.value?.clearValidate('modelName'))
    }
    ElMessage.success(`获取到 ${models.length} 个模型`)
  } finally {
    fetchingModels.value = false
  }
}

defineExpose({
  open,
})
</script>

<style scoped lang="scss">
.provider-select {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10px;
  width: 100%;
}

.provider-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  border: 1px solid var(--el-border-color);
  border-radius: 8px;
  cursor: pointer;
  transition: all 0.2s;
  background: var(--el-bg-color);

  &:hover {
    border-color: var(--el-color-primary-light-3);
  }

  &.active {
    border-color: var(--el-color-primary);
    background: var(--el-color-primary-light-9);
    font-weight: 600;
    color: var(--el-color-primary);
  }
}

.provider-item-badge {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 6px;
}

.model-name-label {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.model-name-row {
  display: flex;
  gap: 8px;
  width: 100%;
}

.model-name-select {
  flex: 1;
}

.api-key-label {
  &.required::before {
    content: '*';
    color: var(--el-color-danger);
    margin-right: 4px;
  }
}

.key-portal-link {
  font-size: 12px;
  margin-top: 4px;
  display: inline-block;
}

.slider-row {
  display: flex;
  align-items: center;
  gap: 12px;
  width: 100%;
}

.slider-value {
  width: 32px;
  text-align: right;
  font-size: 13px;
  color: var(--el-text-color-regular);
}

.form-inline-row {
  display: flex;
  gap: 24px;
}

.inline-item {
  margin-bottom: 18px;
}
</style>

