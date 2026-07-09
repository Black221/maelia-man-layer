import { apiClient } from '@/shared/api'
import type { ParameterSpec, ParameterGroup } from '@/entities/scenario'

/** Corps de création/mise à jour d'un paramètre (miroir de ParameterSpecUpsertRequest). */
export interface ParameterUpsertRequest {
  gamlName: string
  label: string
  group: string
  type: string
  defaultValue?: string | null
  unit?: string | null
  allowedValues?: string[] | null
  visibleIf?: string | null
  enabledIf?: string | null
  optionsDataSpec?: string | null
  advanced: boolean
  order: number
}

export function listParameters(): Promise<ParameterSpec[]> {
  return apiClient.get<ParameterSpec[]>('/api/v1/scenario-parameters').then((r) => r.data)
}

export function listParameterGroups(): Promise<ParameterGroup[]> {
  return apiClient.get<ParameterGroup[]>('/api/v1/scenario-parameters/groups').then((r) => r.data)
}

export function createParameter(req: ParameterUpsertRequest): Promise<ParameterSpec> {
  return apiClient.post<ParameterSpec>('/api/v1/admin/scenario-parameters', req).then((r) => r.data)
}

export function updateParameter(gamlName: string, req: ParameterUpsertRequest): Promise<ParameterSpec> {
  return apiClient
    .put<ParameterSpec>(`/api/v1/admin/scenario-parameters/${encodeURIComponent(gamlName)}`, req)
    .then((r) => r.data)
}

export function deleteParameter(gamlName: string): Promise<void> {
  return apiClient
    .delete(`/api/v1/admin/scenario-parameters/${encodeURIComponent(gamlName)}`)
    .then(() => undefined)
}
