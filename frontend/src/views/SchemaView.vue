<script setup lang="ts">
import RouteShell from '@/components/RouteShell.vue'
</script>

<template>
  <RouteShell
    eyebrow="Schema"
    title="YAML Schema"
    summary="查阅剧本 YAML 的结构定义和设计说明。"
    active-key="schema"
  >
    <section class="schema-grid">
      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Schema</p>
            <h2>结构定义</h2>
          </div>
        </div>
        <pre class="schema-code">project:
  title:
characters:
episodes:</pre>
      </div>

      <div class="placeholder-panel">
        <div class="panel-heading">
          <div>
            <p class="eyebrow">Design</p>
            <h2>设计说明</h2>
          </div>
        </div>
        <pre class="schema-code">剧本结构、角色、分集、场景与对白字段。</pre>
      </div>
    </section>
  </RouteShell>
</template>
