import { describe, expect, it } from 'vitest'
import {
  fileExtLabel,
  fileIconTheme,
  fileSuffix,
  isImage,
  isPdf,
  isPreviewable,
  isVideo,
} from '@/utils/file-type'
import type { FileRecord } from '@/api/system/file/index'

function file(partial: FileRecord): FileRecord {
  return partial
}

describe('file-type', () => {
  it('classifies by mime and suffix', () => {
    expect(isImage(file({ fileType: 'image/png' }))).toBe(true)
    expect(isImage(file({ originalName: 'a.jpg' }))).toBe(true)
    expect(isVideo(file({ fileType: 'video/mp4' }))).toBe(true)
    expect(isPdf(file({ fileType: 'application/pdf' }))).toBe(true)
    expect(isPreviewable(file({ originalName: 'note.txt' }))).toBe(true)
  })

  it('fileSuffix / fileExtLabel / fileIconTheme', () => {
    expect(fileSuffix(file({ fileSuffix: 'PNG' }))).toBe('.png')
    expect(fileExtLabel(file({ originalName: 'doc.pdf' }))).toBe('PDF')
    expect(fileIconTheme(file({ originalName: 'a.docx' }))).toBe('doc')
    expect(fileIconTheme(file({ originalName: 'a.zip' }))).toBe('archive')
  })
})
