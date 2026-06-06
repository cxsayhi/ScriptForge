<script setup lang="ts">
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import type { FormInstance, FormRules } from 'element-plus'
import RouteShell from '@/components/RouteShell.vue'
import { settingApi } from '@/api/setting'
import type { AdaptationSetting } from '@/api/types'
import { useProjectStore } from '@/stores/projectStore'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()
const projectStore = useProjectStore()
const formRef = ref<FormInstance>()
const loading = ref(false)
const saving = ref(false)
const errorText = ref('')
const lastSavedAt = ref('')

const defaultSetting: AdaptationSetting = {
  scriptType: 'web_drama',
  targetEpisodes: 3,
  episodeDurationMinutes: 8,
  style: '悬疑',
  language: 'zh-CN',
  adaptationIntensity: '适度改编',
  dialogueStyle: '影视化',
  budgetPreference: '可拍摄优先',
  keepOriginalDialogues: true
}

const form = reactive<AdaptationSetting>({ ...defaultSetting })
const project = computed(() => projectStore.currentProject)

const scriptTypeOptions = [
  { label: '网剧', value: 'web_drama' },
  { label: '短剧', value: 'short_drama' },
  { label: '电影', value: 'movie' },
  { label: '舞台剧', value: 'stage_play' }
]

const languageOptions = [
  { label: '中文', value: 'zh-CN' },
  { label: 'English', value: 'en-US' }
]

const styleOptions = ['悬疑', '甜宠', '都市', '古风', '喜剧', '现实主义']
const intensityOptions = ['忠于原著', '适度改编', '大幅改编']
const dialogueOptions = ['口语化', '文艺化', '影视化']
const budgetOptions = ['少角色', '少场景', '可拍摄优先']

const rules: FormRules<AdaptationSetting> = {
  scriptType: [{ required: true, message: '请选择剧本类型', trigger: 'change' }],
  targetEpisodes: [{ required: true, message: '请输入目标集数', trigger: 'change' }],
  episodeDurationMinutes: [{ required: true, message: '请输入单集分钟', trigger: 'change' }],
  style: [{ required: true, message: '请选择或输入风格', trigger: 'change' }],
  language: [{ required: true, message: '请选择语言', trigger: 'change' }],
  adaptationIntensity: [{ required: true, message: '请选择改编强度', trigger: 'change' }],
  dialogueStyle: [{ required: true, message: '请选择对白风格', trigger: 'change' }],
  budgetPreference: [{ required: true, message: '请选择预算倾向', trigger: 'change' }]
}

const stats = computed(() => [
  {
    label: '剧本类型',
    value: scriptTypeLabel(form.scriptType),
    tone: 'neutral' as const
  },
  {
    label: '目标集数',
    value: `${form.targetEpisodes} 集`,
    tone: 'neutral' as const
  },
  {
    label: '单集时长',
    value: `${form.episodeDurationMinutes} 分钟`,
    tone: 'neutral' as const
  },
  {
    label: '对白',
    value: form.dialogueStyle,
    tone: 'ready' as const
  }
])

watch(
  () => props.projectId,
  (projectId) => {
    loadSettings(projectId).catch(() => undefined)
  },
  { immediate: true }
)

function applySetting(setting?: AdaptationSetting) {
  Object.assign(form, defaultSetting, setting || {})
}

function scriptTypeLabel(value: string) {
  return scriptTypeOptions.find((option) => option.value === value)?.label || value
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

async function loadSettings(projectId: number) {
  loading.value = true
  errorText.value = ''
  try {
    const [projectData, setting] = await Promise.all([
      projectStore.fetchProject(projectId),
      settingApi.getAdaptationSetting(projectId)
    ])
    projectStore.upsertProject(projectData)
    applySetting(setting)
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '改编配置加载失败'
  } finally {
    loading.value = false
  }
}

async function saveSettings() {
  errorText.value = ''
  const valid = await formRef.value?.validate().catch(() => false)
  if (!valid) {
    ElMessage.warning('请先补全改编配置')
    return
  }

  saving.value = true
  try {
    const saved = await settingApi.saveAdaptationSetting(props.projectId, { ...form })
    applySetting(saved)
    lastSavedAt.value = new Date().toISOString()
    ElMessage.success('改编配置已保存')
  } catch (error) {
    errorText.value = error instanceof Error ? error.message : '改编配置保存失败'
  } finally {
    saving.value = false
  }
}

function resetToDefaults() {
  applySetting()
  ElMessage.info('已恢复默认配置，保存后生效')
}
</script>

<template>
  <RouteShell
    eyebrow="Adaptation"
    :title="project?.title ? `${project.title}：改编配置` : '改编配置'"
    summary="保存剧本类型、目标集数、语言风格和制作倾向。"
    active-key="settings"
    :project-id="props.projectId"
    :stats="stats"
  >
    <template #actions>
      <el-button @click="router.push(`/projects/${props.projectId}/novel`)">
        <el-icon><Back /></el-icon>
        小说导入
      </el-button>
      <el-button type="primary" :disabled="!project?.hasNovel" @click="router.push(`/projects/${props.projectId}/generate`)">
        <el-icon><Right /></el-icon>
        继续生成
      </el-button>
    </template>

    <section v-loading="loading" class="setting-layout">
      <div class="placeholder-panel setting-form-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Settings</p>
            <h2>基础参数</h2>
          </div>
          <div class="setting-action-row">
            <el-button @click="resetToDefaults">
              <el-icon><RefreshLeft /></el-icon>
              默认值
            </el-button>
            <el-button type="primary" :loading="saving" @click="saveSettings">
              <el-icon><Check /></el-icon>
              保存配置
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="errorText"
          class="setting-alert"
          type="error"
          :closable="false"
          show-icon
          title="配置处理失败"
          :description="errorText"
        />

        <el-alert
          v-else-if="!project?.hasNovel"
          class="setting-alert"
          type="warning"
          :closable="false"
          show-icon
          title="小说尚未导入"
          description="可以先保存默认配置，但进入生成流程前需要至少 3 个小说章节。"
        />

        <el-form
          ref="formRef"
          class="settings-form setting-form-grid"
          label-position="top"
          :model="form"
          :rules="rules"
          :disabled="saving"
        >
          <el-form-item label="剧本类型" prop="scriptType">
            <el-select v-model="form.scriptType">
              <el-option
                v-for="option in scriptTypeOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="目标集数" prop="targetEpisodes">
            <el-input-number v-model="form.targetEpisodes" :min="1" :max="80" controls-position="right" />
          </el-form-item>

          <el-form-item label="单集分钟" prop="episodeDurationMinutes">
            <el-input-number v-model="form.episodeDurationMinutes" :min="5" :max="180" controls-position="right" />
          </el-form-item>

          <el-form-item label="语言" prop="language">
            <el-select v-model="form.language">
              <el-option
                v-for="option in languageOptions"
                :key="option.value"
                :label="option.label"
                :value="option.value"
              />
            </el-select>
          </el-form-item>

          <el-form-item label="风格" prop="style">
            <el-select v-model="form.style" filterable allow-create>
              <el-option v-for="option in styleOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>

          <el-form-item label="改编强度" prop="adaptationIntensity">
            <el-segmented v-model="form.adaptationIntensity" :options="intensityOptions" />
          </el-form-item>

          <el-form-item label="对白风格" prop="dialogueStyle">
            <el-segmented v-model="form.dialogueStyle" :options="dialogueOptions" />
          </el-form-item>

          <el-form-item label="预算倾向" prop="budgetPreference">
            <el-select v-model="form.budgetPreference">
              <el-option v-for="option in budgetOptions" :key="option" :label="option" :value="option" />
            </el-select>
          </el-form-item>

          <el-form-item class="setting-switch-item" label="保留原文对白">
            <el-switch
              v-model="form.keepOriginalDialogues"
              active-text="保留"
              inactive-text="重写"
              inline-prompt
            />
          </el-form-item>
        </el-form>
      </div>

      <aside class="placeholder-panel setting-summary-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Brief</p>
            <h2>配置摘要</h2>
          </div>
        </div>

        <div class="setting-summary-list">
          <div>
            <span>剧本类型</span>
            <strong>{{ scriptTypeLabel(form.scriptType) }}</strong>
          </div>
          <div>
            <span>篇幅目标</span>
            <strong>{{ form.targetEpisodes }} 集 x {{ form.episodeDurationMinutes }} 分钟</strong>
          </div>
          <div>
            <span>风格方向</span>
            <strong>{{ form.style }} / {{ form.adaptationIntensity }}</strong>
          </div>
          <div>
            <span>对白处理</span>
            <strong>{{ form.dialogueStyle }}，{{ form.keepOriginalDialogues ? '保留原文对白' : '允许重写对白' }}</strong>
          </div>
          <div>
            <span>预算倾向</span>
            <strong>{{ form.budgetPreference }}</strong>
          </div>
          <div>
            <span>保存时间</span>
            <strong>{{ formatDate(lastSavedAt) }}</strong>
          </div>
        </div>

        <div class="setting-readiness">
          <div class="status-row" :class="{ ready: project?.hasNovel }">
            <span>小说内容</span>
            <strong>{{ project?.hasNovel ? `${project.chapterCount} 章` : '未导入' }}</strong>
          </div>
          <div class="status-row ready">
            <span>配置状态</span>
            <strong>可保存</strong>
          </div>
        </div>
      </aside>
    </section>
  </RouteShell>
</template>
