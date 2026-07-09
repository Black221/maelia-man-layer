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
