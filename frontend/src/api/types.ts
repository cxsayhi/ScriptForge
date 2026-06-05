export interface ApiResponse<T> {
  code: number
  message: string
  data: T
}

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

export interface ScriptResponse {
  projectId: number
  yaml: string
  validationResult: ValidationResult
  updatedAt: string
}

export interface RepairResponse {
  yaml: string
  validationResult: ValidationResult
}
