<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RouteShell from '@/components/RouteShell.vue'
import { scriptApi } from '@/api/script'
import type { ExportFormat, FailedEpisode, ScriptResponse, ValidationResult } from '@/api/types'
import { downloadProjectExport } from '@/utils/download'
import { useProjectStore } from '@/stores/projectStore'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()
const projectStore = useProjectStore()
const script = ref<ScriptResponse | null>(null)
const yaml = ref('')
const savedYaml = ref('')
const validation = ref<ValidationResult | null>(null)
const loading = ref(false)
const actionLoading = ref<'save' | 'validate' | 'repair' | 'yaml' | 'markdown' | ''>('')
const errorText = ref('')

const project = computed(() => projectStore.currentProject)
const hasYaml = computed(() => yaml.value.trim().length > 0)
const hasSavedScript = computed(() => savedYaml.value.trim().length > 0)
const hasUnsavedChanges = computed(() => yaml.value !== savedYaml.value)
const needsReview = computed(() => script.value?.generationStatus === 'needs_review')
const hasRawLlmResponse = computed(() => Boolean(script.value?.rawLlmResponse?.trim()))
const failedEpisodes = computed<FailedEpisode[]>(() => script.value?.failedEpisodes || [])
const failedEpisodeDrafts = ref<Record<number, string>>({})
const failedEpisodeSavingId = ref<number | null>(null)
const validationState = computed(() => {
  if (!validation.value) return '未校验'
  return validation.value.valid ? '通过' : '失败'
})
const lineCount = computed(() => (yaml.value ? yaml.value.split('\n').length : 0))
const issueCount = computed(() => (validation.value?.errors.length || 0) + (validation.value?.warnings.length || 0))

const stats = computed(() => [
  {
    label: '保存状态',
    value: hasUnsavedChanges.value ? '未保存' : hasSavedScript.value ? '已保存' : '无剧本',
    tone: hasUnsavedChanges.value ? ('warn' as const) : hasSavedScript.value ? ('ready' as const) : ('neutral' as const)
  },
  {
    label: '校验',
    value: validationState.value,
    tone: validation.value?.valid ? ('ready' as const) : validation.value ? ('warn' as const) : ('neutral' as const)
  },
  {
    label: '行数',
    value: `${lineCount.value}`,
    tone: 'neutral' as const
  },
  {
    label: '问题',
    value: `${issueCount.value}`,
    tone: issueCount.value ? ('warn' as const) : ('neutral' as const)
  }
])

watch(
  () => props.projectId,
  (projectId) => {
    loadScript(projectId).catch(() => undefined)
  },
  { immediate: true }
)

watch(
  script,
  (currentScript) => {
    failedEpisodeDrafts.value = Object.fromEntries(
      (currentScript?.failedEpisodes || []).map((episode) => [episode.episodeId, episode.rawResponse || ''])
    )
  }
)

function formatDate(value?: string) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

async function loadScript(projectId: number) {
  loading.value = true
  errorText.value = ''
  try {
    const [projectData, scriptData] = await Promise.all([
      projectStore.fetchProject(projectId),
      scriptApi.getScript(projectId)
    ])
    projectStore.upsertProject(projectData)
    script.value = scriptData
    yaml.value = scriptData.yaml || ''
    savedYaml.value = scriptData.yaml || ''
    validation.value = scriptData.validationResult || null
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '剧本加载失败'
  } finally {
    loading.value = false
  }
}

async function validateCurrentYaml(showSuccess = true) {
  if (!hasYaml.value) {
    ElMessage.warning('请先填写 YAML 内容')
    return null
  }

  actionLoading.value = 'validate'
  errorText.value = ''
  try {
    const result = await scriptApi.validateScript(props.projectId, { yaml: yaml.value })
    validation.value = result
    if (showSuccess) {
      ElMessage[result.valid ? 'success' : 'warning'](result.valid ? 'YAML 校验通过' : 'YAML 校验未通过')
    }
    return result
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : 'YAML 校验失败'
    return null
  } finally {
    actionLoading.value = ''
  }
}

async function saveScript() {
  if (!hasYaml.value) {
    ElMessage.warning('请先填写 YAML 内容')
    return
  }

  actionLoading.value = 'save'
  errorText.value = ''
  try {
    const saved = await scriptApi.updateScript(props.projectId, { yaml: yaml.value })
    script.value = saved
    yaml.value = saved.yaml || ''
    savedYaml.value = saved.yaml || ''
    validation.value = saved.validationResult || null
    await projectStore.fetchProject(props.projectId)
    ElMessage.success('YAML 剧本已保存')
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : 'YAML 剧本保存失败'
  } finally {
    actionLoading.value = ''
  }
}

async function repairScript() {
  if (!hasYaml.value) {
    ElMessage.warning('请先填写 YAML 内容')
    return
  }

  actionLoading.value = 'repair'
  errorText.value = ''
  try {
    const repaired = await scriptApi.repairScript(props.projectId, { yaml: yaml.value })
    yaml.value = repaired.yaml || ''
    validation.value = repaired.validationResult
    if (repaired.validationResult.valid) {
      savedYaml.value = repaired.yaml || ''
      script.value = {
        projectId: props.projectId,
        yaml: repaired.yaml || '',
        validationResult: repaired.validationResult,
        updatedAt: new Date().toISOString(),
        generationStatus: repaired.generationStatus,
        generationMessage: repaired.generationMessage,
        rawLlmResponse: repaired.rawLlmResponse,
        failedEpisodes: repaired.failedEpisodes
      }
      await projectStore.fetchProject(props.projectId)
    }
    const remainsUnderReview = repaired.generationStatus === 'needs_review'
    ElMessage[repaired.validationResult.valid && !remainsUnderReview ? 'success' : 'warning'](
      repaired.validationResult.valid
        ? remainsUnderReview ? 'YAML 格式已修复，但失败剧集仍需处理' : 'YAML 已修复并保存'
        : '已尝试修复，仍需手动调整'
    )
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : 'YAML 修复失败'
  } finally {
    actionLoading.value = ''
  }
}

async function exportScript(format: ExportFormat) {
  if (!hasSavedScript.value) {
    ElMessage.warning('当前项目还没有已保存的 YAML 剧本')
    return
  }
  if (hasUnsavedChanges.value) {
    ElMessage.warning('当前编辑内容尚未保存，请先保存后再导出')
    return
  }

  actionLoading.value = format
  errorText.value = ''
  try {
    const result = await scriptApi.validateScript(props.projectId, { yaml: savedYaml.value })
    validation.value = result
    if (!result.valid) {
      ElMessage.warning('YAML 校验未通过，暂不导出')
      return
    }
    await downloadProjectExport(props.projectId, format)
    ElMessage.success(format === 'yaml' ? 'YAML 文件已导出' : 'Markdown 文件已导出')
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '文件导出失败'
  } finally {
    actionLoading.value = ''
  }
}

function restoreSavedYaml() {
  yaml.value = savedYaml.value
  validation.value = script.value?.validationResult || null
  ElMessage.info('已恢复到最近保存版本')
}

async function copyText(text: string, successMessage: string) {
  if (!text) return
  try {
    await navigator.clipboard.writeText(text)
    ElMessage.success(successMessage)
  } catch {
    ElMessage.warning('复制失败，请手动选择并复制文本')
  }
}

async function copyRawLlmResponse() {
  await copyText(script.value?.rawLlmResponse || '', '原始 LLM 回复已复制')
}

async function saveFailedEpisode(failedEpisode: FailedEpisode) {
  const rawResponse = failedEpisodeDrafts.value[failedEpisode.episodeId]?.trim() || ''
  if (!rawResponse) {
    ElMessage.warning(`第 ${failedEpisode.episodeId} 集内容不能为空`)
    return
  }

  failedEpisodeSavingId.value = failedEpisode.episodeId
  errorText.value = ''
  try {
    script.value = await scriptApi.updateFailedEpisode(props.projectId, failedEpisode.episodeId, { rawResponse })
    await projectStore.fetchProject(props.projectId)
    ElMessage.success(`第 ${failedEpisode.episodeId} 集待审核内容已保存`)
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : `第 ${failedEpisode.episodeId} 集保存失败`
  } finally {
    failedEpisodeSavingId.value = null
  }
}
</script>

<template>
  <RouteShell
    eyebrow="Script YAML"
    :title="project?.title ? `${project.title}：YAML 剧本编辑` : 'YAML 剧本编辑'"
    summary="查看、编辑、保存、校验、修复并导出完整 YAML 剧本。"
    active-key="script"
    :project-id="props.projectId"
    :stats="stats"
  >
    <template #actions>
      <el-button @click="router.push(`/projects/${props.projectId}/generate`)">
        <el-icon><Back /></el-icon>
        生成页面
      </el-button>
      <el-button type="primary" :loading="actionLoading === 'save'" :disabled="!hasYaml" @click="saveScript">
        <el-icon><EditPen /></el-icon>
        保存
      </el-button>
    </template>

    <section v-loading="loading" class="script-editor-layout">
      <div class="placeholder-panel script-editor-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Editor</p>
            <h2>剧本文本</h2>
          </div>
          <el-tag :type="validation?.valid ? 'success' : validation ? 'danger' : 'info'" effect="plain">
            {{ validationState }}
          </el-tag>
        </div>

        <el-alert
          v-if="errorText"
          class="script-alert"
          type="error"
          :closable="false"
          show-icon
          title="操作失败"
          :description="errorText"
        />

        <el-alert
          v-else-if="!hasSavedScript"
          class="script-alert"
          type="warning"
          :closable="false"
          show-icon
          title="尚未生成剧本"
          description="可以先返回生成页面生成初稿，也可以在这里粘贴合法 YAML 后保存。"
        />

        <el-alert
          v-if="needsReview"
          class="script-alert"
          type="warning"
          :closable="false"
          show-icon
          title="LLM 结果待审核"
          :description="script?.generationMessage || '原始模型回复已保留。可复制后手动整理为 YAML，或返回生成页再次尝试。'"
        />

        <section v-if="failedEpisodes.length" class="failed-episode-section">
          <div class="panel-heading">
            <div>
              <p class="eyebrow">Failed Episodes</p>
              <h3>待审核剧集</h3>
            </div>
            <el-tag type="warning" effect="plain">{{ failedEpisodes.length }} 集待处理</el-tag>
          </div>

          <el-collapse class="failed-episode-list">
            <el-collapse-item
              v-for="failedEpisode in failedEpisodes"
              :key="failedEpisode.episodeId"
              :name="`failed-${failedEpisode.episodeId}`"
            >
              <template #title>
                <span>第 {{ failedEpisode.episodeId }} 集</span>
                <el-tag class="failed-episode-status" type="warning" size="small" effect="plain">
                  {{ failedEpisode.status === 'edited' ? '已编辑，待审核' : '生成失败' }}
                </el-tag>
              </template>
              <p class="script-helper-text">{{ failedEpisode.reason }}</p>
              <el-input
                v-model="failedEpisodeDrafts[failedEpisode.episodeId]"
                type="textarea"
                resize="vertical"
                :rows="10"
                placeholder="在这里查看或修改该集保留的 LLM 原始内容"
              />
              <div class="panel-actions split-actions">
                <span class="script-helper-text">修改会单独保存，不会覆盖正式 YAML。</span>
                <div>
                  <el-button text @click="copyText(failedEpisodeDrafts[failedEpisode.episodeId] || '', `第 ${failedEpisode.episodeId} 集内容已复制`)">
                    复制内容
                  </el-button>
                  <el-button
                    type="primary"
                    :loading="failedEpisodeSavingId === failedEpisode.episodeId"
                    @click="saveFailedEpisode(failedEpisode)"
                  >
                    保存修改
                  </el-button>
                </div>
              </div>
            </el-collapse-item>
          </el-collapse>
        </section>

        <el-collapse v-if="hasRawLlmResponse" class="script-raw-response">
          <el-collapse-item name="raw-llm-response">
            <template #title>
              <span>查看保留的原始 LLM 回复</span>
            </template>
            <div class="panel-actions">
              <el-button text @click="copyRawLlmResponse">复制原始回复</el-button>
            </div>
            <el-input
              :model-value="script?.rawLlmResponse || ''"
              type="textarea"
              resize="vertical"
              :rows="10"
              readonly
            />
          </el-collapse-item>
        </el-collapse>

        <el-input
          v-model="yaml"
          class="yaml-editor script-yaml-editor"
          type="textarea"
          resize="none"
          :rows="28"
          spellcheck="false"
          placeholder="project:\n  title: 示例剧本\ncharacters: []\nepisodes: []"
        />

        <div class="panel-actions split-actions">
          <span class="script-helper-text">
            {{ hasUnsavedChanges ? '当前内容有未保存修改。' : `最近保存：${formatDate(script?.updatedAt)}` }}
          </span>
          <el-button text :disabled="!hasSavedScript || !hasUnsavedChanges" @click="restoreSavedYaml">恢复已保存版本</el-button>
        </div>
      </div>

      <aside class="placeholder-panel script-side-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Quality</p>
            <h2>校验与导出</h2>
          </div>
        </div>

        <div class="script-action-grid">
          <el-button :loading="actionLoading === 'validate'" :disabled="!hasYaml" @click="validateCurrentYaml()">
            <el-icon><CircleCheck /></el-icon>
            校验
          </el-button>
          <el-button :loading="actionLoading === 'repair'" :disabled="!hasYaml" @click="repairScript">
            <el-icon><Refresh /></el-icon>
            修复
          </el-button>
          <el-button type="primary" :loading="actionLoading === 'save'" :disabled="!hasYaml" @click="saveScript">
            <el-icon><EditPen /></el-icon>
            保存
          </el-button>
        </div>

        <div class="script-export-grid">
          <el-button :loading="actionLoading === 'yaml'" :disabled="!hasSavedScript" @click="exportScript('yaml')">
            <el-icon><Download /></el-icon>
            导出 YAML
          </el-button>
          <el-button :loading="actionLoading === 'markdown'" :disabled="!hasSavedScript" @click="exportScript('markdown')">
            <el-icon><Download /></el-icon>
            导出 Markdown
          </el-button>
        </div>

        <div class="script-summary-list">
          <div>
            <span>字符数</span>
            <strong>{{ yaml.length }}</strong>
          </div>
          <div>
            <span>行数</span>
            <strong>{{ lineCount }}</strong>
          </div>
          <div>
            <span>保存状态</span>
            <strong>{{ hasUnsavedChanges ? '未保存' : hasSavedScript ? '已保存' : '无剧本' }}</strong>
          </div>
          <div>
            <span>更新时间</span>
            <strong>{{ formatDate(script?.updatedAt) }}</strong>
          </div>
        </div>

        <section class="validation-box script-validation-box">
          <header class="validation-header">
            <span>校验结果</span>
            <el-tag v-if="validation" :type="validation.valid ? 'success' : 'danger'" effect="plain">
              {{ validation.valid ? 'Valid' : 'Invalid' }}
            </el-tag>
          </header>

          <div v-if="validation && (validation.errors.length || validation.warnings.length)">
            <div v-for="error in validation.errors" :key="error" class="validation-line error">
              {{ error }}
            </div>
            <div v-for="warning in validation.warnings" :key="warning" class="validation-line warning">
              {{ warning }}
            </div>
          </div>
          <p v-else class="validation-empty">
            {{ validation?.valid ? 'YAML 结构校验通过。' : '尚未获得校验结果。' }}
          </p>
        </section>
      </aside>
    </section>
  </RouteShell>
</template>
