import { describe, expect, it } from 'vitest'
import {
  cleanMarkdownText,
  formatBytes,
  formatDate,
  formatPercent,
  formatUserStatus,
  summarizeText,
} from '@/utils/format'

describe('format', () => {
  it('formatDate / formatBytes / formatPercent', () => {
    expect(formatDate('2026-08-12T10:00:00')).toBe('2026-08-12')
    expect(formatBytes(512)).toBe('512 B')
    expect(formatBytes(2048)).toBe('2.0 KB')
    expect(formatPercent(12.345, 1)).toBe('12.3%')
    expect(formatPercent(null)).toBe('—')
  })

  it('summarizeText strips tags and truncates', () => {
    expect(summarizeText('<b>hello</b> world', 5)).toBe('hello…')
  })

  it('cleanMarkdownText', () => {
    expect(cleanMarkdownText('**粗体** # 标题')).toBe('粗体 标题')
  })

  it('formatUserStatus', () => {
    expect(formatUserStatus(1)).toBe('正常')
    expect(formatUserStatus(0)).toBe('已停用')
    expect(formatUserStatus(99)).toBe('未知')
  })
})
