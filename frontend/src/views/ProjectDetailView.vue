<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const props = defineProps<{
  projectId: number
}>()

const stats = [
  { label: '小说', value: '未导入', tone: 'warn' as const },
  { label: '配置', value: '默认值', tone: 'neutral' as const },
  { label: '剧本', value: '未生成', tone: 'neutral' as const }
]
</script>

<template>
  <RouteShell
    eyebrow="Project Overview"
    title="项目总览"
    summary="集中查看项目基本状态，并进入导入、配置、生成和编辑环节。"
    active-key="detail"
    :project-id="props.projectId"
    :stats="stats"
  >
    <section class="placeholder-layout">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Project</p>
            <h2>项目信息</h2>
          </div>
          <el-tag effect="plain">draft</el-tag>
        </div>
        <dl class="detail-list">
          <div>
            <dt>项目编号</dt>
            <dd>#{{ props.projectId }}</dd>
          </div>
          <div>
            <dt>项目标题</dt>
            <dd>未命名改编项目</dd>
          </div>
          <div>
            <dt>更新时间</dt>
            <dd>-</dd>
          </div>
        </dl>
      </div>

      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Exports</p>
            <h2>文件导出</h2>
          </div>
        </div>
        <div class="export-button-row">
          <el-button disabled>
            <el-icon><Download /></el-icon>
            YAML
          </el-button>
          <el-button disabled>
            <el-icon><Download /></el-icon>
            Markdown
          </el-button>
        </div>
      </div>
    </section>
  </RouteShell>
</template>
