<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import NovelImportPanel from '@/components/NovelImportPanel.vue'
import SettingPanel from '@/components/SettingPanel.vue'
import ScriptEditorPanel from '@/components/ScriptEditorPanel.vue'
import { projectApi } from '@/api/project'
import type { AdaptationSetting, GenerationStatus, NovelContent, Project, ScriptResponse } from '@/api/types'

const route = useRoute()
const router = useRouter()
const projectId = Number(route.params.id)

const project = ref<Project | null>(null)
const novel = ref<NovelContent | null>(null)
const setting = ref<AdaptationSetting | null>(null)
const script = ref<ScriptResponse | null>(null)
const status = ref<GenerationStatus | null>(null)
const schema = ref('')
const schemaDesign = ref('')
const loading = ref(false)
const generating = ref(false)

const currentStep = computed(() => {
  if (script.value?.yaml) return 4
  if (novel.value?.chapterCount && novel.value.chapterCount >= 3) return 2
  return 1
})

onMounted(loadAll)

async function loadAll() {
  loading.value = true
  try {
    const [projectData, novelData, settingData, statusData, scriptData, schemaData, designData] = await Promise.all([
      projectApi.getProject(projectId),
      projectApi.getNovel(projectId),
      projectApi.getSettings(projectId),
      projectApi.getScriptStatus(projectId),
      projectApi.getScript(projectId),
      projectApi.getSchema(),
      projectApi.getSchemaDesign()
    ])
    project.value = projectData
    novel.value = novelData
    setting.value = settingData
    status.value = statusData
    script.value = scriptData
    schema.value = schemaData
    schemaDesign.value = designData
  } finally {
    loading.value = false
  }
}

async function refreshProject() {
  project.value = await projectApi.getProject(projectId)
}

async function handleNovelSaved(value: NovelContent) {
  novel.value = value
  await refreshProject()
}

async function handleSettingSaved(value: AdaptationSetting) {
  setting.value = value
}

async function generateScript() {
  generating.value = true
  try {
    script.value = await projectApi.generateScript(projectId)
    status.value = await projectApi.getScriptStatus(projectId)
    await refreshProject()
    ElMessage.success('剧本初稿已生成')
  } finally {
    generating.value = false
  }
}
</script>

<template>
  <main v-loading="loading" class="page-shell workspace-shell">
    <header class="topbar workspace-topbar">
      <div>
        <el-button link @click="router.push('/')">
          <el-icon><Back /></el-icon>
          项目列表
        </el-button>
        <p class="eyebrow">Project {{ projectId }}</p>
        <h1>{{ project?.title || '改编项目' }}</h1>
      </div>
      <div class="topbar-actions">
        <el-tag effect="plain">{{ status?.message || '未生成' }}</el-tag>
        <el-button type="primary" size="large" :loading="generating" @click="generateScript">
          <el-icon><MagicStick /></el-icon>
          生成剧本
        </el-button>
      </div>
    </header>

    <el-steps :active="currentStep" align-center class="workflow-steps">
      <el-step title="创建项目" />
      <el-step title="导入小说" />
      <el-step title="改编配置" />
      <el-step title="YAML 剧本" />
    </el-steps>

    <el-tabs class="workspace-tabs">
      <el-tab-pane label="导入与配置">
        <div class="workspace-grid">
          <NovelImportPanel
            v-if="novel"
            :project-id="projectId"
            :initial-text="novel.originalText"
            :chapters="novel.chapters"
            @saved="handleNovelSaved"
          />
          <SettingPanel
            v-if="setting"
            :project-id="projectId"
            :setting="setting"
            @saved="handleSettingSaved"
          />
        </div>
      </el-tab-pane>

      <el-tab-pane label="剧本编辑">
        <ScriptEditorPanel :project-id="projectId" :script="script" @refreshed="script = $event" />
      </el-tab-pane>

      <el-tab-pane label="Schema">
        <div class="schema-grid">
          <section class="workspace-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Schema</p>
                <h2>YAML Schema</h2>
              </div>
            </div>
            <pre class="schema-code">{{ schema }}</pre>
          </section>
          <section class="workspace-panel">
            <div class="panel-heading">
              <div>
                <p class="eyebrow">Design</p>
                <h2>设计说明</h2>
              </div>
            </div>
            <pre class="schema-code">{{ schemaDesign }}</pre>
          </section>
        </div>
      </el-tab-pane>
    </el-tabs>
  </main>
</template>
