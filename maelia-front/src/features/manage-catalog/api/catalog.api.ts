import { apiClient } from '@/shared/api'
import type { DataSpec } from '@/entities/dataset'

/** Corps de création/édition d'un fichier (DataSpec). Les champs sont gérés à part. */
export interface DataSpecUpsert {
  id: string
  module: string
  folder?: string | null
  fileName: string
  fileType?: string
  csvFormat?: string | null
  orientation?: string
  matrixValueStartIndex?: number | null
  delimiter?: string | null
  generation?: string
  required?: boolean
  requiredIf?: string | null
  temporalResolution?: string | null
  multiInstance?: boolean
  instancePattern?: string | null
  saisieMode?: string
  description?: string | null
  fieldsStatus?: string
}

export interface FieldSpecUpsert {
  label: string
  position?: number | null
  infoType?: string
  unit?: string | null
  required?: boolean
  requiredIf?: string | null
  referencesDataSpec?: string | null
  description?: string | null
  listSeparator?: string | null
  allowedValues?: string[]
}

export interface CatalogUsage {
  dataSpecId: string
  datasetCount: number
  referencedByFields: string[]
}

const BASE = '/api/v1/dataspecs'
const ADMIN = '/api/v1/admin/dataspecs'

export function listDataSpecs() {
  return apiClient.get<DataSpec[]>(BASE).then((r) => r.data)
}

export function getDataSpec(id: string) {
  return apiClient.get<DataSpec>(`${BASE}/${id}`).then((r) => r.data)
}

export function createDataSpec(body: DataSpecUpsert) {
  return apiClient.post<DataSpec>(ADMIN, body).then((r) => r.data)
}

export function updateDataSpec(id: string, body: DataSpecUpsert) {
  return apiClient.put<DataSpec>(`${ADMIN}/${id}`, body).then((r) => r.data)
}

export function deleteDataSpec(id: string, force = false) {
  return apiClient.delete(`${ADMIN}/${id}`, { params: { force } }).then((r) => r.data)
}

export function duplicateDataSpec(id: string, newId: string) {
  return apiClient.post<DataSpec>(`${ADMIN}/${id}/duplicate`, null, { params: { newId } }).then((r) => r.data)
}

export function getUsage(id: string) {
  return apiClient.get<CatalogUsage>(`${ADMIN}/${id}/usage`).then((r) => r.data)
}

export function addField(specId: string, body: FieldSpecUpsert) {
  return apiClient.post<DataSpec>(`${ADMIN}/${specId}/fields`, body).then((r) => r.data)
}

export function updateField(specId: string, fieldId: string, body: FieldSpecUpsert) {
  return apiClient.put<DataSpec>(`${ADMIN}/${specId}/fields/${fieldId}`, body).then((r) => r.data)
}

export function deleteField(specId: string, fieldId: string) {
  return apiClient.delete<DataSpec>(`${ADMIN}/${specId}/fields/${fieldId}`).then((r) => r.data)
}

export function reorderFields(specId: string, orderedFieldIds: string[]) {
  return apiClient
    .put<DataSpec>(`${ADMIN}/${specId}/fields:reorder`, { orderedFieldIds })
    .then((r) => r.data)
}
