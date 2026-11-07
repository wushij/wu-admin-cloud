import type { Component } from 'vue'
import {
  DocumentRemove,
  ChatDotRound,
  ChatLineRound,
  Timer,
  Bell,
  Tickets,
  FolderDelete,
  Clock,
} from '@element-plus/icons-vue'
import * as ElementPlusIconsVue from '@element-plus/icons-vue'

export const cronPresets = [
  { label: '每 30 秒', value: '0/30 * * * * ?' },
  { label: '每分钟', value: '0 * * * * ?' },
  { label: '每 5 分钟', value: '0 0/5 * * * ?' },
  { label: '每天 0 点', value: '0 0 0 * * ?' },
  { label: '每天 2:30', value: '0 30 2 * * ?' },
  { label: '每周一 9 点', value: '0 0 9 ? * MON' },
]

export interface JobTemplateVisual {
  component: Component
  color: string
  bgColor: string
  borderColor: string
  tagType?: '' | 'success' | 'warning' | 'danger' | 'info'
}

/**
 * 定时任务快捷模板的视觉配置（贴合业务语义的精致图标与主题配色）
 */
export const TEMPLATE_VISUAL_MAP: Record<string, JobTemplateVisual> = {
  // 1. 过期日志归档
  purge_logs: {
    component: DocumentRemove,
    color: '#2563eb',
    bgColor: 'rgba(37, 99, 235, 0.08)',
    borderColor: 'rgba(37, 99, 235, 0.2)',
    tagType: 'info',
  },
  // 2. 私聊消息清理
  purge_chat: {
    component: ChatDotRound,
    color: '#7c3aed',
    bgColor: 'rgba(124, 58, 237, 0.08)',
    borderColor: 'rgba(124, 58, 237, 0.2)',
    tagType: 'warning',
  },
  // 3. 群聊消息清理
  purge_group_chat: {
    component: ChatLineRound,
    color: '#4f46e5',
    bgColor: 'rgba(79, 70, 229, 0.08)',
    borderColor: 'rgba(79, 70, 229, 0.2)',
    tagType: 'warning',
  },
  // 4. 调度日志清理
  purge_job_log: {
    component: Timer,
    color: '#d97706',
    bgColor: 'rgba(217, 119, 6, 0.08)',
    borderColor: 'rgba(217, 119, 6, 0.2)',
    tagType: 'warning',
  },
  // 5. 已读通知清理
  purge_notices: {
    component: Bell,
    color: '#ea580c',
    bgColor: 'rgba(234, 88, 12, 0.08)',
    borderColor: 'rgba(234, 88, 12, 0.2)',
    tagType: 'danger',
  },
  // 6. 工单回收站清理
  purge_ticket_recycle: {
    component: Tickets,
    color: '#db2777',
    bgColor: 'rgba(219, 39, 119, 0.08)',
    borderColor: 'rgba(219, 39, 119, 0.2)',
    tagType: 'danger',
  },
  // 7. 文件回收站清理
  purge_file_recycle: {
    component: FolderDelete,
    color: '#059669',
    bgColor: 'rgba(5, 150, 105, 0.08)',
    borderColor: 'rgba(5, 150, 105, 0.2)',
    tagType: 'success',
  },
}

/**
 * 依据模板配置（key 或 icon 名称）动态计算贴合优雅的图标与微样式
 */
export function getJobTemplateVisual(tpl?: { key?: string; icon?: string }): JobTemplateVisual {
  if (tpl?.key && TEMPLATE_VISUAL_MAP[tpl.key]) {
    return TEMPLATE_VISUAL_MAP[tpl.key]
  }

  const iconName = tpl?.icon?.trim()
  if (iconName) {
    if (iconName === 'Document' || iconName === 'DocumentRemove') return TEMPLATE_VISUAL_MAP.purge_logs
    if (iconName === 'ChatDotRound') return TEMPLATE_VISUAL_MAP.purge_chat
    if (iconName === 'ChatLineRound') return TEMPLATE_VISUAL_MAP.purge_group_chat
    if (iconName === 'Timer') return TEMPLATE_VISUAL_MAP.purge_job_log
    if (iconName === 'Bell') return TEMPLATE_VISUAL_MAP.purge_notices
    if (iconName === 'Tickets') return TEMPLATE_VISUAL_MAP.purge_ticket_recycle
    if (iconName === 'Folder' || iconName === 'FolderDelete') return TEMPLATE_VISUAL_MAP.purge_file_recycle

    const dynamicComp = (ElementPlusIconsVue as Record<string, Component>)[iconName]
    if (dynamicComp) {
      return {
        component: dynamicComp,
        color: '#475569',
        bgColor: 'rgba(71, 85, 105, 0.08)',
        borderColor: 'rgba(71, 85, 105, 0.2)',
        tagType: 'info',
      }
    }
  }

  return {
    component: Clock,
    color: '#475569',
    bgColor: 'rgba(71, 85, 105, 0.08)',
    borderColor: 'rgba(71, 85, 105, 0.2)',
    tagType: 'info',
  }
}

/**
 * 兼容旧导出：已弃用 emoji，返回对应的图标组件
 */
export function tplIcon(icon?: string): Component {
  return getJobTemplateVisual({ icon }).component
}

export function groupTagType(group?: string) {
  return group === 'SYSTEM' ? 'warning' : 'info'
}
