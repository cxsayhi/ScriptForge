<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProjectFormDialog from '@/components/ProjectFormDialog.vue'
import RouteShell from '@/components/RouteShell.vue'
import { useProjectStore } from '@/stores/projectStore'
import type { ProjectPayload } from '@/api/types'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()
const store = useProjectStore()
const dialogVisible = ref(false)
const saving = ref(false)

const project = computed(() => store.currentProject)

const stats = computed(() => [
  {
    label: '小说',
    value: project.value?.hasNovel ? `${project.value.chapterCount} 章` : '未导入',
    tone: project.value?.hasNovel ? ('ready' as const) : ('warn' as const)
  },
  {
    label: '项目状态',
    value: project.value ? statusLabel(project.value.status) : '-',
    tone: project.value?.hasNovel ? ('ready' as const) : ('neutral' as const)
  },
  {
    label: '剧本',
    value: project.value?.hasScript ? '已生成' : '未生成',
    tone: project.value?.hasScript ? ('ready' as const) : ('neutral' as const)
  }
])

const workflowCards = computed(() => [
  {
    title: '小说导入',
    eyebrow: 'Novel',
    description: project.value?.hasNovel ? `已解析 ${project.value.chapterCount} 个章节。` : '上传文件或粘贴正文，解析章节后进入配置。',
    to: `/projects/${props.projectId}/novel`,
    icon: 'Document'
  },
  {
    title: '改编配置',
    eyebrow: 'Settings',
    description: '设置剧本类型、集数、语言风格和制作倾向。',
    to: `/projects/${props.projectId}/settings`,
    icon: 'SetUp'
  },
  {
    title: '剧本生成',
    eyebrow: 'Generate',
    description: project.value?.hasScript ? '已有 YAML 剧本，可重新生成或继续编辑。' : '完成前置检查后生成 YAML 剧本初稿。',
    to: `/projects/${props.projectId}/generate`,
    icon: 'MagicStick'
  },
  {
    title: 'YAML 编辑',
    eyebrow: 'Script',
    description: project.value?.hasScript ? '查看、编辑、校验并导出剧本。' : '生成剧本后可进入编辑器。',
    to: `/projects/${props.projectId}/script`,
    icon: 'EditPen',
    disabled: !project.value?.hasScript
  }
])

watch(
  () => props.projectId,
  (projectId) => {
    store.fetchProject(projectId).catch(() => undefined)
  },
  { immediate: true }
)

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    novel_ready: '已导入',
    generated: '已生成',
    script_ready: '已生成'
  }
  return labels[status] || status
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

async function updateProject(payload: ProjectPayload) {
  if (!project.value) return
  saving.value = true
  try {
    await store.updateProject(project.value.id, {
      ...payload,
      status: project.value.status
    })
    dialogVisible.value = false
    ElMessage.success('项目已更新')
  } finally {
    saving.value = false
  }
}

async function deleteCurrentProject() {
  if (!project.value) return
  try {
    await ElMessageBox.confirm(`确定删除「${project.value.title}」吗？删除后将返回项目列表。`, '删除项目', {
      type: 'warning',
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      confirmButtonClass: 'el-button--danger'
    })
    await store.deleteProject(project.value.id)
    ElMessage.success('项目已删除')
    router.push('/projects')
  } catch {
    // User canceled the destructive action.
  }
}
</script>

<template>
  <RouteShell
    eyebrow="Project Overview"
    :title="project?.title || '项目总览'"
    :summary="project?.description || '集中查看项目基本状态，并进入导入、配置、生成和编辑环节。'"
    active-key="detail"
    :project-id="props.projectId"
    :stats="stats"
  >
    <template #actions>
      <el-button :disabled="!project" @click="dialogVisible = true">
        <el-icon><EditPen /></el-icon>
        编辑
      </el-button>
      <el-button type="danger" plain :disabled="!project" @click="deleteCurrentProject">
        <el-icon><Delete /></el-icon>
        删除
      </el-button>
    </template>

    <section v-loading="store.loading" class="project-detail-layout">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Project</p>
            <h2>项目信息</h2>
          </div>
          <el-tag v-if="project" effect="plain">{{ statusLabel(project.status) }}</el-tag>
        </div>

        <dl class="detail-list">
          <div>
            <dt>项目编号</dt>
            <dd>#{{ props.projectId }}</dd>
          </div>
          <div>
            <dt>项目标题</dt>
            <dd>{{ project?.title || '-' }}</dd>
          </div>
          <div>
            <dt>项目备注</dt>
            <dd>{{ project?.description || '暂无备注' }}</dd>
          </div>
          <div>
            <dt>创建时间</dt>
            <dd>{{ formatDate(project?.createdAt) }}</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>{{ formatDate(project?.updatedAt) }}</dd>
          </div>
        </dl>
      </div>

      <aside class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Status</p>
            <h2>当前进度</h2>
          </div>
        </div>
        <div class="project-status-stack">
          <div class="status-row" :class="{ ready: project?.hasNovel }">
            <span>小说内容</span>
            <strong>{{ project?.hasNovel ? `${project.chapterCount} 章` : '未导入' }}</strong>
          </div>
          <div class="status-row" :class="{ ready: project?.hasScript }">
            <span>YAML 剧本</span>
            <strong>{{ project?.hasScript ? '已生成' : '未生成' }}</strong>
          </div>
        </div>
      </aside>
    </section>

    <section class="project-workflow-grid" aria-label="项目功能入口">
      <RouterLink
        v-for="card in workflowCards"
        :key="card.title"
        v-slot="{ href, navigate }"
        :to="card.to"
        custom
      >
        <a
          :href="href"
          class="workflow-card"
          :class="{ disabled: card.disabled }"
          @click="card.disabled ? $event.preventDefault() : navigate($event)"
        >
          <div class="workflow-card-top">
            <p class="eyebrow">{{ card.eyebrow }}</p>
            <el-icon>
              <component :is="card.icon" />
            </el-icon>
          </div>
          <h2>{{ card.title }}</h2>
          <p>{{ card.description }}</p>
        </a>
      </RouterLink>
    </section>

    <ProjectFormDialog
      v-model="dialogVisible"
      :loading="saving"
      :project="project"
      @submit="updateProject"
    />
  </RouteShell>
</template>
