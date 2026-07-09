export type ProjectStatus = 'ACTIF' | 'ARCHIVE'

export interface ModelingConfiguration {
  assolementMethod: 'DONNEES_ENTREE' | 'FONCTIONS_DE_CROYANCE'
  irrigationMode: 'BLOC' | 'SIMPLE'
  cropModel: 'AQYIELD' | 'HERBSIM' | 'SIMPLE'
  restrictionMethod: 'SIMPLE' | 'COMPLEXE'
  modules: string[]
  scenarioClimatique: string | null
}

export interface Project {
  id: string
  name: string
  description: string | null
  studyArea: string
  modelingConfiguration: ModelingConfiguration
  status: ProjectStatus
  createdAt: string
  updatedAt: string
}

export type Generation = 'AUTO' | 'MANUAL'

export interface CompletionEntry {
  dataSpecId: string
  module: string
  fileName: string
  fileType: string
  saisieMode: string
  generation: Generation
  required: boolean
  description: string | null
  datasetExists: boolean
  datasetStatus: string | null
}
