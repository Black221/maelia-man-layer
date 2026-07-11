export type RunStatus =
  | 'EN_FILE'
  | 'EN_COURS'
  | 'TERMINE'
  | 'ECHEC'
  | 'ANNULE'

export interface SimulationRun {
  id: string
  status: RunStatus
  modelPath: string
  experimentName: string
  projectId?: string | null
  scenarioId?: string | null
  scenarioName?: string | null
  createdAt: string
  startedAt: string | null
  finishedAt: string | null
  finalCycle: number
  errorMessage: string | null
}

/** Message envoyé par le backend via STOMP sur /topic/runs/{id} */
export interface StompRunUpdate {
  type: 'PROGRESS' | 'LOG' | 'ENDED' | 'ERROR' | 'EN_COURS' | 'TERMINE' | 'ECHEC'
  cycle: number
  message: string | null
  error: string | null
}
