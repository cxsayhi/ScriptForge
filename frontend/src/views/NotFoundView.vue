<template>
  <main class="page-shell not-found-shell">
    <section class="not-found-panel">
      <p class="eyebrow">404</p>
      <h1>页面不存在</h1>
      <RouterLink to="/projects" custom v-slot="{ href, navigate }">
        <el-button type="primary" :href="href" tag="a" @click="navigate">
          <el-icon><Back /></el-icon>
          返回项目列表
        </el-button>
      </RouterLink>
    </section>
  </main>
</template>
