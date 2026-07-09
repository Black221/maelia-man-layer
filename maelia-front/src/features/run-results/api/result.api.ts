import { apiClient } from '@/shared/api'
import { API_BASE_URL } from '@/shared/config/env'
import type { ResultSummary } from '@/entities/result'

export function getRunResults(runId: string) {
  return apiClient.get<ResultSummary>(`/api/v1/runs/${runId}/results`).then((r) => r.data)
}

/** URL absolue d'un artefact (pour <img>, téléchargement). */
export function artifactHref(url: string): string {
  return `${API_BASE_URL}${url}`
}
