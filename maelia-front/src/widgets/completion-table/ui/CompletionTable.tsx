import { useMemo, useState, type ReactNode } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  CheckCircle2,
  Circle,
  Loader2,
  Pencil,
  Search,
  Sparkles,
  Lock,
  X,
} from 'lucide-react'
import { getProjectCompletion } from '@/features/configure-project'
import { getOrCreateDataset } from '@/features/manage-dataset'
import { queryKeys } from '@/shared/api'
import type { CompletionEntry } from '@/entities/project'

interface CompletionTableProps {
  projectId: string
}

// Le backend expose 4 modules métier. On les présente comme onglets dans cet ordre.
const MODULE_LABELS: Record<string, string> = {
  COMMUN:        'Commun',
  AGRICOLE:      'Agricole',
  HYDROGRAPHIQUE:'Hydrologique',
  NORMATIF:      'Normatif',
}
const MODULE_ORDER = ['COMMUN', 'AGRICOLE', 'HYDROGRAPHIQUE', 'NORMATIF']

type QuickFilter = 'A_SAISIR' | 'A_FOURNIR' | 'GENERES' | 'TOUS'

// Vue par défaut : uniquement les fichiers obligatoires à saisir manuellement.
// Les autres vues (tout le catalogue, restants à fournir, générés) sont accessibles par filtre.
const FILTERS: { id: QuickFilter; label: string }[] = [
  { id: 'A_SAISIR',  label: 'Obligatoires à saisir' },
  { id: 'A_FOURNIR', label: 'Restants à fournir' },
  { id: 'GENERES',   label: 'Générés par le prétraitement' },
  { id: 'TOUS',      label: 'Tous' },
]

/** Un fichier est « à fournir » par l'utilisateur s'il est saisi manuellement et pas encore renseigné. */
function isToProvide(e: CompletionEntry) {
  return e.generation === 'MANUAL' && !e.datasetExists
}

function matchesFilter(e: CompletionEntry, filter: QuickFilter) {
  switch (filter) {
    case 'A_SAISIR':  return e.required && e.generation === 'MANUAL'
    case 'A_FOURNIR': return isToProvide(e)
    case 'GENERES':   return e.generation === 'AUTO'
    default:          return true
  }
}

function matchesSearch(e: CompletionEntry, q: string) {
  if (!q) return true
  const needle = q.toLowerCase()
  return (
    e.fileName.toLowerCase().includes(needle) ||
    e.dataSpecId.toLowerCase().includes(needle) ||
    (e.description?.toLowerCase().includes(needle) ?? false)
  )
}

export function CompletionTable({ projectId }: CompletionTableProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [activeModule, setActiveModule] = useState<string | null>(null)
  const [search, setSearch] = useState('')
  const [filter, setFilter] = useState<QuickFilter>('A_SAISIR')

  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.projects.completion(projectId),
    queryFn: () => getProjectCompletion(projectId),
  })

  const openDataset = useMutation({
    mutationFn: (dataSpecId: string) => getOrCreateDataset(projectId, dataSpecId),
    onSuccess: (dataset) => {
      queryClient.setQueryData(queryKeys.datasets.detail(dataset.id), dataset)
      navigate(`/projects/${projectId}/data/${dataset.id}`)
    },
  })

  const byModule = useMemo(() => {
    return (data ?? []).reduce<Record<string, CompletionEntry[]>>((acc, entry) => {
      const mod = entry.module ?? 'COMMUN'
      ;(acc[mod] ??= []).push(entry)
      return acc
    }, {})
  }, [data])

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-500 py-6">
        <Loader2 size={14} className="animate-spin" />
        Chargement du catalogue…
      </div>
    )
  }

  if (isError || !data) {
    return <p className="text-sm text-danger">Impossible de charger le tableau de complétude.</p>
  }

  const modules = MODULE_ORDER.filter((m) => byModule[m]?.length > 0)
  const current = activeModule && byModule[activeModule] ? activeModule : (modules[0] ?? null)
  const moduleEntries = current ? (byModule[current] ?? []) : []

  const entries = moduleEntries
    .filter((e) => matchesFilter(e, filter))
    .filter((e) => matchesSearch(e, search))

  // Progression : ne compte que les fichiers que l'utilisateur doit fournir (saisie manuelle).
  const manual = data.filter((e) => e.generation === 'MANUAL')
  const manualDone = manual.filter((e) => e.datasetExists).length
  const autoCount = data.filter((e) => e.generation === 'AUTO').length
  const pct = manual.length ? Math.round((manualDone / manual.length) * 100) : 100

  return (
    <div className="space-y-5">
      {/* Récapitulatif + progression */}
      <div className="rounded-xl border border-neutral-200 bg-white p-4 space-y-3">
        <div className="flex items-baseline justify-between gap-3">
          <p className="text-sm font-medium text-neutral-800">
            {manualDone}/{manual.length}{' '}
            <span className="font-normal text-neutral-500">fichiers à fournir renseignés</span>
          </p>
          <span className="text-xs text-neutral-400 inline-flex items-center gap-1">
            <Sparkles size={12} className="text-primary-500" />
            {autoCount} générés par le prétraitement
          </span>
        </div>
        <div className="h-2 rounded-full bg-neutral-100 overflow-hidden">
          <div
            className="h-full rounded-full bg-primary transition-all duration-300"
            style={{ width: `${pct}%` }}
          />
        </div>
      </div>

      {/* Onglets modules */}
      <div className="border-b border-neutral-200">
        <div className="flex gap-0 overflow-x-auto">
          {modules.map((mod) => {
            const modEntries = byModule[mod] ?? []
            const modManual = modEntries.filter((e) => e.generation === 'MANUAL')
            const modDone = modManual.filter((e) => e.datasetExists).length
            const complete = modManual.length > 0 && modDone === modManual.length
            const isActive = mod === current

            return (
              <button
                key={mod}
                onClick={() => setActiveModule(mod)}
                className={[
                  'flex items-center gap-2 px-4 py-2.5 text-[13px] font-medium border-b-2 whitespace-nowrap transition-colors',
                  isActive
                    ? 'border-primary text-primary'
                    : 'border-transparent text-neutral-500 hover:text-neutral-800 hover:border-neutral-300',
                ].join(' ')}
              >
                {MODULE_LABELS[mod] ?? mod}
                <span
                  className={[
                    'text-[11px] px-1.5 py-0.5 rounded-full font-semibold tabular-nums',
                    complete
                      ? 'bg-success/10 text-success'
                      : isActive
                        ? 'bg-primary/10 text-primary'
                        : 'bg-neutral-100 text-neutral-400',
                  ].join(' ')}
                >
                  {modManual.length ? `${modDone}/${modManual.length}` : modEntries.length}
                </span>
              </button>
            )
          })}
        </div>
      </div>

      {/* Recherche + filtres rapides */}
      <div className="flex flex-col sm:flex-row sm:items-center gap-3">
        <div className="relative flex-1">
          <Search size={15} className="absolute left-3 top-1/2 -translate-y-1/2 text-neutral-400" />
          <input
            type="text"
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            placeholder="Rechercher un fichier…"
            className="w-full pl-9 pr-9 py-2 text-sm rounded-lg border border-neutral-200 bg-white
                       placeholder:text-neutral-400 focus:outline-none focus:ring-2 focus:ring-primary-300
                       focus:border-primary-400 transition"
          />
          {search && (
            <button
              onClick={() => setSearch('')}
              className="absolute right-2.5 top-1/2 -translate-y-1/2 text-neutral-400 hover:text-neutral-700"
              aria-label="Effacer la recherche"
            >
              <X size={14} />
            </button>
          )}
        </div>
        <div className="flex gap-1.5 overflow-x-auto">
          {FILTERS.map((f) => (
            <button
              key={f.id}
              onClick={() => setFilter(f.id)}
              className={[
                'px-3 py-1.5 text-xs font-medium rounded-full whitespace-nowrap transition-colors',
                filter === f.id
                  ? 'bg-primary text-white'
                  : 'bg-neutral-100 text-neutral-600 hover:bg-neutral-200',
              ].join(' ')}
            >
              {f.label}
            </button>
          ))}
        </div>
      </div>

      {/* Liste des fichiers */}
      {entries.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-200 py-10 text-center text-sm text-neutral-400">
          {search
            ? 'Aucun fichier ne correspond à votre recherche.'
            : 'Aucun fichier pour ce filtre dans ce module — essayez « Tous ».'}
        </div>
      ) : (
        <ul className="rounded-xl border border-neutral-200 overflow-hidden bg-white divide-y divide-neutral-100">
          {entries.map((entry) => (
            <FileRow
              key={entry.dataSpecId}
              entry={entry}
              onOpen={() => openDataset.mutate(entry.dataSpecId)}
              opening={openDataset.isPending && openDataset.variables === entry.dataSpecId}
            />
          ))}
        </ul>
      )}
    </div>
  )
}

interface FileRowProps {
  entry: CompletionEntry
  onOpen: () => void
  opening: boolean
}

function FileRow({ entry, onOpen, opening }: FileRowProps) {
  const isAuto = entry.generation === 'AUTO'

  return (
    <li className="flex items-center gap-3 px-4 py-3 hover:bg-neutral-50 transition-colors">
      {/* Statut */}
      <span className="shrink-0">
        {entry.datasetExists ? (
          <CheckCircle2 size={18} className="text-success" />
        ) : isAuto ? (
          <Sparkles size={18} className="text-primary-400" />
        ) : (
          <Circle size={18} className={entry.required ? 'text-warning/60' : 'text-neutral-300'} />
        )}
      </span>

      {/* Nom + description + badges */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-mono text-[13px] text-neutral-800 truncate">{entry.fileName}</span>
          <span className="text-[10px] font-semibold px-1.5 py-0.5 rounded bg-neutral-100 text-neutral-500 uppercase">
            {entry.fileType}
          </span>
          {entry.required ? (
            <Badge tone="warning" icon={<Lock size={10} />}>Obligatoire</Badge>
          ) : (
            <Badge tone="neutral">Optionnel</Badge>
          )}
          {isAuto && (
            <Badge tone="primary" icon={<Sparkles size={10} />}>Généré par le prétraitement</Badge>
          )}
        </div>
        {entry.description && (
          <p className="text-xs text-neutral-500 mt-0.5 truncate" title={entry.description}>
            {entry.description}
          </p>
        )}
      </div>

      {/* Action */}
      <div className="shrink-0">
        <button
          onClick={onOpen}
          disabled={opening}
          className={[
            'inline-flex items-center gap-1 text-xs font-medium rounded-lg px-2.5 py-1.5 transition-colors disabled:opacity-50',
            entry.datasetExists
              ? 'text-neutral-600 hover:bg-neutral-100'
              : 'text-primary hover:bg-primary-50',
          ].join(' ')}
        >
          {opening ? (
            <Loader2 size={12} className="animate-spin" />
          ) : (
            <Pencil size={11} />
          )}
          {entry.datasetExists ? 'Modifier' : isAuto ? 'Personnaliser' : 'Saisir'}
        </button>
      </div>
    </li>
  )
}

type Tone = 'neutral' | 'warning' | 'primary'

const TONE_STYLES: Record<Tone, string> = {
  neutral: 'bg-neutral-100 text-neutral-500',
  warning: 'bg-warning/10 text-warning',
  primary: 'bg-primary-50 text-primary-700',
}

function Badge({ tone, icon, children }: { tone: Tone; icon?: ReactNode; children: ReactNode }) {
  return (
    <span
      className={`inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded-full ${TONE_STYLES[tone]}`}
    >
      {icon}
      {children}
    </span>
  )
}
