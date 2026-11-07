<template>
  <el-dialog
    :model-value="modelValue"
    title="上传 Markdown (.md) 文档知识库"
    width="680px"
    destroy-on-close
    class="md-upload-dialog"
    :before-close="handleBeforeClose"
    @update:model-value="(val) => $emit('update:modelValue', val)"
  >
    <div class="upload-dialog-body">
      <!-- 提示引导横幅 -->
      <el-alert
        type="info"
        :closable="false"
        show-icon
        class="guide-alert"
      >
        <template #title>
          <span>上传企业规章制度、系统操作手册等 Markdown 文档。</span>
        </template>
        <template #default>
          <div class="alert-desc">
            支持同时拖拽多个 <b>.md</b> 文件；系统将自动以文件名作为文档标题，AI 助手在用户提问时将优先检索本批文档进行回答。若存在同名文档将自动覆盖更新。
          </div>
        </template>
      </el-alert>

      <!-- 拖拽上传核心触发区 -->
      <div
        class="drag-upload-box"
        :class="{ 'is-dragover': isDragOver }"
        @click="triggerFileInput"
        @dragover.prevent="onDragOver"
        @dragleave.prevent="onDragLeave"
        @drop.prevent="onDropFiles"
      >
        <input
          ref="fileInputRef"
          type="file"
          accept=".md,.markdown"
          multiple
          style="display: none"
          @change="handleFileInputChange"
        />
        <div class="upload-icon-circle">
          <el-icon class="icon-upload"><Upload /></el-icon>
        </div>
        <div class="upload-primary-text">
          <span>点击选择或直接将 <b>.md 文档</b> 拖入此处</span>
        </div>
        <div class="upload-sub-text">
          支持 UTF-8 编码的 Markdown (.md / .markdown) 文件，支持多选批量上传
        </div>
      </div>

      <!-- 待入库文档清单卡片 -->
      <div v-if="parsedFiles.length > 0" class="parsed-files-section">
        <div class="section-header">
          <span class="section-title">
            待入库文档清单 (共 <b>{{ parsedFiles.length }}</b> 篇)
          </span>
          <el-button link type="danger" size="small" @click="clearFiles">
            清空列表
          </el-button>
        </div>

        <el-scrollbar max-height="240px">
          <div class="file-card-list">
            <div
              v-for="(item, idx) in parsedFiles"
              :key="idx"
              class="file-item-card"
            >
              <div class="file-card-left">
                <div class="file-md-badge">MD</div>
                <div class="file-info-text">
                  <div class="file-title-line">
                    <span class="file-title">{{ item.title }}</span>
                    <span class="file-size-tag">{{ formatSize(item.file.size) }}</span>
                    <span class="file-words-tag">{{ item.content.length.toLocaleString() }} 字</span>
                  </div>
                  <div class="file-preview-line">
                    {{ item.previewText }}
                  </div>
                </div>
              </div>

              <div class="file-card-right">
                <el-popconfirm
                  :title="`确定移除待入库文档「${item.title}」吗？`"
                  confirm-button-text="移除"
                  cancel-button-text="取消"
                  confirm-button-type="danger"
                  width="220"
                  @confirm="removeFile(idx)"
                >
                  <template #reference>
                    <el-button
                      link
                      type="danger"
                      :icon="Close"
                      title="移除该文档"
                      @click.stop
                    />
                  </template>
                </el-popconfirm>
              </div>
            </div>
          </div>
        </el-scrollbar>
      </div>
    </div>

    <template #footer>
      <div class="dialog-footer-row">
        <span v-if="parsedFiles.length > 0" class="selected-summary">
          已解析 {{ parsedFiles.length }} 篇文档，累计 {{ totalSelectedWords.toLocaleString() }} 字
        </span>
        <span v-else />
        <div class="footer-btns">
          <el-button @click="handleCancel">取消</el-button>
          <el-button
            type="primary"
            :disabled="parsedFiles.length === 0"
            :loading="uploading"
            @click="submitUpload"
          >
            确认上传入库 ({{ parsedFiles.length }} 篇)
          </el-button>
        </div>
      </div>
    </template>
  </el-dialog>
</template>

<script setup lang="ts">
import { ref, computed } from 'vue'
import { Upload, Close } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { uploadAiKnowledgeMd } from '@/api/system/ai-knowledge'

defineProps<{
  modelValue: boolean
}>()

const emit = defineEmits<{
  (e: 'update:modelValue', val: boolean): void
  (e: 'success'): void
}>()

interface ParsedDocItem {
  file: File
  title: string
  content: string
  previewText: string
}

const fileInputRef = ref<HTMLInputElement | null>(null)
const isDragOver = ref(false)
const uploading = ref(false)
const parsedFiles = ref<ParsedDocItem[]>([])

const totalSelectedWords = computed(() => {
  return parsedFiles.value.reduce((acc, item) => acc + item.content.length, 0)
})

function triggerFileInput() {
  if (fileInputRef.value) {
    fileInputRef.value.value = ''
    fileInputRef.value.click()
  }
}

function onDragOver() {
  isDragOver.value = true
}

function onDragLeave() {
  isDragOver.value = false
}

function onDropFiles(e: DragEvent) {
  isDragOver.value = false
  if (e.dataTransfer && e.dataTransfer.files) {
    handleFiles(Array.from(e.dataTransfer.files))
  }
}

function handleFileInputChange(e: Event) {
  const target = e.target as HTMLInputElement
  if (target.files) {
    handleFiles(Array.from(target.files))
  }
}

async function handleFiles(files: File[]) {
  const validFiles = files.filter((f) => {
    const name = f.name.toLowerCase()
    return name.endsWith('.md') || name.endsWith('.markdown')
  })

  if (validFiles.length < files.length) {
    ElMessage.warning(`已过滤掉 ${files.length - validFiles.length} 个非 .md 格式的文件`)
  }

  if (validFiles.length === 0) return

  for (const file of validFiles) {
    try {
      const text = await readFileAsText(file)
      let title = file.name
      if (title.toLowerCase().endsWith('.md')) {
        title = title.slice(0, -3)
      } else if (title.toLowerCase().endsWith('.markdown')) {
        title = title.slice(0, -9)
      }

      // 生成首段纯文本预览
      const preview = text
        .replace(/^#+\s+/gm, '')
        .replace(/[*_`~>\[\]\(\)]/g, '')
        .replace(/\s+/g, ' ')
        .trim()
        .slice(0, 75)

      // 避免重复加入同一文件名
      const existsIdx = parsedFiles.value.findIndex((p) => p.file.name === file.name)
      const newItem: ParsedDocItem = {
        file,
        title,
        content: text,
        previewText: preview || '正文暂无预览',
      }
      if (existsIdx >= 0) {
        parsedFiles.value[existsIdx] = newItem
      } else {
        parsedFiles.value.push(newItem)
      }
    } catch {
      ElMessage.error(`读取文件 ${file.name} 失败`)
    }
  }
}

function readFileAsText(file: File): Promise<string> {
  return new Promise((resolve, reject) => {
    const reader = new FileReader()
    reader.onload = () => resolve(String(reader.result || ''))
    reader.onerror = () => reject(reader.error)
    reader.readAsText(file, 'utf-8')
  })
}

function removeFile(index: number) {
  const removed = parsedFiles.value[index]
  parsedFiles.value.splice(index, 1)
  ElMessage.info(`已移除待入库文档「${removed?.title || '未命名'}」`)
}

async function clearFiles() {
  if (parsedFiles.value.length === 0) return
  try {
    await ElMessageBox.confirm(
      `确定要清空已解析的全部 ${parsedFiles.value.length} 篇待入库文档吗？清空后需重新拖拽选择。`,
      '清空确认',
      {
        confirmButtonText: '确认清空',
        cancelButtonText: '取消',
        confirmButtonClass: 'el-button--danger',
        type: 'warning',
      }
    )
    parsedFiles.value = []
    ElMessage.info('已清空待入库文档清单')
  } catch {
    // 用户取消清空，无副作用
  }
}

function handleBeforeClose(done: () => void) {
  if (uploading.value) return
  if (parsedFiles.value.length > 0) {
    ElMessageBox.confirm(
      `当前有 ${parsedFiles.value.length} 篇已解析文档尚未确认上传入库，直接关闭将放弃本次解析，确定退出吗？`,
      '退出确认',
      {
        confirmButtonText: '确定退出',
        cancelButtonText: '继续上传',
        type: 'warning',
      }
    )
      .then(() => {
        parsedFiles.value = []
        done()
      })
      .catch(() => {
        // 用户选择继续上传，保持对话框打开
      })
  } else {
    done()
  }
}

async function handleCancel() {
  if (uploading.value) return
  if (parsedFiles.value.length > 0) {
    try {
      await ElMessageBox.confirm(
        `当前有 ${parsedFiles.value.length} 篇已解析文档尚未确认上传入库，直接关闭将放弃本次解析，确定退出吗？`,
        '退出确认',
        {
          confirmButtonText: '确定退出',
          cancelButtonText: '继续上传',
          type: 'warning',
        }
      )
      parsedFiles.value = []
      emit('update:modelValue', false)
    } catch {
      // 用户取消
    }
  } else {
    emit('update:modelValue', false)
  }
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return bytes + ' B'
  if (bytes < 1024 * 1024) return (bytes / 1024).toFixed(1) + ' KB'
  return (bytes / (1024 * 1024)).toFixed(1) + ' MB'
}

async function submitUpload() {
  if (parsedFiles.value.length === 0) return
  uploading.value = true
  try {
    const filesToUpload = parsedFiles.value.map((item) => item.file)
    const res = await uploadAiKnowledgeMd(filesToUpload)
    const count = res.data?.total || filesToUpload.length
    ElMessage.success(`成功入库 ${count} 篇 Markdown 知识文档！`)
    parsedFiles.value = []
    emit('update:modelValue', false)
    emit('success')
  } catch (err: unknown) {
    const message = err instanceof Error ? err.message : '上传失败，请重试'
    ElMessage.error(message)
  } finally {
    uploading.value = false
  }
}
</script>

<style scoped lang="scss">
.md-upload-dialog {
  :deep(.el-dialog__body) {
    padding: 20px 24px;
  }
}

.guide-alert {
  margin-bottom: 16px;
  border-radius: 8px;

  .alert-desc {
    margin-top: 4px;
    font-size: 12.5px;
    line-height: 1.5;
    color: #475569;
  }
}

.drag-upload-box {
  border: 1.5px dashed #cbd5e1;
  border-radius: 12px;
  background: #f8fafc;
  padding: 32px 20px;
  text-align: center;
  cursor: pointer;
  transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

  &:hover,
  &.is-dragover {
    border-color: #3b82f6;
    background: #f0f7ff;

    .upload-icon-circle {
      transform: scale(1.06);
      background: #dbeafe;
      border-color: #bfdbfe;
      color: #1d4ed8;
      box-shadow: 0 4px 14px rgba(37, 99, 235, 0.12);
    }
  }

  .upload-icon-circle {
    width: 52px;
    height: 52px;
    margin: 0 auto 14px;
    border-radius: 50%;
    background: #eff6ff;
    border: 1px solid #dbeafe;
    color: #2563eb;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1);

    .icon-upload {
      font-size: 26px;
    }
  }

  .upload-primary-text {
    font-size: 14.5px;
    font-weight: 600;
    color: #1e293b;
    margin-bottom: 6px;

    b {
      color: #2563eb;
      font-weight: 600;
    }
  }

  .upload-sub-text {
    font-size: 12.5px;
    color: #94a3b8;
  }
}

.parsed-files-section {
  margin-top: 20px;

  .section-header {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin-bottom: 10px;

    .section-title {
      font-size: 13.5px;
      font-weight: 600;
      color: #334155;
      b {
        color: #2563eb;
      }
    }
  }
}

.file-card-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.file-item-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 14px;
  background: #ffffff;
  border: 1px solid #e2e8f0;
  border-radius: 8px;
  transition: all 0.15s ease;

  &:hover {
    border-color: #bfdbfe;
    background: #f8fafc;
  }

  .file-card-left {
    display: flex;
    align-items: center;
    gap: 12px;
    flex: 1;
    min-width: 0;

    .file-md-badge {
      width: 30px;
      height: 30px;
      min-width: 30px;
      border-radius: 6px;
      background: #f8fafc;
      border: 1px solid #e2e8f0;
      color: #475569;
      font-size: 10.5px;
      font-weight: 700;
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .file-info-text {
      flex: 1;
      min-width: 0;

      .file-title-line {
        display: flex;
        align-items: center;
        gap: 8px;
        margin-bottom: 3px;

        .file-title {
          font-size: 13.5px;
          font-weight: 600;
          color: #0f172a;
          white-space: nowrap;
          overflow: hidden;
          text-overflow: ellipsis;
        }

        .file-size-tag,
        .file-words-tag {
          font-size: 11px;
          color: #64748b;
          background: #f1f5f9;
          padding: 1px 6px;
          border-radius: 4px;
          white-space: nowrap;
        }
      }

      .file-preview-line {
        font-size: 11.5px;
        color: #94a3b8;
        white-space: nowrap;
        overflow: hidden;
        text-overflow: ellipsis;
      }
    }
  }

  .file-card-right {
    margin-left: 12px;
  }
}

.dialog-footer-row {
  display: flex;
  align-items: center;
  justify-content: space-between;

  .selected-summary {
    font-size: 12.5px;
    color: #64748b;
  }
}
</style>
