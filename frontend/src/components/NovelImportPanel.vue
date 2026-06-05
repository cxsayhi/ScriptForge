<script setup lang="ts">
import { ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import type { UploadFile } from 'element-plus'
import { projectApi } from '@/api/project'
import type { Chapter, NovelContent } from '@/api/types'

const props = defineProps<{
  projectId: number
  initialText: string
  chapters: Chapter[]
}>()

const emit = defineEmits<{
  saved: [novel: NovelContent]
}>()

const draftText = ref('')
const saving = ref(false)

watch(
  () => props.initialText,
  (value) => {
    draftText.value = value || ''
  },
  { immediate: true }
)

async function saveText() {
  saving.value = true
  try {
    const novel = await projectApi.saveNovelText(props.projectId, draftText.value)
    emit('saved', novel)
    ElMessage.success('小说文本已保存')
  } finally {
    saving.value = false
  }
}

async function handleFile(file: UploadFile) {
  if (!file.raw) {
    return
  }
  saving.value = true
  try {
    const novel = await projectApi.uploadNovelFile(props.projectId, file.raw)
    draftText.value = novel.originalText
    emit('saved', novel)
    ElMessage.success('文件已导入')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <section class="workspace-panel import-panel">
    <div class="panel-heading">
      <div>
        <p class="eyebrow">Novel Input</p>
        <h2>小说导入</h2>
      </div>
      <el-tag :type="chapters.length >= 3 ? 'success' : 'warning'" effect="plain">
        {{ chapters.length }} 章
      </el-tag>
    </div>

    <div class="import-grid">
      <el-upload
        drag
        action="#"
        accept=".txt,.md"
        :auto-upload="false"
        :show-file-list="false"
        :on-change="handleFile"
      >
        <el-icon class="upload-icon"><UploadFilled /></el-icon>
        <div class="upload-text">上传 .txt / .md 小说文件</div>
      </el-upload>

      <div class="chapter-list">
        <div v-for="chapter in chapters" :key="chapter.index" class="chapter-row">
          <span>{{ chapter.index }}</span>
          <div>
            <strong>{{ chapter.title }}</strong>
            <p>{{ chapter.preview || '暂无预览' }}</p>
          </div>
        </div>
        <el-empty v-if="!chapters.length" :image-size="76" description="尚未解析章节" />
      </div>
    </div>

    <el-input
      v-model="draftText"
      class="novel-textarea"
      type="textarea"
      resize="none"
      :rows="14"
      placeholder="粘贴包含至少 3 个章节标题的小说正文"
    />

    <div class="panel-actions">
      <el-button type="primary" :loading="saving" :disabled="!draftText.trim()" @click="saveText">
        <el-icon><DocumentChecked /></el-icon>
        保存并解析
      </el-button>
    </div>
  </section>
</template>
