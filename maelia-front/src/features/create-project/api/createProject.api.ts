import { apiClient } from '@/shared/api'
import type { Project } from '@/entities/project'

export interface CreateProjectRequest {
  name: string
  description?: string
}

export function createProject(req: CreateProjectRequest) {
  return apiClient.post<Project>('/api/v1/projects', req).then((r) => r.data)
}

export function listProjects() {
  return apiClient.get<Project[]>('/api/v1/projects').then((r) => r.data)
}

export function getProject(id: string) {
  return apiClient.get<Project>(`/api/v1/projects/${id}`).then((r) => r.data)
}
