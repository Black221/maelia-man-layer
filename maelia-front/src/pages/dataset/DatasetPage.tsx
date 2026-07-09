import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import {
  Loader2,
  Upload,
  Table2,
  AlertCircle,
  ArrowLeft,
  Lock,
  Sparkles,
  ChevronDown,
  ListTree,
} from 'lucide-react'
import { useState, type ReactNode } from 'react'
import { queryKeys } from '@/shared/api'
import { DatasetStatusBadge } from '@/entities/dataset'
import { DatasetGrid, DatasetUpload, ShpUpload, getDataset, getDataSpec, useReferentialOptions } from '@/features/manage-dataset'

const MODULE_LABELS: Record<string, string> = {
  COMMUN:         'Commun',
  AGRICOLE:       'Agricole',
  HYDROGRAPHIQUE: 'Hydrologique',
  NORMATIF:       'Normatif',
}

export function DatasetPage() {
  const { datasetId, id: projectId } = useParams<{ datasetId: string; id: string }>()
  const [tab, setTab] = useState<'grid' | 'upload'>('grid')
  const [schemaOpen, setSchemaOpen] = useState(false)

  const { data: dataset, isLoading: datasetLoading, isError } = useQuery({
    queryKey: queryKeys.datasets.detail(datasetId!),
    queryFn: () => getDataset(datasetId!),
    enabled: !!datasetId,
  })

  const { data: dataSpec, isLoading: specLoading } = useQuery({
    queryKey: queryKeys.dataspecs.detail(dataset?.dataSpecId ?? ''),
    queryFn: () => getDataSpec(dataset!.dataSpecId),
    enabled: !!dataset?.dataSpecId,
  })

  const isLoading = datasetLoading || (!!dataset && specLoading)

  // Colonnes depuis le FieldSpec (source de vérité) ou inférées des enregistrements existants.
  const fields = dataSpec?.fields?.length
    ? dataSpec.fields.map((f) => ({
        label: f.label,
        infoType: f.infoType,
        unit: f.unit ?? null,
        description: f.description ?? null,
        required: f.required,
        referencesDataSpec: f.referencesDataSpec ?? null,
        allowedValues: f.allowedValues ?? [],
      }))
    : (dataset?.records?.length ?? 0) > 0
      ? Object.keys(dataset!.records[0]).map((label) => ({
          label,
          infoType: 'String' as const,
          unit: null,
          description: null,
          required: false,
          referencesDataSpec: null,
          allowedValues: [] as string[],
        }))
      : []

  const refSpecIds = fields.map((f) => f.referencesDataSpec).filter((v): v is string => !!v)
  const { data: referentialOptions } = useReferentialOptions(dataset?.projectId, refSpecIds)

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-20 text-neutral-400">
        <Loader2 size={18} className="animate-spin" />
        Chargement…
      </div>
    )
  }

  if (isError || !dataset) {
    return (
      <div className="flex items-center gap-2 py-12 text-danger">
        <AlertCircle size={16} />
        Dataset introuvable.
      </div>
    )
  }

  const isAuto = dataSpec?.generation === 'AUTO'
  const fileName = dataSpec?.fileName ?? dataset.dataSpecId
  const fileType = dataSpec?.fileType ?? ''

  const tabs = [
    { id: 'grid',   icon: <Table2 size={14} />, label: 'Saisie en grille' },
    { id: 'upload', icon: <Upload size={14} />, label: 'Import CSV' },
  ] as const

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      {/* Fil d'Ariane */}
      <Link
        to={`/projects/${projectId}/data`}
        className="inline-flex items-center gap-1.5 text-[13px] text-neutral-500 hover:text-primary transition-colors"
      >
        <ArrowLeft size={14} />
        Données du projet
      </Link>

      {/* En-tête fichier */}
      <header className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm space-y-4">
        <div className="flex items-start justify-between gap-4">
          <div className="min-w-0">
            <p className="text-[10px] font-medium uppercase tracking-wider text-neutral-400 mb-1">
              {dataSpec ? (MODULE_LABELS[dataSpec.module] ?? dataSpec.module) : 'Fichier de données'}
            </p>
            <div className="flex items-center gap-2 flex-wrap">
              <h1 className="text-[19px] font-semibold text-neutral-900 font-mono break-all">{fileName}</h1>
              {fileType && (
                <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-neutral-100 text-neutral-500 uppercase">
                  {fileType}
                </span>
              )}
            </div>
            <p className="font-mono text-[11px] text-neutral-400 mt-1 break-all">{dataset.dataSpecId}</p>
          </div>
          <DatasetStatusBadge status={dataset.status} />
        </div>

        {dataSpec?.description && (
          <p className="text-[13px] text-neutral-600 leading-relaxed">{dataSpec.description}</p>
        )}

        {/* Badges indicatifs */}
        <div className="flex items-center gap-2 flex-wrap">
          {dataSpec?.required ? (
            <Pill tone="warning" icon={<Lock size={11} />}>Obligatoire</Pill>
          ) : (
            <Pill tone="neutral">Optionnel</Pill>
          )}
          {isAuto && <Pill tone="primary" icon={<Sparkles size={11} />}>Généré par le prétraitement</Pill>}
          {dataSpec?.saisieMode && <Pill tone="neutral">Saisie : {dataSpec.saisieMode}</Pill>}
          {dataSpec?.multiInstance && <Pill tone="neutral">Multi-fichiers</Pill>}
        </div>

        {/* Mini-stats */}
        <div className="flex items-center gap-6 pt-1 text-[13px]">
          <Stat label="Enregistrements" value={dataset.recordCount} />
          {fields.length > 0 && <Stat label="Colonnes" value={fields.length} />}
        </div>

        {isAuto && (
          <div className="flex items-start gap-2 rounded-lg bg-primary-50 px-3 py-2 text-[12px] text-primary-800">
            <Sparkles size={14} className="mt-0.5 shrink-0" />
            <span>
              Ce fichier est normalement <strong>généré automatiquement par le module de prétraitement</strong>. La saisie
              manuelle ci-dessous permet de le personnaliser si nécessaire.
            </span>
          </div>
        )}
      </header>

      {/* Schéma attendu (repliable) */}
      {fields.length > 0 && (
        <section className="rounded-2xl border border-neutral-200 bg-white overflow-hidden">
          <button
            onClick={() => setSchemaOpen((v) => !v)}
            className="w-full flex items-center gap-2 px-5 py-3 text-[13px] font-medium text-neutral-700 hover:bg-neutral-50 transition-colors"
          >
            <ListTree size={15} className="text-neutral-400" />
            Schéma attendu
            <span className="text-neutral-400 font-normal">· {fields.length} colonne(s)</span>
            <ChevronDown
              size={15}
              className={`ml-auto text-neutral-400 transition-transform ${schemaOpen ? 'rotate-180' : ''}`}
            />
          </button>
          {schemaOpen && (
            <ul className="border-t border-neutral-100 divide-y divide-neutral-50">
              {fields.map((f) => (
                <li key={f.label} className="px-5 py-2.5 flex items-start gap-3">
                  <div className="min-w-0 flex-1">
                    <div className="flex items-center gap-2">
                      <span className="font-mono text-[12px] text-neutral-800">{f.label}</span>
                      {f.required && <span className="text-danger text-xs" title="Champ requis">*</span>}
                    </div>
                    {f.description && (
                      <p className="text-[12px] text-neutral-500 mt-0.5">{f.description}</p>
                    )}
                  </div>
                  <div className="shrink-0 flex items-center gap-1.5 text-[11px] text-neutral-400">
                    <span className="px-1.5 py-0.5 rounded bg-neutral-100 text-neutral-500">{f.infoType}</span>
                    {f.unit && <span>{f.unit}</span>}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </section>
      )}

      {/* SHP (C8) : upload d'archive zip, pas de saisie en grille pour la géométrie */}
      {fileType === 'SHP' && (
        <ShpUpload
          projectId={dataset.projectId}
          dataSpecId={dataset.dataSpecId}
          expectedFileName={fileName}
        />
      )}

      {/* Onglets saisie / import (CSV) */}
      {fileType !== 'SHP' && (<>
      <div>
        <div className="flex gap-1 p-1 rounded-xl bg-neutral-100 w-fit">
          {tabs.map(({ id, icon, label }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={[
                'flex items-center gap-1.5 px-4 py-1.5 text-[13px] font-medium rounded-lg transition-colors',
                tab === id
                  ? 'bg-white text-primary shadow-sm'
                  : 'text-neutral-500 hover:text-neutral-800',
              ].join(' ')}
            >
              {icon}
              {label}
            </button>
          ))}
        </div>
      </div>

      {/* Contenu */}
      {tab === 'grid' &&
        (fields.length === 0 ? (
          <div className="rounded-2xl border border-dashed border-neutral-200 bg-white p-10 text-center space-y-2">
            <p className="text-[14px] font-medium text-neutral-700">Aucune colonne définie</p>
            <p className="text-[13px] text-neutral-500 max-w-md mx-auto">
              La définition des champs (<span className="font-mono text-xs">FieldSpec</span>) de ce fichier
              est en cours de saisie dans le catalogue. Utilisez l&apos;import CSV en attendant.
            </p>
            <button
              onClick={() => setTab('upload')}
              className="inline-flex items-center gap-1.5 text-[13px] text-primary hover:underline mt-1"
            >
              <Upload size={13} /> Importer un CSV
            </button>
          </div>
        ) : (
          <DatasetGrid
            datasetId={dataset.id}
            projectId={dataset.projectId}
            fields={fields}
            initialRecords={dataset.records}
            referentialOptions={referentialOptions ?? {}}
            orientation={dataSpec?.orientation ?? 'FIELDS_AS_COLUMNS'}
          />
        ))}

      {tab === 'upload' && (
        <DatasetUpload
          projectId={dataset.projectId}
          dataSpecId={dataset.dataSpecId}
          csvFormat={dataSpec?.csvFormat ?? null}
          orientation={dataSpec?.orientation ?? 'FIELDS_AS_COLUMNS'}
          delimiter={dataSpec?.delimiter ?? ';'}
          onImported={() => setTab('grid')}
        />
      )}
      </>)}
    </div>
  )
}

function Stat({ label, value }: { label: string; value: number }) {
  return (
    <div>
      <span className="text-[17px] font-semibold text-neutral-900 tabular-nums">{value}</span>
      <span className="text-neutral-400 ml-1.5">{label}</span>
    </div>
  )
}

type Tone = 'neutral' | 'warning' | 'primary'

const TONE_STYLES: Record<Tone, string> = {
  neutral: 'bg-neutral-100 text-neutral-600',
  warning: 'bg-warning/10 text-warning',
  primary: 'bg-primary-50 text-primary-700',
}

function Pill({ tone, icon, children }: { tone: Tone; icon?: ReactNode; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full ${TONE_STYLES[tone]}`}
    >
      {icon}
      {children}
    </span>
  )
}
