<script setup lang="ts">
import { computed, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import RouteShell from '@/components/RouteShell.vue'
import { novelApi } from '@/api/novel'
import type { Chapter, NovelContent } from '@/api/types'
import { useProjectStore } from '@/stores/projectStore'

const props = defineProps<{
  projectId: number
}>()

const router = useRouter()
const projectStore = useProjectStore()
const novel = ref<NovelContent | null>(null)
const draftText = ref('')
const loading = ref(false)
const saving = ref(false)
const uploadedFileName = ref('')
const fileInput = ref<HTMLInputElement | null>(null)

const project = computed(() => projectStore.currentProject)
const chapters = computed<Chapter[]>(() => novel.value?.chapters || [])
const savedChapterCount = computed(() => novel.value?.chapterCount || 0)
const draftChapterEstimate = computed(() => estimateChapterCount(draftText.value))
const hasDraft = computed(() => draftText.value.trim().length > 0)
const draftMeetsMinimum = computed(() => draftChapterEstimate.value >= 3)
const canContinue = computed(() => savedChapterCount.value >= 3)
const isSavedText = computed(() => draftText.value === (novel.value?.originalText || ''))

const stats = computed(() => [
  {
    label: '已解析章节',
    value: `${savedChapterCount.value} 章`,
    tone: canContinue.value ? ('ready' as const) : ('warn' as const)
  },
  {
    label: '文本预估',
    value: hasDraft.value ? `${draftChapterEstimate.value} 章` : '-',
    tone: !hasDraft.value || draftMeetsMinimum.value ? ('neutral' as const) : ('warn' as const)
  },
  {
    label: '最低要求',
    value: '3 章',
    tone: 'neutral' as const
  }
])

watch(
  () => props.projectId,
  (projectId) => {
    loadNovel(projectId).catch(() => undefined)
  },
  { immediate: true }
)

async function loadNovel(projectId: number) {
  loading.value = true
  try {
    const [projectData, novelData] = await Promise.all([
      projectStore.fetchProject(projectId),
      novelApi.getNovelContent(projectId)
    ])
    projectStore.upsertProject(projectData)
    novel.value = novelData
    draftText.value = novelData.originalText || ''
    uploadedFileName.value = ''
  } finally {
    loading.value = false
  }
}

function normalizeText(text: string) {
  return text.replace(/\r\n/g, '\n').replace(/\r/g, '\n').replace(/\uFEFF/g, ' ').trim()
}

function estimateChapterCount(text: string) {
  const normalized = normalizeText(text)
  if (!normalized) {
    return 0
  }

  const headings = normalized.match(
    /^\s*(?:#{1,6}\s*)?(?:(?:第\s*[0-9一二三四五六七八九十百千零两]+\s*[章节回卷].*)|(?:Chapter\s+\d+.*)|(?:CHAPTER\s+\d+.*))\s*$/gm
  )

  if (headings?.length) {
    return headings.length
  }

  return normalized.split(/\n\s*\n\s*\n+/).filter((block) => block.trim().length >= 120).length
}

function formatDate(value?: string) {
  if (!value) return '-'
  return new Intl.DateTimeFormat('zh-CN', {
    year: 'numeric',
    month: '2-digit',
    day: '2-digit',
    hour: '2-digit',
    minute: '2-digit'
  }).format(new Date(value))
}

function warnInsufficientChapters(count: number) {
  ElMessage.warning(`当前文本预计只有 ${count} 章，至少需要 3 章后才能保存解析。`)
}

async function saveText() {
  if (!hasDraft.value) {
    ElMessage.warning('请先粘贴小说正文')
    return
  }
  if (!draftMeetsMinimum.value) {
    warnInsufficientChapters(draftChapterEstimate.value)
    return
  }

  saving.value = true
  try {
    const saved = await novelApi.submitNovelText(props.projectId, { text: draftText.value })
    novel.value = saved
    await projectStore.fetchProject(props.projectId)
    ElMessage.success('小说文本已保存并解析')
  } finally {
    saving.value = false
  }
}

async function handleFileInput(event: Event) {
  const input = event.target as HTMLInputElement
  const file = input.files?.[0]
  input.value = ''
  if (!file) {
    return
  }

  await handleFile(file)
}

async function handleFile(file: File) {
  const isAllowed = /\.(txt|md)$/i.test(file.name)
  if (!isAllowed) {
    ElMessage.warning('仅支持上传 .txt 或 .md 文件')
    return
  }

  const text = await file.text()
  const estimated = estimateChapterCount(text)
  draftText.value = text
  uploadedFileName.value = file.name

  if (estimated < 3) {
    warnInsufficientChapters(estimated)
    return
  }

  saving.value = true
  try {
    const saved = await novelApi.uploadNovelFile(props.projectId, file)
    novel.value = saved
    draftText.value = saved.originalText
    await projectStore.fetchProject(props.projectId)
    ElMessage.success('小说文件已上传并解析')
  } finally {
    saving.value = false
  }
}
</script>

<template>
  <RouteShell
    eyebrow="Novel Input"
    :title="project?.title ? `${project.title}：小说导入` : '小说导入'"
    summary="上传小说文件或粘贴正文，解析章节后进入改编配置。"
    active-key="novel"
    :project-id="props.projectId"
    :stats="stats"
  >
    <template #actions>
      <el-button @click="router.push(`/projects/${props.projectId}`)">
        <el-icon><Back /></el-icon>
        项目总览
      </el-button>
      <el-button type="primary" :disabled="!canContinue" @click="router.push(`/projects/${props.projectId}/settings`)">
        <el-icon><Right /></el-icon>
        继续配置
      </el-button>
    </template>

    <section v-loading="loading" class="novel-input-layout">
      <div class="placeholder-panel novel-editor-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Text</p>
            <h2>原文输入</h2>
          </div>
          <div class="novel-action-row">
            <input
              ref="fileInput"
              class="visually-hidden-file"
              type="file"
              accept=".txt,.md"
              @change="handleFileInput"
            />
            <el-button :loading="saving" @click="fileInput?.click()">
              <el-icon><UploadFilled /></el-icon>
              上传文件
            </el-button>
            <el-button
              type="primary"
              :loading="saving"
              :disabled="!hasDraft || !draftMeetsMinimum || isSavedText"
              @click="saveText"
            >
              <el-icon><DocumentChecked /></el-icon>
              保存并解析
            </el-button>
          </div>
        </div>

        <el-alert
          v-if="hasDraft && !draftMeetsMinimum"
          class="novel-warning"
          type="warning"
          :closable="false"
          show-icon
          title="章节数量不足"
          :description="`当前文本预计只有 ${draftChapterEstimate} 章，至少需要 3 章。可使用“第一章 / 第二章 / Chapter 1”这样的独立章节标题。`"
        />

        <el-alert
          v-else-if="canContinue"
          class="novel-warning"
          type="success"
          :closable="false"
          show-icon
          :title="`已解析 ${savedChapterCount} 个章节`"
          description="小说内容已满足生成前置条件，可以继续填写改编配置。"
        />

        <div class="novel-import-bar">
          <div>
            <span>来源</span>
            <strong>{{ uploadedFileName || '粘贴文本 / 已保存内容' }}</strong>
          </div>
          <div>
            <span>最近保存</span>
            <strong>{{ formatDate(novel?.updatedAt) }}</strong>
          </div>
          <div>
            <span>字符数</span>
            <strong>{{ draftText.length }}</strong>
          </div>
        </div>

        <el-input
          v-model="draftText"
          class="novel-textarea"
          type="textarea"
          resize="none"
          :rows="18"
          spellcheck="false"
          placeholder="粘贴包含至少 3 个章节标题的小说正文，例如：第一章 雨夜"
        />

        <div class="panel-actions split-actions">
          <span class="novel-helper-text">保存后将以最后一次提交的文本或文件作为项目小说内容。</span>
          <el-button text @click="draftText = novel?.originalText || ''">恢复已保存文本</el-button>
        </div>
      </div>

      <aside class="placeholder-panel novel-chapter-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Chapters</p>
            <h2>章节解析</h2>
          </div>
          <el-tag :type="canContinue ? 'success' : 'warning'" effect="plain">{{ savedChapterCount }} 章</el-tag>
        </div>

        <div v-if="chapters.length" class="chapter-list rich-chapter-list">
          <article v-for="chapter in chapters" :key="chapter.index" class="chapter-row rich-chapter-row">
            <span>{{ chapter.index }}</span>
            <div>
              <strong>{{ chapter.title }}</strong>
              <p>{{ chapter.preview || '暂无预览' }}</p>
              <small>{{ chapter.contentLength }} 字</small>
            </div>
          </article>
        </div>

        <el-empty v-else :image-size="82" description="尚未解析章节">
          <p class="empty-note">粘贴正文或上传文件后，章节会在这里出现。</p>
        </el-empty>
      </aside>
    </section>
  </RouteShell>
</template>
