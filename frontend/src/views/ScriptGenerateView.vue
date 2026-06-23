<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import RouteShell from '@/components/RouteShell.vue'
import { scriptApi } from '@/api/script'
import { settingApi } from '@/api/setting'
import type { AdaptationSetting, GenerationStatus, ScriptResponse } from '@/api/types'
import { useProjectStore } from '@/stores/projectStore'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()
const projectStore = useProjectStore()
const setting = ref<AdaptationSetting | null>(null)
const status = ref<GenerationStatus | null>(null)
const script = ref<ScriptResponse | null>(null)
const loading = ref(false)
const generating = ref(false)
const errorText = ref('')

const project = computed(() => projectStore.currentProject)
const hasNovel = computed(() => Boolean(project.value?.hasNovel))
const hasEnoughChapters = computed(() => (project.value?.chapterCount || 0) >= 3)
const hasSettings = computed(() => Boolean(setting.value))
const needsReview = computed(() => script.value?.generationStatus === 'needs_review' || status.value?.status === 'needs_review')
const hasScript = computed(() => Boolean(project.value?.hasScript || script.value?.yaml || script.value?.rawLlmResponse))
const canGenerate = computed(() => Boolean(project.value && hasNovel.value && hasEnoughChapters.value && hasSettings.value))
const statusText = computed(() => script.value?.generationMessage || status.value?.message || (hasScript.value ? '剧本初稿已生成' : '尚未生成剧本'))
const isRunning = computed(() => generating.value || status.value?.status === 'running')

const checks = computed(() => [
  {
    label: '项目存在',
    detail: project.value ? `#${project.value.id} ${project.value.title}` : '正在加载项目',
    ready: Boolean(project.value),
    to: `/projects/${props.projectId}`
  },
  {
    label: '小说已提交',
    detail: hasNovel.value ? `已导入 ${project.value?.chapterCount || 0} 章` : '请先导入小说文本',
    ready: hasNovel.value,
    to: `/projects/${props.projectId}/novel`
  },
  {
    label: '章节不少于 3 个',
    detail: `${project.value?.chapterCount || 0} / 3 章`,
    ready: hasEnoughChapters.value,
    to: `/projects/${props.projectId}/novel`
  },
  {
    label: '改编配置已保存',
    detail: setting.value
      ? `${scriptTypeLabel(setting.value.scriptType)} · ${setting.value.targetEpisodes} 集 · ${setting.value.style}`
      : '正在读取配置',
    ready: hasSettings.value,
    to: `/projects/${props.projectId}/settings`
  },
  {
    label: '当前剧本状态',
    detail: hasScript.value ? '已有 YAML，可重新生成' : '还没有 YAML 剧本',
    ready: true,
    to: `/projects/${props.projectId}/script`
  }
])

const stats = computed(() => [
  {
    label: '生成状态',
    value: status.value?.status || 'idle',
    tone: needsReview.value || status.value?.status === 'failed' ? ('warn' as const) : hasScript.value ? ('ready' as const) : ('neutral' as const)
  },
  {
    label: '章节',
    value: `${project.value?.chapterCount || 0}`,
    tone: hasEnoughChapters.value ? ('ready' as const) : ('warn' as const)
  },
  {
    label: '配置',
    value: hasSettings.value ? '已加载' : '未加载',
    tone: hasSettings.value ? ('ready' as const) : ('neutral' as const)
  },
  {
    label: '剧本',
    value: hasScript.value ? '已有' : '无',
    tone: hasScript.value ? ('ready' as const) : ('neutral' as const)
  }
])

watch(
  () => props.projectId,
  (projectId) => {
    loadGenerationContext(projectId).catch(() => undefined)
  },
  { immediate: true }
)

function scriptTypeLabel(value: string) {
  const labels: Record<string, string> = {
    web_drama: '网剧',
    short_drama: '短剧',
    movie: '电影',
    stage_play: '舞台剧'
  }
  return labels[value] || value
}

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

async function loadGenerationContext(projectId: number) {
  loading.value = true
  errorText.value = ''
  try {
    const [projectData, settingData, statusData, scriptData] = await Promise.all([
      projectStore.fetchProject(projectId),
      settingApi.getAdaptationSetting(projectId),
      scriptApi.getScriptStatus(projectId),
      scriptApi.getScript(projectId)
    ])
    projectStore.upsertProject(projectData)
    setting.value = settingData
    status.value = statusData
    script.value = scriptData
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '生成上下文加载失败'
  } finally {
    loading.value = false
  }
}

async function refreshAfterGeneration(result: ScriptResponse) {
  script.value = result
  status.value = await scriptApi.getScriptStatus(props.projectId)
  await projectStore.fetchProject(props.projectId)
}

async function generateScript() {
  if (!canGenerate.value) {
    ElMessage.warning('请先完成生成前检查项')
    return
  }

  generating.value = true
  errorText.value = ''
  try {
    const result = await scriptApi.generateScript(props.projectId)
    await refreshAfterGeneration(result)
    if (result.generationStatus === 'needs_review') {
      ElMessage.warning('LLM 返回内容已保留，但需要人工审核或修复')
    } else {
      ElMessage.success('剧本初稿已生成')
    }
    router.push(`/projects/${props.projectId}/script`)
  } catch (error) {
    status.value = await scriptApi.getScriptStatus(props.projectId).catch(() => status.value)
    errorText.value = error instanceof Error ? error.message : '剧本生成失败'
  } finally {
    generating.value = false
  }
}

async function regenerateScript() {
  if (!canGenerate.value) {
    ElMessage.warning('请先完成生成前检查项')
    return
  }
  if (hasScript.value) {
    try {
      await ElMessageBox.confirm('重新生成会覆盖当前已保存的 YAML 剧本，确定继续吗？', '重新生成剧本', {
        type: 'warning',
        confirmButtonText: '重新生成',
        cancelButtonText: '取消'
      })
    } catch {
      return
    }
  }

  generating.value = true
  errorText.value = ''
  try {
    const result = await scriptApi.regenerateScript(props.projectId)
    await refreshAfterGeneration(result)
    if (result.generationStatus === 'needs_review') {
      ElMessage.warning('LLM 返回内容已保留，但需要人工审核或修复')
    } else {
      ElMessage.success('剧本已重新生成')
    }
    router.push(`/projects/${props.projectId}/script`)
  } catch (error) {
    status.value = await scriptApi.getScriptStatus(props.projectId).catch(() => status.value)
    errorText.value = error instanceof Error ? error.message : '剧本重新生成失败'
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <RouteShell
    eyebrow="Generation"
    :title="project?.title ? `${project.title}：剧本生成` : '剧本生成'"
    summary="检查小说、配置和当前剧本状态，然后触发 YAML 剧本初稿生成。"
    active-key="generate"
    :project-id="props.projectId"
    :stats="stats"
  >
    <template #actions>
      <el-button @click="router.push(`/projects/${props.projectId}/settings`)">
        <el-icon><Back /></el-icon>
        改编配置
      </el-button>
      <el-button type="primary" :loading="generating" :disabled="!canGenerate || isRunning" @click="generateScript">
        <el-icon><MagicStick /></el-icon>
        生成剧本
      </el-button>
    </template>

    <section v-loading="loading" class="generate-layout">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Checklist</p>
            <h2>生成前检查</h2>
          </div>
          <el-tag :type="canGenerate ? 'success' : 'warning'" effect="plain">
            {{ canGenerate ? 'ready' : 'blocked' }}
          </el-tag>
        </div>

        <el-alert
          v-if="errorText"
          class="generate-alert"
          type="error"
          :closable="false"
          show-icon
          title="生成失败"
          :description="errorText"
        />

        <el-alert
          v-else-if="needsReview"
          class="generate-alert"
          type="warning"
          :closable="false"
          show-icon
          title="生成结果待审核"
          :description="statusText"
        />

        <ul class="generate-check-list">
          <li v-for="item in checks" :key="item.label" :class="{ ready: item.ready }">
            <span class="check-dot" />
            <div>
              <strong>{{ item.label }}</strong>
              <p>{{ item.detail }}</p>
            </div>
            <RouterLink v-if="!item.ready" :to="item.to">处理</RouterLink>
          </li>
        </ul>
      </div>

      <aside class="placeholder-panel generation-panel">
        <div>
          <p class="eyebrow">Agent</p>
          <h2>生成控制</h2>
        </div>

        <div class="generation-status-card" :class="{ ready: hasScript }">
          <span>当前状态</span>
          <strong>{{ statusText }}</strong>
          <small>更新：{{ formatDate(status?.updatedAt || script?.updatedAt) }}</small>
        </div>

        <div class="generation-actions">
          <el-button
            type="primary"
            :loading="generating"
            :disabled="!canGenerate || isRunning"
            @click="generateScript"
          >
            <el-icon><MagicStick /></el-icon>
            生成剧本
          </el-button>
          <el-button
            :loading="generating"
            :disabled="!canGenerate || isRunning"
            @click="regenerateScript"
          >
            <el-icon><Refresh /></el-icon>
            重新生成
          </el-button>
          <el-button :disabled="!hasScript" @click="router.push(`/projects/${props.projectId}/script`)">
            <el-icon><EditPen /></el-icon>
            YAML 编辑
          </el-button>
        </div>
      </aside>
    </section>
  </RouteShell>
</template>
