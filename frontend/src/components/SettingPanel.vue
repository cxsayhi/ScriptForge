<script setup lang="ts">
import { reactive, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'
import type { AdaptationSetting } from '@/api/types'

const props = defineProps<{
  projectId: number
  setting: AdaptationSetting
}>()

const emit = defineEmits<{
  saved: [setting: AdaptationSetting]
}>()

const form = reactive<AdaptationSetting>({
  scriptType: 'web_drama',
  targetEpisodes: 3,
  episodeDurationMinutes: 8,
  style: '悬疑',
  language: 'zh-CN',
  adaptationIntensity: '适度改编',
  dialogueStyle: '影视化',
  budgetPreference: '可拍摄优先',
  keepOriginalDialogues: true
})

watch(
  () => props.setting,
  (setting) => Object.assign(form, setting),
  { immediate: true, deep: true }
)

async function saveSettings() {
  const saved = await projectApi.saveSettings(props.projectId, { ...form })
  Object.assign(form, saved)
  emit('saved', saved)
  ElMessage.success('改编配置已保存')
}
</script>

<template>
  <section class="workspace-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">Adaptation</p>
        <h2>改编配置</h2>
      </div>
      <el-button type="primary" plain @click="saveSettings">
        <el-icon><Check /></el-icon>
        保存
      </el-button>
    </div>

    <el-form class="settings-form" label-position="top">
      <el-form-item label="剧本类型">
        <el-select v-model="form.scriptType">
          <el-option label="网剧" value="web_drama" />
          <el-option label="短剧" value="short_drama" />
          <el-option label="电影" value="movie" />
          <el-option label="舞台剧" value="stage_play" />
        </el-select>
      </el-form-item>
      <el-form-item label="目标集数">
        <el-input-number v-model="form.targetEpisodes" :min="1" :max="80" controls-position="right" />
      </el-form-item>
      <el-form-item label="单集分钟">
        <el-input-number v-model="form.episodeDurationMinutes" :min="1" :max="180" controls-position="right" />
      </el-form-item>
      <el-form-item label="风格">
        <el-select v-model="form.style" filterable allow-create>
          <el-option label="悬疑" value="悬疑" />
          <el-option label="甜宠" value="甜宠" />
          <el-option label="都市" value="都市" />
          <el-option label="古风" value="古风" />
          <el-option label="喜剧" value="喜剧" />
        </el-select>
      </el-form-item>
      <el-form-item label="语言">
        <el-select v-model="form.language">
          <el-option label="中文" value="zh-CN" />
          <el-option label="English" value="en-US" />
        </el-select>
      </el-form-item>
      <el-form-item label="改编强度">
        <el-segmented v-model="form.adaptationIntensity" :options="['忠于原著', '适度改编', '大幅改编']" />
      </el-form-item>
      <el-form-item label="对白风格">
        <el-segmented v-model="form.dialogueStyle" :options="['口语化', '文艺化', '影视化']" />
      </el-form-item>
      <el-form-item label="预算倾向">
        <el-select v-model="form.budgetPreference">
          <el-option label="少角色" value="少角色" />
          <el-option label="少场景" value="少场景" />
          <el-option label="可拍摄优先" value="可拍摄优先" />
        </el-select>
      </el-form-item>
      <el-form-item label="保留原文对白">
        <el-switch v-model="form.keepOriginalDialogues" />
      </el-form-item>
    </el-form>
  </section>
</template>
