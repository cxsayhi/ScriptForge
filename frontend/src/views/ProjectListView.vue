<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProjectFormDialog from '@/components/ProjectFormDialog.vue'
import RouteShell from '@/components/RouteShell.vue'
import { useProjectStore } from '@/stores/projectStore'
import type { Project, ProjectPayload } from '@/api/types'

const router = useRouter()
const store = useProjectStore()
const dialogVisible = ref(false)
const saving = ref(false)
const editingProject = ref<Project | null>(null)

const stats = computed(() => [
  { label: '项目', value: String(store.totalProjects), tone: 'neutral' as const },
  {
    label: '已导入小说',
    value: String(store.importedProjects),
    tone: store.importedProjects ? ('ready' as const) : ('warn' as const)
  },
  {
    label: '已生成剧本',
    value: String(store.readyProjects),
    tone: store.readyProjects ? ('ready' as const) : ('neutral' as const)
  }
])

onMounted(() => {
  store.fetchProjects().catch(() => undefined)
})

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    novel_ready: '已导入',
    generated: '已生成',
    script_ready: '已生成'
  }
  return labels[status] || status
}

function statusType(project: Project) {
  if (project.hasScript) return 'success'
  if (project.hasNovel) return 'warning'
  return 'info'
}

function formatDate(value: string) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function openCreateDialog() {
  editingProject.value = null
  dialogVisible.value = true
}

function openEditDialog(project: Project) {
  editingProject.value = project
  dialogVisible.value = true
}

async function submitProject(payload: ProjectPayload) {
  saving.value = true
  try {
    if (editingProject.value) {
      await store.updateProject(editingProject.value.id, {
        ...payload,
        status: editingProject.value.status
      })
      ElMessage.success('项目已更新')
    } else {
      const project = await store.createProject(payload)
      ElMessage.success('项目已创建')
      router.push(`/projects/${project.id}`)
    }
    dialogVisible.value = false
  } finally {
    saving.value = false
  }
}

async function deleteProject(project: Project) {
  try {
    await ElMessageBox.confirm(`确定删除「${project.title}」吗？删除后项目数据将无法在前端恢复。`, '删除项目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    await store.deleteProject(project.id)
    ElMessage.success('项目已删除')
  } catch {
    // User canceled the destructive action.
  }
}
</script>

<template>
  <RouteShell
    eyebrow="ScriptForge"
    title="小说转剧本工作台"
    summary="把小说原文、改编参数和 YAML 剧本组织进同一个清晰流程。"
    active-key="projects"
    :stats="stats"
  >
    <template #actions>
      <el-button type="primary" @click="openCreateDialog">
        <el-icon><Plus /></el-icon>
        新建项目
      </el-button>
    </template>

    <section class="project-index-layout">
      <div v-loading="store.loading" class="placeholder-panel project-index-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Projects</p>
            <h2>项目档案</h2>
          </div>
          <el-tag effect="plain">{{ store.totalProjects }} 个项目</el-tag>
        </div>

        <div v-if="store.projects.length" class="project-board">
          <article
            v-for="project in store.projects"
            :key="project.id"
            class="project-card"
            @click="router.push(`/projects/${project.id}`)"
          >
            <div class="project-card-top">
              <el-tag :type="statusType(project)" effect="plain">{{ statusLabel(project.status) }}</el-tag>
              <div class="card-action-row">
                <el-button circle text aria-label="编辑项目" @click.stop="openEditDialog(project)">
                  <el-icon><EditPen /></el-icon>
                </el-button>
                <el-button circle text aria-label="删除项目" @click.stop="deleteProject(project)">
                  <el-icon><Delete /></el-icon>
                </el-button>
              </div>
            </div>

            <h2>{{ project.title }}</h2>
            <p>{{ project.description || '暂无备注，后续可补充改编目标、受众和题材方向。' }}</p>

            <div class="project-progress-row">
              <span :class="{ ready: project.hasNovel }">小说</span>
              <span :class="{ ready: project.hasScript }">剧本</span>
            </div>

            <div class="project-meta">
              <span>{{ project.chapterCount }} 章</span>
              <span>更新 {{ formatDate(project.updatedAt) }}</span>
            </div>
          </article>
        </div>

        <el-empty v-else-if="!store.loading" class="project-empty" description="暂无项目">
          <el-button type="primary" @click="openCreateDialog">
            <el-icon><Plus /></el-icon>
            创建第一个项目
          </el-button>
        </el-empty>
      </div>

      <aside class="placeholder-panel project-flow-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Flow</p>
            <h2>改编流程</h2>
          </div>
        </div>
        <ol class="route-timeline">
          <li>创建项目</li>
          <li>导入小说</li>
          <li>填写配置</li>
          <li>生成剧本</li>
          <li>校验导出</li>
        </ol>
      </aside>
    </section>

    <ProjectFormDialog
      v-model="dialogVisible"
      :loading="saving"
      :project="editingProject"
      @submit="submitProject"
    />
  </RouteShell>
</template>
