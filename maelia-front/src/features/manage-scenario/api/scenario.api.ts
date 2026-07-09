import { apiClient } from '@/shared/api'
import type {
  Scenario,
  ScenarioRequest,
  ParameterSpec,
  ParameterGroup,
} from '@/entities/scenario'
import type { SimulationRun } from '@/entities/run'

export function listScenarios(projectId: string): Promise<Scenario[]> {
  return apiClient.get<Scenario[]>(`/api/v1/projects/${projectId}/scenarios`).then((r) => r.data)
}

export function createScenario(projectId: string, data: ScenarioRequest): Promise<Scenario> {
  return apiClient.post<Scenario>(`/api/v1/projects/${projectId}/scenarios`, data).then((r) => r.data)
}

export function getScenario(projectId: string, scenarioId: string): Promise<Scenario> {
  return apiClient
    .get<Scenario>(`/api/v1/projects/${projectId}/scenarios/${scenarioId}`)
    .then((r) => r.data)
}

export function updateScenario(
  projectId: string,
  scenarioId: string,
  data: ScenarioRequest,
): Promise<Scenario> {
  return apiClient
    .put<Scenario>(`/api/v1/projects/${projectId}/scenarios/${scenarioId}`, data)
    .then((r) => r.data)
}

export function deleteScenario(projectId: string, scenarioId: string): Promise<void> {
  return apiClient
    .delete(`/api/v1/projects/${projectId}/scenarios/${scenarioId}`)
    .then(() => undefined)
}

export function launchRunForScenario(projectId: string, scenarioId: string): Promise<SimulationRun> {
  return apiClient
    .post<SimulationRun>(`/api/v1/projects/${projectId}/runs?scenarioId=${scenarioId}`)
    .then((r) => r.data)
}

export function listRunsForProject(projectId: string): Promise<SimulationRun[]> {
  return apiClient.get<SimulationRun[]>(`/api/v1/projects/${projectId}/runs`).then((r) => r.data)
}

// --- Catalogue de paramètres de simulation (M7/M8) ---

export function getScenarioParameters(): Promise<ParameterSpec[]> {
  return apiClient.get<ParameterSpec[]>('/api/v1/scenario-parameters').then((r) => r.data)
}

export function getScenarioParameterGroups(): Promise<ParameterGroup[]> {
  return apiClient.get<ParameterGroup[]>('/api/v1/scenario-parameters/groups').then((r) => r.data)
}
