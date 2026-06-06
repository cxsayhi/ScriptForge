import { createRouter, createWebHistory } from 'vue-router'
import AdaptationSettingView from '@/views/AdaptationSettingView.vue'
import NotFoundView from '@/views/NotFoundView.vue'
import NovelInputView from '@/views/NovelInputView.vue'
import ProjectDetailView from '@/views/ProjectDetailView.vue'
import ProjectListView from '@/views/ProjectListView.vue'
import SchemaView from '@/views/SchemaView.vue'
import ScriptEditorView from '@/views/ScriptEditorView.vue'
import ScriptGenerateView from '@/views/ScriptGenerateView.vue'

function projectRouteProps(route: { params: Record<string, string | string[]> }) {
  return {
    projectId: Number(route.params.projectId)
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      redirect: '/projects'
    },
    {
      path: '/projects',
      name: 'projects',
      component: ProjectListView
    },
    {
      path: '/projects/:projectId',
      name: 'project-detail',
      component: ProjectDetailView,
      props: projectRouteProps
    },
    {
      path: '/projects/:projectId/novel',
      name: 'project-novel',
      component: NovelInputView,
      props: projectRouteProps
    },
    {
      path: '/projects/:projectId/settings',
      name: 'project-settings',
      component: AdaptationSettingView,
      props: projectRouteProps
    },
    {
      path: '/projects/:projectId/generate',
      name: 'project-generate',
      component: ScriptGenerateView,
      props: projectRouteProps
    },
    {
      path: '/projects/:projectId/script',
      name: 'project-script',
      component: ScriptEditorView,
      props: projectRouteProps
    },
    {
      path: '/schema',
      name: 'schema',
      component: SchemaView
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: NotFoundView
    }
  ]
})

export default router
