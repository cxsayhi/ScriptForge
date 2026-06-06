import { API_BASE } from '@/api/http'
import { exportProjectFile } from '@/api/export'
import type { ExportFormat, ProjectId } from '@/api/types'

export function exportUrl(projectId: ProjectId, format: ExportFormat) {
  return `${API_BASE}/projects/${projectId}/export/${format}`
}

export function downloadBlob(blob: Blob, filename: string) {
  const objectUrl = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = objectUrl
  link.download = filename
  link.click()
  URL.revokeObjectURL(objectUrl)
}

export async function downloadProjectExport(projectId: ProjectId, format: ExportFormat) {
  const file = await exportProjectFile(projectId, format)
  downloadBlob(file.blob, file.filename)
  return file
}
