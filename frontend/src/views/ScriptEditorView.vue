<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const props = defineProps<{
  projectId: number
}>()
</script>

<template>
  <RouteShell
    eyebrow="Script YAML"
    title="YAML 剧本编辑"
    summary="编辑、保存、校验、修复并导出完整 YAML 剧本。"
    active-key="script"
    :project-id="props.projectId"
  >
    <section class="placeholder-panel editor-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Editor</p>
          <h2>剧本文本</h2>
        </div>
        <el-tag effect="plain">not loaded</el-tag>
      </div>

      <el-input
        class="yaml-editor"
        type="textarea"
        resize="none"
        :rows="24"
        disabled
        placeholder="project:"
      />

      <div class="panel-actions split-actions">
        <div>
          <el-button disabled>
            <el-icon><CircleCheck /></el-icon>
            校验
          </el-button>
          <el-button disabled>
            <el-icon><Refresh /></el-icon>
            修复
          </el-button>
          <el-button type="primary" disabled>
            <el-icon><EditPen /></el-icon>
            保存
          </el-button>
        </div>
        <div>
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
