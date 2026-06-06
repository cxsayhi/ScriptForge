import http, { unwrap } from './http'
import { exportMarkdown, exportYaml } from './export'
import { getNovelContent, submitNovelText, uploadNovelFile } from './novel'
import { getScriptSchema, getScriptSchemaDesign } from './schema'
import {
  generateScript,
  getScript,
  getScriptStatus,
  regenerateScript,
  repairScript,
  updateScript,
  validateScript
} from './script'
import { getAdaptationSetting, saveAdaptationSetting } from './setting'
import type { Project, ProjectId, ProjectPayload, ProjectUpdatePayload } from './types'

export type { ProjectPayload, ProjectUpdatePayload } from './types'

export function createProject(payload: ProjectPayload) {
  return unwrap<Project>(http.post('/projects', payload))
}

export function getProjectList() {
  return unwrap<Project[]>(http.get('/projects'))
}

export function getProjectDetail(projectId: ProjectId) {
  return unwrap<Project>(http.get(`/projects/${projectId}`))
}

export function updateProject(projectId: ProjectId, payload: ProjectUpdatePayload) {
  return unwrap<Project>(http.put(`/projects/${projectId}`, payload))
}

export function deleteProject(projectId: ProjectId) {
  return unwrap<void>(http.delete(`/projects/${projectId}`))
}

export const projectApi = {
  createProject,
  listProjects: getProjectList,
  getProject: getProjectDetail,
  updateProject,
  deleteProject,
  saveNovelText(projectId: ProjectId, text: string) {
    return submitNovelText(projectId, { text })
  },
  uploadNovelFile,
  getNovel: getNovelContent,
  getSettings: getAdaptationSetting,
  saveSettings: saveAdaptationSetting,
  generateScript,
  regenerateScript,
  getScript,
  saveScript(projectId: ProjectId, yaml: string) {
    return updateScript(projectId, { yaml })
  },
  validateScript(projectId: ProjectId, yaml: string) {
    return validateScript(projectId, { yaml })
  },
  repairScript(projectId: ProjectId, yaml: string) {
    return repairScript(projectId, { yaml })
  },
  getScriptStatus,
  getSchema: getScriptSchema,
  getSchemaDesign: getScriptSchemaDesign,
  exportYaml,
  exportMarkdown
}
