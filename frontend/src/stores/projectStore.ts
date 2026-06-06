import { defineStore } from 'pinia'
import { projectApi, type ProjectPayload, type ProjectUpdatePayload } from '@/api/project'
import type { Project } from '@/api/types'

interface ProjectState {
  projects: Project[]
  currentProject: Project | null
  loading: boolean
}

export const useProjectStore = defineStore('projects', {
  state: (): ProjectState => ({
    projects: [],
    currentProject: null,
    loading: false
  }),
  getters: {
    totalProjects: (state) => state.projects.length,
    importedProjects: (state) => state.projects.filter((project) => project.hasNovel).length,
    readyProjects: (state) => state.projects.filter((project) => project.hasScript).length
  },
  actions: {
    upsertProject(project: Project) {
      const index = this.projects.findIndex((item) => item.id === project.id)
      if (index >= 0) {
        this.projects.splice(index, 1, project)
      } else {
        this.projects = [project, ...this.projects]
      }
      if (this.currentProject?.id === project.id) {
        this.currentProject = project
      }
    },
    async fetchProjects() {
      this.loading = true
      try {
        this.projects = await projectApi.listProjects()
      } finally {
        this.loading = false
      }
    },
    async fetchProject(projectId: number) {
      this.loading = true
      try {
        const project = await projectApi.getProject(projectId)
        this.currentProject = project
        this.upsertProject(project)
        return project
      } finally {
        this.loading = false
      }
    },
    async createProject(payload: ProjectPayload) {
      const project = await projectApi.createProject(payload)
      this.upsertProject(project)
      this.currentProject = project
      return project
    },
    async updateProject(projectId: number, payload: ProjectUpdatePayload) {
      const project = await projectApi.updateProject(projectId, payload)
      this.upsertProject(project)
      return project
    },
    async deleteProject(projectId: number) {
      await projectApi.deleteProject(projectId)
      this.projects = this.projects.filter((project) => project.id !== projectId)
      if (this.currentProject?.id === projectId) {
        this.currentProject = null
      }
    }
  }
})
