<script setup lang="ts">
import { computed, onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import ProjectFormDialog from '@/components/ProjectFormDialog.vue'
import { useProjectStore } from '@/stores/projectStore'

const router = useRouter()
const store = useProjectStore()
const dialogVisible = ref(false)
const creating = ref(false)

const importedCount = computed(() => store.projects.filter((project) => project.hasNovel).length)

onMounted(() => {
  store.fetchProjects()
})

async function createProject(payload: { title: string; description: string }) {
  creating.value = true
  try {
    const project = await store.createProject(payload)
    dialogVisible.value = false
    ElMessage.success('项目已创建')
    router.push(`/projects/${project.id}`)
  } finally {
    creating.value = false
  }
}

async function deleteProject(projectId: number) {
  await ElMessageBox.confirm('删除后当前内存中的项目数据会消失。', '删除项目', {
    type: 'warning',
    confirmButtonText: '删除',
    cancelButtonText: '取消'
  })
  await store.deleteProject(projectId)
  ElMessage.success('项目已删除')
}

function statusLabel(status: string) {
  const labels: Record<string, string> = {
    draft: '草稿',
    novel_ready: '已导入',
    script_ready: '已生成'
  }
  return labels[status] || status
}
</script>

<template>
  <main class="page-shell">
    <header class="topbar">
      <div>
        <p class="eyebrow">ScriptForge</p>
        <h1>小说转剧本工作台</h1>
      </div>
      <el-button type="primary" size="large" @click="dialogVisible = true">
        <el-icon><Plus /></el-icon>
        新建项目
      </el-button>
    </header>

    <section class="metric-strip">
      <div>
        <span>项目</span>
        <strong>{{ store.totalProjects }}</strong>
      </div>
      <div>
        <span>已导入小说</span>
        <strong>{{ importedCount }}</strong>
      </div>
      <div>
        <span>已生成剧本</span>
        <strong>{{ store.readyProjects }}</strong>
      </div>
    </section>

    <section v-loading="store.loading" class="project-board">
      <article
        v-for="project in store.projects"
        :key="project.id"
        class="project-card"
        @click="router.push(`/projects/${project.id}`)"
      >
        <div class="project-card-top">
          <el-tag effect="plain">{{ statusLabel(project.status) }}</el-tag>
          <el-button circle text @click.stop="deleteProject(project.id)">
            <el-icon><Delete /></el-icon>
          </el-button>
        </div>
        <h2>{{ project.title }}</h2>
        <p>{{ project.description || '暂无备注' }}</p>
        <div class="project-meta">
          <span>{{ project.chapterCount }} 章</span>
          <span>{{ project.hasScript ? 'YAML ready' : '待生成' }}</span>
        </div>
      </article>

      <el-empty v-if="!store.loading && !store.projects.length" description="暂无项目">
        <el-button type="primary" @click="dialogVisible = true">
          <el-icon><Plus /></el-icon>
          创建第一个项目
        </el-button>
      </el-empty>
    </section>

    <ProjectFormDialog v-model="dialogVisible" :loading="creating" @submit="createProject" />
  </main>
</template>
