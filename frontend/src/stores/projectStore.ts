import { defineStore } from 'pinia'
import { projectApi, type ProjectPayload } from '@/api/project'
import type { Project } from '@/api/types'

interface ProjectState {
  projects: Project[]
  loading: boolean
}

export const useProjectStore = defineStore('projects', {
  state: (): ProjectState => ({
    projects: [],
    loading: false
  }),
  getters: {
    totalProjects: (state) => state.projects.length,
    readyProjects: (state) => state.projects.filter((project) => project.hasScript).length
  },
  actions: {
    async fetchProjects() {
      this.loading = true
      try {
        this.projects = await projectApi.listProjects()
      } finally {
        this.loading = false
      }
    },
    async createProject(payload: ProjectPayload) {
      const project = await projectApi.createProject(payload)
      this.projects = [project, ...this.projects]
      return project
    },
    async deleteProject(projectId: number) {
      await projectApi.deleteProject(projectId)
      this.projects = this.projects.filter((project) => project.id !== projectId)
    }
  }
})
