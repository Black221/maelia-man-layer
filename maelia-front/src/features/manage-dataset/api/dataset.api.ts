import { apiClient } from '@/shared/api'
import type { Dataset, DataSpec, ValidationReportDto } from '@/entities/dataset'

export function listProjectDatasets(projectId: string) {
  return apiClient.get<Dataset[]>(`/api/v1/projects/${projectId}/datasets`).then((r) => r.data)
}

export function getOrCreateDataset(projectId: string, dataSpecId: string) {
  return apiClient
    .post<Dataset>(`/api/v1/projects/${projectId}/datasets/${encodeURIComponent(dataSpecId)}`)
    .then((r) => r.data)
}

export function getDataset(datasetId: string) {
  return apiClient.get<Dataset>(`/api/v1/datasets/${datasetId}`).then((r) => r.data)
}

export function upsertRecords(datasetId: string, records: Record<string, unknown>[]) {
  return apiClient
    .put<Dataset>(`/api/v1/datasets/${datasetId}/records`, records)
    .then((r) => r.data)
}

export function importCsv(projectId: string, dataSpecId: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiClient
    .post<Dataset>(`/api/v1/projects/${projectId}/datasets/${encodeURIComponent(dataSpecId)}/import`, form)
    .then((r) => r.data)
}

export interface DatasetFileDto {
  fileName: string
  sizeBytes: number
  uploadedAt: string
}

/** C8 — upload d'un shapefile : archive .zip (.shp + .shx + .dbf, + .prj…). */
export function uploadShp(projectId: string, dataSpecId: string, file: File) {
  const form = new FormData()
  form.append('file', file)
  return apiClient
    .post<DatasetFileDto[]>(`/api/v1/projects/${projectId}/datasets/${encodeURIComponent(dataSpecId)}/shp`, form)
    .then((r) => r.data)
}

/** Fichiers uploadés pour ce DataSpec — vide si le socle est utilisé. */
export function listDatasetFiles(projectId: string, dataSpecId: string) {
  return apiClient
    .get<DatasetFileDto[]>(`/api/v1/projects/${projectId}/datasets/${encodeURIComponent(dataSpecId)}/files`)
    .then((r) => r.data)
}

export function validateDataset(datasetId: string) {
  return apiClient
    .post<ValidationReportDto>(`/api/v1/datasets/${datasetId}/validate`)
    .then((r) => r.data)
}

export function materializeIncludes(projectId: string) {
  return apiClient.post<{ path: string }>(`/api/v1/projects/${projectId}/materialize`).then((r) => r.data)
}

export function getDataSpec(dataSpecId: string) {
  return apiClient
    .get<DataSpec>(`/api/v1/dataspecs/${encodeURIComponent(dataSpecId)}`)
    .then((r) => r.data)
}

/**
 * Valeurs proposées pour un sélecteur de paramètre : colonne d'un DataSpec, lue dans les données
 * saisies du projet sinon dans le socle (opérationnel même sans upload ; supporte CSV et SHP/DBF).
 * `column` omis = 1er champ (ID) ; `source` = COLUMN (défaut) | COLUMN_HEADERS | INSTANCE_KEYS.
 */
export function getReferentialOptions(
  projectId: string,
  dataSpec: string,
  column?: string | null,
  source?: string | null,
) {
  return apiClient
    .get<string[]>(`/api/v1/projects/${projectId}/referential-options`, {
      params: { dataSpec, column: column || undefined, source: source || undefined },
    })
    .then((r) => r.data)
}
