import http, { unwrap } from './http'
import type {
  GenerationStatus,
  FailedEpisodeUpdatePayload,
  ProjectId,
  RepairResponse,
  ScriptResponse,
  ScriptUpdatePayload,
  ValidationResult
} from './types'

const GENERATION_TIMEOUT_MS = 120000

export function generateScript(projectId: ProjectId) {
  return unwrap<ScriptResponse>(http.post(`/projects/${projectId}/script/generate`, undefined, {
    timeout: GENERATION_TIMEOUT_MS
  }))
}

export function regenerateScript(projectId: ProjectId) {
  return unwrap<ScriptResponse>(http.post(`/projects/${projectId}/script/regenerate`, undefined, {
    timeout: GENERATION_TIMEOUT_MS
  }))
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

export function updateFailedEpisode(projectId: ProjectId, episodeId: number, payload: FailedEpisodeUpdatePayload) {
  return unwrap<ScriptResponse>(http.put(`/projects/${projectId}/script/failed-episodes/${episodeId}`, payload))
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
  updateFailedEpisode,
  validateScript,
  repairScript
}
