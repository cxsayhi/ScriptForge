import http, { unwrap } from './http'
import type {
  AdaptationSetting,
  GenerationStatus,
  NovelContent,
  Project,
  RepairResponse,
  ScriptResponse,
  ValidationResult
} from './types'

export interface ProjectPayload {
  title: string
  description?: string
}

export const projectApi = {
  createProject(payload: ProjectPayload) {
    return unwrap<Project>(http.post('/projects', payload))
  },
  listProjects() {
    return unwrap<Project[]>(http.get('/projects'))
  },
  getProject(projectId: number) {
    return unwrap<Project>(http.get(`/projects/${projectId}`))
  },
  updateProject(projectId: number, payload: ProjectPayload & { status?: string }) {
    return unwrap<Project>(http.put(`/projects/${projectId}`, payload))
  },
  deleteProject(projectId: number) {
    return unwrap<void>(http.delete(`/projects/${projectId}`))
  },
  saveNovelText(projectId: number, text: string) {
    return unwrap<NovelContent>(http.post(`/projects/${projectId}/novel/text`, { text }))
  },
  uploadNovelFile(projectId: number, file: File) {
    const form = new FormData()
    form.append('file', file)
    return unwrap<NovelContent>(http.post(`/projects/${projectId}/novel/file`, form))
  },
  getNovel(projectId: number) {
    return unwrap<NovelContent>(http.get(`/projects/${projectId}/novel`))
  },
  getSettings(projectId: number) {
    return unwrap<AdaptationSetting>(http.get(`/projects/${projectId}/settings`))
  },
  saveSettings(projectId: number, payload: AdaptationSetting) {
    return unwrap<AdaptationSetting>(http.put(`/projects/${projectId}/settings`, payload))
  },
  generateScript(projectId: number) {
    return unwrap<ScriptResponse>(http.post(`/projects/${projectId}/script/generate`))
  },
  getScript(projectId: number) {
    return unwrap<ScriptResponse>(http.get(`/projects/${projectId}/script`))
  },
  saveScript(projectId: number, yaml: string) {
    return unwrap<ScriptResponse>(http.put(`/projects/${projectId}/script`, { yaml }))
  },
  validateScript(projectId: number, yaml: string) {
    return unwrap<ValidationResult>(http.post(`/projects/${projectId}/script/validate`, { yaml }))
  },
  repairScript(projectId: number, yaml: string) {
    return unwrap<RepairResponse>(http.post(`/projects/${projectId}/script/repair`, { yaml }))
  },
  getScriptStatus(projectId: number) {
    return unwrap<GenerationStatus>(http.get(`/projects/${projectId}/script/status`))
  },
  getSchema() {
    return unwrap<string>(http.get('/schema/script'))
  },
  getSchemaDesign() {
    return unwrap<string>(http.get('/schema/script/design'))
  }
}
