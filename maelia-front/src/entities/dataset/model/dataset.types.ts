export type DatasetStatus = 'VIDE' | 'EN_COURS' | 'VALIDE' | 'INVALIDE'

export type InfoType = 'String' | 'Integer' | 'Double' | 'Date' | 'Boolean'

export interface FieldSpec {
  id: string
  label: string
  position: number | null
  infoType: InfoType
  unit: string | null
  required: boolean
  description: string | null
  referencesDataSpec: string | null
  listSeparator: string | null
  allowedValues: string[]
}

export type Generation = 'AUTO' | 'MANUAL'

/** FIELDS_AS_COLUMNS = standard (1 ligne = 1 enregistrement) ; FIELDS_AS_ROWS = transposé. */
export type Orientation = 'FIELDS_AS_COLUMNS' | 'FIELDS_AS_ROWS'

export interface DataSpec {
  id: string
  module: string
  folder: string | null
  fileName: string
  fileType: string
  csvFormat: string | null
  orientation: Orientation
  matrixValueStartIndex: number | null
  delimiter: string | null
  generation: Generation
  required: boolean
  requiredIf: string | null
  temporalResolution: string | null
  multiInstance: boolean
  instancePattern?: string | null
  /** Regex (match complet, insensible à la casse) reconnaissant les instances d'un type multi-instance (ex. \d{4}\.csv pour AAAA.csv). */
  fileNamePattern?: string | null
  saisieMode: string
  description: string | null
  fieldsStatus: string
  origin?: string
  // Le backend sérialise les champs sous la clé `fields` (cf. DataSpecDto).
  fields: FieldSpec[]
}

export interface Dataset {
  id: string
  projectId: string
  dataSpecId: string
  /** Nom de fichier de l'instance (types multi-instance, ex. "2018.csv") ; null = dataset unique du type. */
  instanceKey?: string | null
  status: DatasetStatus
  recordCount: number
  records: Record<string, unknown>[]
  createdAt: string
  updatedAt: string
}

/** Rapport de l'initialisation en masse (upload ZIP). */
export interface BulkImportEntryReport {
  entryName: string
  dataSpecId: string | null
  fileType: string | null
  status: 'VALIDE' | 'INVALIDE' | 'IGNORE' | 'ERREUR'
  message: string
  recordCount: number
}

export interface BulkImportReport {
  totalEntries: number
  imported: number
  invalid: number
  ignored: number
  errors: number
  entries: BulkImportEntryReport[]
}

export interface ValidationIssueDto {
  field: string | null
  rowIndex: number | null
  severity: 'ERROR' | 'WARNING'
  message: string
}

export interface ValidationReportDto {
  valid: boolean
  recordCount: number
  errorCount: number
  issues: ValidationIssueDto[]
}
