import { describe, it, expect } from 'vitest'
import {
  getJobTemplateVisual,
  tplIcon,
  groupTagType,
  cronPresets,
} from '@/views/monitor/job/constants/cronPresets'
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

describe('定时任务快捷模板视觉与图标体系测试', () => {
  it('应当正确匹配 7 个标准业务模板的专属图标与主题配色', () => {
    // 1. 过期日志归档
    const logsVisual = getJobTemplateVisual({ key: 'purge_logs' })
    expect(logsVisual.component).toBe(DocumentRemove)
    expect(logsVisual.color).toBe('#2563eb')

    // 2. 私聊消息清理
    const chatVisual = getJobTemplateVisual({ key: 'purge_chat' })
    expect(chatVisual.component).toBe(ChatDotRound)
    expect(chatVisual.color).toBe('#7c3aed')

    // 3. 群聊消息清理
    const groupChatVisual = getJobTemplateVisual({ key: 'purge_group_chat' })
    expect(groupChatVisual.component).toBe(ChatLineRound)
    expect(groupChatVisual.color).toBe('#4f46e5')

    // 4. 调度日志清理
    const jobLogVisual = getJobTemplateVisual({ key: 'purge_job_log' })
    expect(jobLogVisual.component).toBe(Timer)
    expect(jobLogVisual.color).toBe('#d97706')

    // 5. 已读通知清理
    const noticesVisual = getJobTemplateVisual({ key: 'purge_notices' })
    expect(noticesVisual.component).toBe(Bell)
    expect(noticesVisual.color).toBe('#ea580c')

    // 6. 工单回收站清理
    const ticketVisual = getJobTemplateVisual({ key: 'purge_ticket_recycle' })
    expect(ticketVisual.component).toBe(Tickets)
    expect(ticketVisual.color).toBe('#db2777')

    // 7. 文件回收站清理
    const fileVisual = getJobTemplateVisual({ key: 'purge_file_recycle' })
    expect(fileVisual.component).toBe(FolderDelete)
    expect(fileVisual.color).toBe('#059669')
  })

  it('当仅提供 icon 属性时，亦能正确识别并映射到对应视觉方案', () => {
    expect(getJobTemplateVisual({ icon: 'Document' }).component).toBe(DocumentRemove)
    expect(getJobTemplateVisual({ icon: 'DocumentRemove' }).component).toBe(DocumentRemove)
    expect(getJobTemplateVisual({ icon: 'Folder' }).component).toBe(FolderDelete)
    expect(getJobTemplateVisual({ icon: 'FolderDelete' }).component).toBe(FolderDelete)
    expect(getJobTemplateVisual({ icon: 'Timer' }).component).toBe(Timer)
  })

  it('当传入未知的模板或空对象时，能平滑降级为默认时钟调度图标', () => {
    const unknownVisual = getJobTemplateVisual({ key: 'custom_unknown_task' })
    expect(unknownVisual.component).toBe(Clock)
    expect(unknownVisual.color).toBe('#475569')

    const emptyVisual = getJobTemplateVisual(undefined)
    expect(emptyVisual.component).toBe(Clock)
  })

  it('tplIcon 兼容函数应当返回组件对象而非 emoji 字符串', () => {
    const comp = tplIcon('Timer')
    expect(typeof comp).toBe('object')
    expect(comp).toBe(Timer)
  })

  it('groupTagType 应当正确识别 SYSTEM 与其它任务组', () => {
    expect(groupTagType('SYSTEM')).toBe('warning')
    expect(groupTagType('DEFAULT')).toBe('info')
    expect(groupTagType(undefined)).toBe('info')
  })

  it('cronPresets 包含合法的预设项', () => {
    expect(cronPresets.length).toBeGreaterThan(0)
    expect(cronPresets[0].label).toBe('每 30 秒')
  })
})
