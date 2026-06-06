import http from './http'
import type { ExportFile, ExportFormat, ProjectId } from './types'

function defaultExportFilename(projectId: ProjectId, format: ExportFormat) {
  return `script-${projectId}.${format === 'yaml' ? 'yaml' : 'md'}`
}

function parseFilename(contentDisposition?: string) {
  const match = contentDisposition?.match(/filename="?([^"]+)"?/)
  return match?.[1]
}

export async function exportProjectFile(projectId: ProjectId, format: ExportFormat): Promise<ExportFile> {
  const response = await http.get<Blob>(`/projects/${projectId}/export/${format}`, {
    responseType: 'blob'
  })

  return {
    blob: response.data,
    filename: parseFilename(response.headers['content-disposition']) || defaultExportFilename(projectId, format),
    format
  }
}

export function exportYaml(projectId: ProjectId) {
  return exportProjectFile(projectId, 'yaml')
}

export function exportMarkdown(projectId: ProjectId) {
  return exportProjectFile(projectId, 'markdown')
}

export const exportApi = {
  exportProjectFile,
  exportYaml,
  exportMarkdown
}
