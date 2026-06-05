<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { projectApi } from '@/api/project'
import type { ScriptResponse, ValidationResult } from '@/api/types'
import { exportUrl } from '@/utils/download'

const props = defineProps<{
  projectId: number
  script: ScriptResponse | null
}>()

const emit = defineEmits<{
  refreshed: [script: ScriptResponse]
}>()

const yaml = ref('')
const validation = ref<ValidationResult | null>(null)
const loading = ref(false)

watch(
  () => props.script,
  (script) => {
    yaml.value = script?.yaml || ''
    validation.value = script?.validationResult || null
  },
  { immediate: true }
)

async function validate() {
  loading.value = true
  try {
    validation.value = await projectApi.validateScript(props.projectId, yaml.value)
    ElMessage[validation.value.valid ? 'success' : 'warning'](validation.value.valid ? 'YAML 校验通过' : 'YAML 仍有错误')
  } finally {
    loading.value = false
  }
}

async function repair() {
  loading.value = true
  try {
    const result = await projectApi.repairScript(props.projectId, yaml.value)
    yaml.value = result.yaml
    validation.value = result.validationResult
    ElMessage[result.validationResult.valid ? 'success' : 'warning'](result.validationResult.valid ? '已完成修复' : '已尝试修复')
  } finally {
    loading.value = false
  }
}

async function save() {
  loading.value = true
  try {
    const script = await projectApi.saveScript(props.projectId, yaml.value)
    emit('refreshed', script)
    ElMessage.success('YAML 剧本已保存')
  } finally {
    loading.value = false
  }
}
</script>

<template>
  <section class="workspace-panel editor-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">Script YAML</p>
        <h2>剧本编辑</h2>
      </div>
      <el-tag v-if="validation" :type="validation.valid ? 'success' : 'danger'" effect="plain">
        {{ validation.valid ? 'Valid' : 'Invalid' }}
      </el-tag>
    </div>

    <el-input
      v-model="yaml"
      class="yaml-editor"
      type="textarea"
      resize="none"
      :rows="24"
      spellcheck="false"
      placeholder="生成后将在这里编辑 YAML 剧本"
    />

    <div v-if="validation && (!validation.valid || validation.warnings.length)" class="validation-box">
      <div v-for="error in validation.errors" :key="error" class="validation-line error">
        {{ error }}
      </div>
      <div v-for="warning in validation.warnings" :key="warning" class="validation-line warning">
        {{ warning }}
      </div>
    </div>

    <div class="panel-actions split-actions">
      <div>
        <el-button :loading="loading" @click="validate">
          <el-icon><CircleCheck /></el-icon>
          校验
        </el-button>
        <el-button :loading="loading" @click="repair">
          <el-icon><Refresh /></el-icon>
          修复
        </el-button>
        <el-button type="primary" :loading="loading" :disabled="!yaml.trim()" @click="save">
          <el-icon><EditPen /></el-icon>
          保存
        </el-button>
      </div>
      <div>
        <el-button tag="a" :href="exportUrl(projectId, 'yaml')" target="_blank">
          <el-icon><Download /></el-icon>
          YAML
        </el-button>
        <el-button tag="a" :href="exportUrl(projectId, 'markdown')" target="_blank">
          <el-icon><Download /></el-icon>
          Markdown
        </el-button>
      </div>
    </div>
  </section>
</template>
