import { apiClient } from '@/shared/api'
import type { DependencyGraph, GenerationPlanEntry } from '@/entities/preprocessing'

/** Graphe global des dépendances entre les fichiers du catalogue. */
export function getDependencyGraph() {
  return apiClient
    .get<DependencyGraph>('/api/v1/preprocessing/dependency-graph')
    .then((r) => r.data)
}

/** Plan de prétraitement du projet, trié par niveau topologique. */
export function getGenerationPlan(projectId: string) {
  return apiClient
    .get<GenerationPlanEntry[]>(`/api/v1/projects/${projectId}/preprocessing/plan`)
    .then((r) => r.data)
}
