import http, { unwrap } from './http'
import type { NovelContent, NovelTextPayload, ProjectId } from './types'

export function uploadNovelFile(projectId: ProjectId, file: File) {
  const form = new FormData()
  form.append('file', file)
  return unwrap<NovelContent>(http.post(`/projects/${projectId}/novel/file`, form))
}

export function submitNovelText(projectId: ProjectId, payload: NovelTextPayload) {
  return unwrap<NovelContent>(http.post(`/projects/${projectId}/novel/text`, payload))
}

export function getNovelContent(projectId: ProjectId) {
  return unwrap<NovelContent>(http.get(`/projects/${projectId}/novel`))
}

export const novelApi = {
  uploadNovelFile,
  submitNovelText,
  getNovelContent
}
