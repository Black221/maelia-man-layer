import { apiClient } from '@/shared/api'
import type { SimulationRun } from '@/entities/run'

/** Valeurs de paramètres d'expérience GAMA (clé = nom de la variable GAML). */
export type GamaParameterValues = Record<string, number | string | boolean>

export interface LaunchRunRequest {
  modelPath: string
  experimentName: string
  until?: string
  parameters?: GamaParameterValues
}

export function launchRun(req?: LaunchRunRequest) {
  return apiClient
    .post<SimulationRun>('/api/v1/dev/runs', req ?? undefined)
    .then((r) => r.data)
}

/** Lance le modèle de test autonome (indépendant de MAELIA) — valide la communication. */
export function launchTestRun(parameters?: GamaParameterValues) {
  return apiClient
    .post<SimulationRun>('/api/v1/dev/runs/test', parameters ? { parameters } : undefined)
    .then((r) => r.data)
}

/** Lance le VRAI modèle MAELIA (launcherTest.gaml + includes de base SASSEME). */
export function launchMaeliaTestRun(parameters?: GamaParameterValues) {
  return apiClient
    .post<SimulationRun>('/api/v1/dev/runs/maelia-test', parameters ? { parameters } : undefined)
    .then((r) => r.data)
}

/** Statut d'un run (dev ou projet) — utilise l'endpoint unifié. */
export function getRunStatus(runId: string) {
  return apiClient
    .get<SimulationRun>(`/api/v1/runs/${runId}`)
    .then((r) => r.data)
}
