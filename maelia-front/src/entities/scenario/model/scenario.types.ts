/** Scénario piloté par le catalogue de paramètres (M8). */
export interface Scenario {
  id: string
  projectId: string
  name: string
  description?: string
  /** Écarts au défaut du catalogue (clé = gamlName). */
  parameterValues: Record<string, unknown>
  createdAt: string
}

export interface ScenarioRequest {
  name: string
  description?: string
  parameterValues: Record<string, unknown>
}

/** Une spécification de paramètre exposée par le catalogue (GET /api/v1/scenario-parameters). */
export type ParamType = 'BOOLEAN' | 'INTEGER' | 'FLOAT' | 'STRING' | 'ENUM' | 'STRING_LIST'

export interface ParameterSpec {
  gamlName: string
  label: string
  group: string
  type: ParamType
  defaultValue: unknown
  unit: string | null
  allowedValues: string[] | null
  visibleIf: string | null
  /** Condition d'activation : champ grisé tant qu'elle est fausse (dépendance entre paramètres). */
  enabledIf: string | null
  /** Id d'un DataSpec dont le dataset projet fournit les valeurs proposées (select issu des données). */
  optionsDataSpec: string | null
  /** Colonne du DataSpec à proposer (null = 1er champ / ID). Ex. ZONE_PEDO pour les sols. */
  optionsColumn: string | null
  /** Source des valeurs : null/COLUMN (défaut), COLUMN_HEADERS, INSTANCE_KEYS. */
  optionsSource: string | null
  advanced: boolean
  order: number
}

export interface ParameterGroup {
  id: string
  label: string
  order: number
  parentId: string | null
}
