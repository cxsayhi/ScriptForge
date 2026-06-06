<script setup lang="ts">
import { computed } from 'vue'

interface ShellStat {
  label: string
  value: string
  tone?: 'neutral' | 'ready' | 'warn'
}

const props = withDefaults(
  defineProps<{
    eyebrow: string
    title: string
    summary: string
    activeKey?: string
    projectId?: number
    stats?: ShellStat[]
  }>(),
  {
    activeKey: 'projects',
    projectId: undefined,
    stats: () => []
  }
)

const navItems = computed(() => {
  const items = [{ key: 'projects', label: '项目', to: '/projects' }]

  if (!props.projectId) {
    items.push({ key: 'schema', label: 'Schema', to: '/schema' })
    return items
  }

  return [
    ...items,
    { key: 'detail', label: '总览', to: `/projects/${props.projectId}` },
    { key: 'novel', label: '小说', to: `/projects/${props.projectId}/novel` },
    { key: 'settings', label: '配置', to: `/projects/${props.projectId}/settings` },
    { key: 'generate', label: '生成', to: `/projects/${props.projectId}/generate` },
    { key: 'script', label: '剧本', to: `/projects/${props.projectId}/script` },
    { key: 'schema', label: 'Schema', to: '/schema' }
  ]
})
</script>

<template>
  <main class="page-shell route-shell">
    <header class="topbar route-topbar">
      <div class="route-title">
        <p class="eyebrow">{{ eyebrow }}</p>
        <h1>{{ title }}</h1>
        <p class="route-summary">{{ summary }}</p>
      </div>
      <div class="topbar-actions route-actions">
        <slot name="actions" />
      </div>
    </header>

    <nav class="route-nav" aria-label="主流程导航">
      <RouterLink
        v-for="item in navItems"
        :key="item.key"
        v-slot="{ href, navigate, isActive }"
        :to="item.to"
        custom
      >
        <a
          :href="href"
          class="route-nav-link"
          :class="{ active: activeKey === item.key || isActive }"
          @click="navigate"
        >
          {{ item.label }}
        </a>
      </RouterLink>
    </nav>

    <section v-if="stats.length" class="route-stat-strip" aria-label="页面状态">
      <div v-for="stat in stats" :key="stat.label" class="route-stat" :class="stat.tone || 'neutral'">
        <span>{{ stat.label }}</span>
        <strong>{{ stat.value }}</strong>
      </div>
    </section>

    <slot />
  </main>
</template>
