export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

export type ProjectId = number
export type ExportFormat = 'yaml' | 'markdown'

export interface Project {
  id: number
  title: string
  description: string
  status: string
  createdAt: string
  updatedAt: string
  hasNovel: boolean
  chapterCount: number
  hasScript: boolean
}

export interface ProjectPayload {
  title: string
  description?: string
}

export interface ProjectUpdatePayload extends ProjectPayload {
  status?: string
}

export interface Chapter {
  index: number
  title: string
  contentLength: number
  preview: string
}

export interface NovelContent {
  projectId: number
  chapterCount: number
  chapters: Chapter[]
  originalText: string
  updatedAt: string
}

export interface NovelTextPayload {
  text: string
}

export interface AdaptationSetting {
  scriptType: string
  targetEpisodes: number
  episodeDurationMinutes: number
  style: string
  language: string
  adaptationIntensity: string
  dialogueStyle: string
  budgetPreference: string
  keepOriginalDialogues: boolean
}

export interface GenerationStatus {
  status: string
  message: string
  updatedAt: string
}

export interface ValidationResult {
  valid: boolean
  errors: string[]
  warnings: string[]
}

export interface FailedEpisode {
  episodeId: number
  status: string
  reason: string
  rawResponse: string
}

export interface ScriptResponse {
  projectId: number
  yaml: string
  validationResult: ValidationResult
  updatedAt: string
  generationStatus: string
  generationMessage: string
  rawLlmResponse: string
  failedEpisodes: FailedEpisode[]
}

export interface ScriptUpdatePayload {
  yaml: string
}

export interface FailedEpisodeUpdatePayload {
  rawResponse: string
}

export interface RepairResponse {
  yaml: string
  validationResult: ValidationResult
  generationStatus: string
  generationMessage: string
  rawLlmResponse: string
  failedEpisodes: FailedEpisode[]
}

export interface ExportFile {
  blob: Blob
  filename: string
  format: ExportFormat
}
