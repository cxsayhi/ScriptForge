<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const stats = [
  { label: '项目', value: '0', tone: 'neutral' as const },
  { label: '已导入小说', value: '0', tone: 'warn' as const },
  { label: '已生成剧本', value: '0', tone: 'ready' as const }
]
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
      <el-button type="primary" disabled>
        <el-icon><Plus /></el-icon>
        新建项目
      </el-button>
    </template>

    <section class="placeholder-layout">
      <div class="placeholder-panel project-index-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Projects</p>
            <h2>项目档案</h2>
          </div>
          <el-tag effect="plain">列表</el-tag>
        </div>
        <div class="placeholder-card-grid">
          <article v-for="index in 3" :key="index" class="placeholder-card">
            <div class="placeholder-line short" />
            <div class="placeholder-line title" />
            <div class="placeholder-line" />
            <div class="placeholder-line thin" />
          </article>
        </div>
      </div>

      <aside class="placeholder-panel">
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
  </RouteShell>
</template>
