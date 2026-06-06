import http, { unwrap } from './http'
import type {
  GenerationStatus,
  ProjectId,
  RepairResponse,
  ScriptResponse,
  ScriptUpdatePayload,
  ValidationResult
} from './types'

export function generateScript(projectId: ProjectId) {
  return unwrap<ScriptResponse>(http.post(`/projects/${projectId}/script/generate`))
}

export function regenerateScript(projectId: ProjectId) {
  return unwrap<ScriptResponse>(http.post(`/projects/${projectId}/script/regenerate`))
}

export function getScriptStatus(projectId: ProjectId) {
  return unwrap<GenerationStatus>(http.get(`/projects/${projectId}/script/status`))
}

export function getScript(projectId: ProjectId) {
  return unwrap<ScriptResponse>(http.get(`/projects/${projectId}/script`))
}

export function updateScript(projectId: ProjectId, payload: ScriptUpdatePayload) {
  return unwrap<ScriptResponse>(http.put(`/projects/${projectId}/script`, payload))
}

export function validateScript(projectId: ProjectId, payload: ScriptUpdatePayload) {
  return unwrap<ValidationResult>(http.post(`/projects/${projectId}/script/validate`, payload))
}

export function repairScript(projectId: ProjectId, payload: ScriptUpdatePayload) {
  return unwrap<RepairResponse>(http.post(`/projects/${projectId}/script/repair`, payload))
}

export const scriptApi = {
  generateScript,
  regenerateScript,
  getScriptStatus,
  getScript,
  updateScript,
  validateScript,
  repairScript
}
