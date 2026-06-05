<script setup lang="ts">
import { reactive, watch } from 'vue'

const props = defineProps<{
  modelValue: boolean
  loading?: boolean
}>()

const emit = defineEmits<{
  'update:modelValue': [value: boolean]
  submit: [payload: { title: string; description: string }]
}>()

const form = reactive({
  title: '',
  description: ''
})

watch(
  () => props.modelValue,
  (visible) => {
    if (visible) {
      form.title = ''
      form.description = ''
    }
  }
)

function submit() {
  emit('submit', {
    title: form.title.trim(),
    description: form.description.trim()
  })
}
</script>

<template>
  <el-dialog
    :model-value="modelValue"
    title="新建改编项目"
    width="520px"
    @update:model-value="emit('update:modelValue', $event)"
  >
    <el-form label-position="top" @submit.prevent="submit">
      <el-form-item label="项目标题" required>
        <el-input v-model="form.title" maxlength="80" placeholder="例如：雨夜来信改编" />
      </el-form-item>
      <el-form-item label="项目备注">
        <el-input
          v-model="form.description"
          type="textarea"
          :rows="4"
          maxlength="500"
          show-word-limit
          placeholder="改编目标、受众、题材方向"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="emit('update:modelValue', false)">取消</el-button>
      <el-button type="primary" :loading="loading" :disabled="!form.title.trim()" @click="submit">
        创建
      </el-button>
    </template>
  </el-dialog>
</template>
