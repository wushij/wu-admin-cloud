<template>
  <div class="app-container module-page ai-model-page">
    <el-card class="search-card module-hero-card" shadow="never">
      <div class="module-hero-row">
        <div class="module-hero-text">
          <div class="module-hero-title">
            <ModulePageIcon :icon="MODULE_PAGE_ICON.aiModel" />
            <span>AI 模型配置</span>
          </div>
          <p class="module-hero-desc">管理 AI wu助手接入的大模型供应商，支持 DeepSeek / Kimi / GLM / 自定义</p>
        </div>
        <div class="module-hero-stats">
          <div class="stat-num">{{ total }}</div>
          <div class="stat-label">模型总数</div>
        </div>
      </div>
    </el-card>

    <el-card class="search-card module-search-card" shadow="never">
      <el-form :model="queryParams" inline class="module-search-form">
        <el-form-item label="配置名称">
          <el-input v-model="queryParams.name" placeholder="请输入配置名称" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="供应商">
          <el-select v-model="queryParams.provider" placeholder="全部供应商" clearable style="width: 160px">
            <el-option v-for="p in PROVIDER_OPTIONS" :key="p.value" :label="p.label" :value="p.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="状态">
          <el-select v-model="queryParams.status" placeholder="全部状态" clearable style="width: 120px">
            <el-option label="启用" :value="1" />
            <el-option label="停用" :value="0" />
          </el-select>
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :icon="Search" @click="handleQuery">搜索</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-card>
      <template #header>
        <div class="card-header">
          <span>模型列表</span>
          <div class="header-actions">
            <el-button type="primary" :icon="Plus" v-permission="'system:ai-model:create'" @click="formDialogRef?.open()">新增模型</el-button>
          </div>
        </div>
      </template>

      <div v-loading="loading" class="model-grid">
        <el-empty v-if="!loading && list.length === 0" description="暂无模型配置，点击右上角新增模型" class="grid-empty" />
        <div v-for="item in list" :key="item.id" class="model-card" :class="{ 'is-disabled': item.status === 0 }">
          <div class="model-card-head">
            <div class="provider-badge" :style="{ background: providerMeta(item.provider).bg }">
              <AiProviderIcon :provider="item.provider" :size="28" />
            </div>
            <div class="model-card-title">
              <div class="name-row">
                <span class="name" :title="item.name">{{ item.name }}</span>
              </div>
              <div class="sub">{{ providerMeta(item.provider).label }} · {{ item.modelName }}</div>
            </div>
            <div class="status-switch-wrap">
              <el-switch
                :model-value="item.status === 1"
                :disabled="!canUpdate"
                @change="(v: boolean | string | number) => toggleStatus(item, Boolean(v))"
              />
              <span class="status-text" :class="{ active: item.status === 1 }">{{ item.status === 1 ? '启用' : '停用' }}</span>
            </div>
          </div>

          <div class="model-card-body">
            <div class="info-line">
              <span class="label">接口地址</span>
              <span class="value" :title="item.baseUrl">{{ item.baseUrl || '—' }}</span>
            </div>
            <div class="info-line">
              <span class="label">API Key</span>
              <div class="value">
                <span class="key-tag" :class="{ configured: item.hasApiKey }">
                  {{ item.hasApiKey ? (item.apiKeyMasked || '✓ 已配置') : '未配置' }}
                </span>
              </div>
            </div>
            <div class="info-line">
              <span class="label">模型参数</span>
              <span class="value">温度 {{ item.temperature ?? 0.7 }} · 最大 {{ item.maxTokens ?? 8192 }} Tokens</span>
            </div>
            <div v-if="testResults[item.id] !== undefined" class="test-result" :class="testResults[item.id]! >= 0 ? 'ok' : 'fail'">
              <el-icon v-if="testResults[item.id]! >= 0"><CircleCheckFilled /></el-icon>
              <el-icon v-else><CircleCloseFilled /></el-icon>
              <span>{{ testResults[item.id]! >= 0 ? `连接正常 · ${testResults[item.id]}ms` : '连接失败' }}</span>
            </div>
          </div>

          <div class="model-card-foot">
            <span v-if="item.isDefault === 1" class="default-badge">
              <el-icon :size="11"><Star /></el-icon>
              <span>默认</span>
            </span>
            <el-button
              size="small"
              class="action-btn"
              :loading="testingId === item.id"
              :icon="Connection"
              v-permission="'system:ai-model:test'"
              @click="handleTest(item)"
            >
              测试
            </el-button>
            <el-button
              v-if="item.isDefault !== 1"
              size="small"
              type="warning"
              plain
              class="action-btn"
              :icon="Star"
              :disabled="item.status !== 1"
              v-permission="'system:ai-model:update'"
              @click="handleSetDefault(item)"
            >
              设为默认
            </el-button>
            <el-button
              size="small"
              type="primary"
              plain
              class="action-btn"
              :icon="Edit"
              v-permission="'system:ai-model:update'"
              @click="formDialogRef?.open(item)"
            >
              编辑
            </el-button>
            <el-button
              size="small"
              type="danger"
              plain
              class="action-btn"
              :icon="Delete"
              :disabled="item.isDefault === 1"
              v-permission="'system:ai-model:delete'"
              @click="handleDelete(item)"
            >
              删除
            </el-button>
          </div>
        </div>
      </div>

      <el-pagination
        v-model:current-page="queryParams.pageNo"
        v-model:page-size="queryParams.pageSize"
        :total="total"
        :page-sizes="[8, 16, 32]"
        layout="total, sizes, prev, pager, next, jumper"
        class="table-pagination"
        @size-change="loadData"
        @current-change="loadData"
      />
    </el-card>

    <!-- 新增 / 编辑弹窗组件 -->
    <ModelFormDialog ref="formDialogRef" @saved="loadData" />
  </div>
</template>

<script setup lang="ts">
import { ref, reactive, computed, onMounted } from 'vue'
import {
  Search,
  Refresh,
  Plus,
  CircleCheckFilled,
  CircleCloseFilled,
  Connection,
  Star,
  Edit,
  Delete,
} from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import ModulePageIcon from '@/components/ModulePageIcon.vue'
import { MODULE_PAGE_ICON } from '@/constants/module-page-icons'
import { useUserStore } from '@/store/user'
import { useAiWuStore } from '@/store/aiWu'
import {
  pageAiModel,
  deleteAiModel,
  setDefaultAiModel,
  testAiModel,
  updateAiModel,
  type AiModelVO,
  type AiModelPageQuery,
} from '@/api/system/ai-model'
import AiProviderIcon from '@/components/AiProviderIcon.vue'
import ModelFormDialog from './components/ModelFormDialog.vue'
import { PROVIDER_OPTIONS, providerMeta } from './constants/model'

const userStore = useUserStore()
const canUpdate = computed(() =>
  (userStore.userInfo?.permissions || []).some((p: string) => p === '*:*:*' || p === 'system:ai-model:update'),
)

const formDialogRef = ref<InstanceType<typeof ModelFormDialog>>()

const loading = ref(false)
const total = ref(0)
const list = ref<AiModelVO[]>([])
const testingId = ref<number | null>(null)
/** id -> 延迟ms（-1 表示失败） */
const testResults = reactive<Record<number, number>>({})

const queryParams = reactive<AiModelPageQuery>({
  pageNo: 1,
  pageSize: 8,
  name: '',
  provider: '',
  status: null,
})

async function loadData() {
  loading.value = true
  try {
    const res = await pageAiModel({
      pageNo: queryParams.pageNo,
      pageSize: queryParams.pageSize,
      name: queryParams.name || undefined,
      provider: queryParams.provider || undefined,
      status: queryParams.status,
    })
    list.value = res.data?.list || []
    total.value = Number(res.data?.total) || 0
  } finally {
    loading.value = false
  }
}

function handleQuery() {
  queryParams.pageNo = 1
  loadData()
}

function resetQuery() {
  queryParams.name = ''
  queryParams.provider = ''
  queryParams.status = null
  handleQuery()
}

/* ---------- 卡片操作 ---------- */
async function handleTest(row: AiModelVO) {
  testingId.value = row.id
  try {
    const res = await testAiModel({ id: row.id })
    testResults[row.id] = Number(res.data) || 0
    ElMessage.success(`连接正常，延迟 ${res.data}ms`)
  } catch {
    testResults[row.id] = -1
  } finally {
    testingId.value = null
  }
}

async function handleSetDefault(row: AiModelVO) {
  await ElMessageBox.confirm(`确定将「${row.name}」设为默认模型吗？AI wu助手默认使用该模型对话。`, '提示', { type: 'warning' })
  await setDefaultAiModel(row.id)
  ElMessage.success('设置成功')
  loadData()
  useAiWuStore().loadModels()
}

async function toggleStatus(row: AiModelVO, enabled: boolean) {
  if (row.isDefault === 1 && !enabled) {
    ElMessage.warning('默认模型不能停用，请先设置其他默认模型')
    return
  }
  await updateAiModel({
    id: row.id,
    name: row.name,
    provider: row.provider,
    protocol: row.protocol || 'openai',
    modelName: row.modelName,
    baseUrl: row.baseUrl,
    pathTemplate: row.pathTemplate,
    extraHeaders: row.extraHeaders,
    temperature: row.temperature,
    maxTokens: row.maxTokens,
    systemPrompt: row.systemPrompt,
    isDefault: row.isDefault,
    status: enabled ? 1 : 0,
    remark: row.remark,
  })
  ElMessage.success(enabled ? '已启用' : '已停用')
  loadData()
}

async function handleDelete(row: AiModelVO) {
  await ElMessageBox.confirm(`确定要删除模型「${row.name}」吗？`, '提示', { type: 'warning' })
  await deleteAiModel(row.id)
  ElMessage.success('删除成功')
  loadData()
}

onMounted(() => loadData())
</script>

<style scoped lang="scss">
.model-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(360px, 1fr));
  gap: 20px;
  min-height: 120px;
}

.grid-empty {
  grid-column: 1 / -1;
}

.model-card {
  border: 1px solid var(--el-border-color-lighter, #e2e8f0);
  border-radius: 16px;
  padding: 18px;
  display: flex;
  flex-direction: column;
  gap: 14px;
  background: var(--el-bg-color, #ffffff);
  box-shadow: 0 2px 10px rgba(15, 23, 42, 0.03);
  transition: border-color 0.2s, box-shadow 0.2s;

  &:hover {
    border-color: var(--el-color-primary-light-5, #818cf8);
    box-shadow: 0 4px 16px rgba(15, 23, 42, 0.08);
  }

  &.is-disabled {
    opacity: 0.65;
  }
}

.model-card-head {
  display: flex;
  align-items: center;
  gap: 12px;
}

.provider-badge {
  width: 44px;
  height: 44px;
  border-radius: 13px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 15px;
  font-weight: 800;
  flex-shrink: 0;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.06);
}

.model-card-title {
  flex: 1;
  min-width: 0;

  .name-row {
    display: flex;
    align-items: center;
    gap: 8px;
    min-width: 0;
  }

  .name {
    flex: 1;
    min-width: 0;
    font-size: 15px;
    font-weight: 700;
    color: var(--el-text-color-primary, #0f172a);
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }

  .sub {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-top: 3px;
    font-size: 12px;
    color: var(--el-text-color-secondary, #64748b);
    min-width: 0;
  }
}

.default-badge {
  display: inline-flex;
  align-items: center;
  gap: 3px;
  padding: 2px 8px;
  border-radius: 20px;
  background: linear-gradient(135deg, #f59e0b 0%, #d97706 100%);
  color: #ffffff;
  font-size: 10.5px;
  font-weight: 600;
  box-shadow: 0 2px 6px rgba(245, 158, 11, 0.3);
  flex-shrink: 0;
}

.status-switch-wrap {
  display: flex;
  align-items: center;
  gap: 6px;
  flex-shrink: 0;

  .status-text {
    font-size: 12px;
    color: #94a3b8;
    font-weight: 500;

    &.active {
      color: var(--theme-primary, #6366f1);
      font-weight: 600;
    }
  }
}

.model-card-body {
  display: flex;
  flex-direction: column;
  gap: 8px;
  padding: 12px 14px;
  border-radius: 12px;
  background: var(--el-fill-color-lighter, rgba(248, 250, 252, 0.8));
  border: 1px solid rgba(226, 232, 240, 0.7);

  .info-line {
    display: flex;
    align-items: center;
    gap: 10px;
    font-size: 12px;
    line-height: 1.6;

    .label {
      flex-shrink: 0;
      width: 58px;
      color: var(--el-text-color-secondary, #64748b);
      font-weight: 500;
    }

    .value {
      flex: 1;
      min-width: 0;
      color: var(--el-text-color-regular, #334155);
      overflow: hidden;
      text-overflow: ellipsis;
      white-space: nowrap;
    }
  }
}

.key-tag {
  display: inline-block;
  padding: 1px 8px;
  border-radius: 6px;
  font-size: 11px;
  font-family: Consolas, Monaco, monospace;
  background: #f1f5f9;
  color: #64748b;
  border: 1px solid #e2e8f0;

  &.configured {
    background: rgba(16, 185, 129, 0.1);
    color: #059669;
    border-color: rgba(16, 185, 129, 0.25);
    font-weight: 600;
  }
}

.test-result {
  display: inline-flex;
  align-items: center;
  gap: 5px;
  font-size: 11.5px;
  font-weight: 500;
  padding: 3px 8px;
  border-radius: 6px;
  margin-top: 2px;
  width: fit-content;

  &.ok {
    background: #f0fdf4;
    color: #16a34a;
    border: 1px solid #bbf7d0;
  }

  &.fail {
    background: #fef2f2;
    color: #dc2626;
    border: 1px solid #fecaca;
  }
}

.model-card-foot {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 8px;
  border-top: 1px dashed rgba(226, 232, 240, 0.8);
  padding-top: 12px;

  .action-btn {
    border-radius: 8px;
    height: 28px;
    padding: 0 10px;
    font-size: 12px;
    transition: all 0.2s;
  }
}
</style>
