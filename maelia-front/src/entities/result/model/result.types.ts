export type ArtifactType = 'IMAGE' | 'CSV' | 'XML' | 'OTHER'

export interface ResultPoint {
  date: string | null
  cycle: number | null
  value: number
}

export interface ResultSeries {
  indicator: string
  zone: string | null
  unit: string | null
  points: ResultPoint[]
}

export interface OutputArtifact {
  id: string
  name: string
  type: ArtifactType
  contentType: string | null
  sizeBytes: number
  url: string
}

export interface ResultSummary {
  runId: string
  indicators: string[]
  series: ResultSeries[]
  yearly: ResultSeries[]
  artifacts: OutputArtifact[]
}

// --- Tableau de bord agronomique (séries indicateur × culture au cours du temps) ---

export interface IndicatorMeta {
  indicator: string
  unit: string | null
  categories: string[]
  years: number[]
}

export interface YearPoint {
  year: number
  /** Moyenne de l'indicateur (ex. rendement t/ha). */
  mean: number
  /** Somme pondérée par la surface (ex. production t) si indicateur surfacique, sinon null. */
  total: number | null
  count: number
}

export interface AggSeries {
  indicator: string
  category: string | null
  unit: string | null
  points: YearPoint[]
}

export interface RunDashboard {
  runId: string
  indicators: IndicatorMeta[]
  series: AggSeries[]
}
