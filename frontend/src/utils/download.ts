import { API_BASE } from '@/api/http'

export function exportUrl(projectId: number, format: 'yaml' | 'markdown') {
  return `${API_BASE}/projects/${projectId}/export/${format}`
}
