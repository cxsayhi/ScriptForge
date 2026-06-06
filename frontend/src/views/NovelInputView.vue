<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const props = defineProps<{
  projectId: number
}>()

const stats = [
  { label: '章节', value: '0', tone: 'warn' as const },
  { label: '最低要求', value: '3 章', tone: 'neutral' as const }
]
</script>

<template>
  <RouteShell
    eyebrow="Novel Input"
    title="小说导入"
    summary="接收小说文件或正文文本，并承载章节解析结果。"
    active-key="novel"
    :project-id="props.projectId"
    :stats="stats"
  >
    <section class="placeholder-layout wide-left">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Text</p>
            <h2>原文输入</h2>
          </div>
          <el-button type="primary" disabled>
            <el-icon><DocumentChecked /></el-icon>
            保存并解析
          </el-button>
        </div>
        <el-input
          class="novel-textarea"
          type="textarea"
          resize="none"
          :rows="14"
          disabled
          placeholder="小说正文"
        />
      </div>

      <aside class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Chapters</p>
            <h2>章节列表</h2>
          </div>
          <el-tag type="warning" effect="plain">0 章</el-tag>
        </div>
        <div class="chapter-skeleton-list">
          <div v-for="index in 4" :key="index" class="chapter-skeleton-row">
            <span>{{ index }}</span>
            <div>
              <div class="placeholder-line title" />
              <div class="placeholder-line thin" />
            </div>
          </div>
        </div>
      </aside>
    </section>
  </RouteShell>
</template>
