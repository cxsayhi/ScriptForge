import { createRouter, createWebHistory } from 'vue-router'
import ProjectListView from '@/views/ProjectListView.vue'
import WorkspaceView from '@/views/WorkspaceView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'projects',
      component: ProjectListView
    },
    {
      path: '/projects/:id',
      name: 'workspace',
      component: WorkspaceView
    }
  ]
})

export default router
