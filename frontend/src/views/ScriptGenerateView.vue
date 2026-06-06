<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const props = defineProps<{
  projectId: number
}>()

const checks = ['项目存在', '小说已提交', '章节不少于 3 个', '配置已保存', '当前剧本状态']
</script>

<template>
  <RouteShell
    eyebrow="Generation"
    title="剧本生成"
    summary="汇总生成前检查项，并触发 YAML 剧本初稿生成。"
    active-key="generate"
    :project-id="props.projectId"
  >
    <section class="placeholder-layout wide-left">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Checklist</p>
            <h2>生成前检查</h2>
          </div>
          <el-tag type="warning" effect="plain">idle</el-tag>
        </div>
        <ul class="check-list">
          <li v-for="item in checks" :key="item">
            <span class="check-dot" />
            {{ item }}
          </li>
        </ul>
      </div>

      <aside class="placeholder-panel generation-panel">
        <p class="eyebrow">Agent</p>
        <h2>生成控制</h2>
        <div class="generation-actions">
          <el-button type="primary" disabled>
            <el-icon><MagicStick /></el-icon>
            生成剧本
          </el-button>
          <el-button disabled>
            <el-icon><Refresh /></el-icon>
            重新生成
          </el-button>
        </div>
      </aside>
    </section>
  </RouteShell>
</template>
