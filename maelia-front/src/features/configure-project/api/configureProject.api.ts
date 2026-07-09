import { apiClient } from '@/shared/api'
import type { CompletionEntry, ModelingConfiguration, Project } from '@/entities/project'
import type { BulkImportReport, DataSpec } from '@/entities/dataset'

export function updateModelingConfig(projectId: string, config: ModelingConfiguration) {
  return apiClient
    .put<Project>(`/api/v1/projects/${projectId}/modeling-configuration`, config)
    .then((r) => r.data)
}

export function getProjectCompletion(projectId: string) {
  return apiClient
    .get<CompletionEntry[]>(`/api/v1/projects/${projectId}/completion`)
    .then((r) => r.data)
}

/** Édition des informations générales du projet (page Initialisation). */
export function updateProject(projectId: string, body: { name: string; description: string | null }) {
  return apiClient.put<Project>(`/api/v1/projects/${projectId}`, body).then((r) => r.data)
}

/** Fichiers applicables pour une configuration (aperçu en direct des modules activés). */
export function getApplicableDataSpecs(config: ModelingConfiguration) {
  return apiClient.post<DataSpec[]>('/api/v1/dataspecs/applicable', config).then((r) => r.data)
}

/** Initialisation en masse : upload d'un ZIP de fichiers d'entrée, rapport par fichier. */
export function importInitZip(projectId: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiClient
    .post<BulkImportReport>(`/api/v1/projects/${projectId}/datasets/import-zip`, form)
    .then((r) => r.data)
}
