<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'

const props = defineProps<{
  projectId: number
}>()

const stats = [
  { label: '剧本类型', value: 'web_drama', tone: 'neutral' as const },
  { label: '集数', value: '3', tone: 'neutral' as const },
  { label: '对白', value: '影视化', tone: 'ready' as const }
]
</script>

<template>
  <RouteShell
    eyebrow="Adaptation"
    title="改编配置"
    summary="保存剧本类型、目标集数、语言风格和制作倾向。"
    active-key="settings"
    :project-id="props.projectId"
    :stats="stats"
  >
    <section class="placeholder-panel">
      <div class="panel-heading">
        <div>
          <p class="eyebrow">Settings</p>
          <h2>基础参数</h2>
        </div>
        <el-button type="primary" disabled>
          <el-icon><Check /></el-icon>
          保存
        </el-button>
      </div>

      <el-form class="settings-form" label-position="top" disabled>
        <el-form-item label="剧本类型">
          <el-select model-value="web_drama">
            <el-option label="网剧" value="web_drama" />
          </el-select>
        </el-form-item>
        <el-form-item label="目标集数">
          <el-input-number :model-value="3" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="单集分钟">
          <el-input-number :model-value="8" :min="1" controls-position="right" />
        </el-form-item>
        <el-form-item label="语言">
          <el-select model-value="zh-CN">
            <el-option label="中文" value="zh-CN" />
          </el-select>
        </el-form-item>
      </el-form>
    </section>
  </RouteShell>
</template>
