import type { Generation } from '@/entities/project'

/** Statut d'un fichier dans le plan de prétraitement du projet. */
export type PlanStatus = 'DONE' | 'READY' | 'BLOCKED'

/**
 * Entrée du plan de prétraitement : un fichier applicable du catalogue,
 * son niveau topologique (0 = racine, -1 = cycle) et l'état de ses dépendances.
 */
export interface GenerationPlanEntry {
  dataSpecId: string
  module: string
  fileName: string
  generation: Generation
  level: number
  dependencies: string[]
  missingDependencies: string[]
  datasetExists: boolean
  status: PlanStatus
}

/** EXPLICIT = référence par identifiant (colonne FK) ; IMPLICIT = par construction. */
export type DependencyKind = 'EXPLICIT' | 'IMPLICIT'

export interface DependencyEdge {
  sourceId: string
  targetId: string
  viaField: string | null
  kind: DependencyKind
}

export interface DependencyNode {
  dataSpecId: string
  module: string
  fileName: string
  fileType: string
  generation: Generation
  level: number
  dependsOn: string[]
  requiredBy: string[]
}

export interface DependencyGraph {
  nodes: DependencyNode[]
  edges: DependencyEdge[]
  hasCycle: boolean
  cycleIds: string[]
  unknownReferences: string[]
}
