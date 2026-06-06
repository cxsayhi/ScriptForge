import http, { unwrap } from './http'
import type { AdaptationSetting, ProjectId } from './types'

export function getAdaptationSetting(projectId: ProjectId) {
  return unwrap<AdaptationSetting>(http.get(`/projects/${projectId}/settings`))
}

export function saveAdaptationSetting(projectId: ProjectId, payload: AdaptationSetting) {
  return unwrap<AdaptationSetting>(http.put(`/projects/${projectId}/settings`, payload))
}

export const settingApi = {
  getAdaptationSetting,
  saveAdaptationSetting
}
