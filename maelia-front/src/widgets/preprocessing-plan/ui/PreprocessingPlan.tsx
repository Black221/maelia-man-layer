import { useMemo, useState } from 'react'
import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import {
  AlertTriangle,
  CheckCircle2,
  CircleDashed,
  CircleDot,
  Loader2,
  Pencil,
  Sparkles,
} from 'lucide-react'
import { getGenerationPlan } from '@/features/preprocessing'
import { getOrCreateDataset } from '@/features/manage-dataset'
import { queryKeys } from '@/shared/api'
import type { GenerationPlanEntry, PlanStatus } from '@/entities/preprocessing'
import { DependencyGraphView } from './DependencyGraphView'

interface PreprocessingPlanProps {
  projectId: string
}

type PlanTab = 'GENERES' | 'BLOQUES' | 'PRETS' | 'TOUS' | 'GRAPHE'
type PlanFilter = Exclude<PlanTab, 'GRAPHE'>

// Vue par défaut : les fichiers produits par le module de prétraitement (AUTO).
// Onglets dans le même style que les onglets modules de la page « Données ».
const TABS: { id: PlanTab; label: string }[] = [
  { id: 'GENERES', label: 'Générés par le prétraitement' },
  { id: 'BLOQUES', label: 'Bloqués' },
  { id: 'PRETS',   label: 'Prêts à générer' },
  { id: 'TOUS',    label: 'Tous' },
  { id: 'GRAPHE',  label: 'Graphe' },
]

function matchesFilter(e: GenerationPlanEntry, filter: PlanFilter) {
  switch (filter) {
    case 'GENERES': return e.generation === 'AUTO'
    case 'BLOQUES': return e.status === 'BLOCKED'
    case 'PRETS':   return e.status === 'READY' && !e.datasetExists
    default:        return true
  }
}

const STATUS_META: Record<PlanStatus, { label: string; className: string }> = {
  DONE:    { label: 'Présent',  className: 'bg-success/10 text-success' },
  READY:   { label: 'Prêt',     className: 'bg-primary-50 text-primary-700' },
  BLOCKED: { label: 'Bloqué',   className: 'bg-warning/10 text-warning' },
}

export function PreprocessingPlan({ projectId }: PreprocessingPlanProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [tab, setTab] = useState<PlanTab>('GENERES')

  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.preprocessing.plan(projectId),
    queryFn: () => getGenerationPlan(projectId),
  })

  const openDataset = useMutation({
    mutationFn: (dataSpecId: string) => getOrCreateDataset(projectId, dataSpecId),
    onSuccess: (dataset) => {
      queryClient.setQueryData(queryKeys.datasets.detail(dataset.id), dataset)
      navigate(`/projects/${projectId}/data/${dataset.id}`)
    },
  })

  // dataSpecId → nom de fichier, pour afficher les dépendances lisiblement.
  const fileNameById = useMemo(() => {
    const map: Record<string, string> = {}
    for (const e of data ?? []) map[e.dataSpecId] = e.fileName
    return map
  }, [data])

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-500 py-6">
        <Loader2 size={14} className="animate-spin" />
        Calcul du plan de prétraitement…
      </div>
    )
  }

  if (isError || !data) {
    return <p className="text-sm text-danger">Impossible de charger le plan de prétraitement.</p>
  }

  const filtered = tab === 'GRAPHE' ? [] : data.filter((e) => matchesFilter(e, tab))
  const cycleEntries = data.filter((e) => e.level === -1)

  // Regroupement par niveau topologique (ordre de génération).
  const byLevel = filtered.reduce<Record<number, GenerationPlanEntry[]>>((acc, e) => {
    ;(acc[e.level] ??= []).push(e)
    return acc
  }, {})
  const levels = Object.keys(byLevel)
    .map(Number)
    .sort((a, b) => (a === -1 ? 1 : b === -1 ? -1 : a - b))

  const done = data.filter((e) => e.status === 'DONE').length
  const ready = data.filter((e) => e.status === 'READY').length
  const blocked = data.filter((e) => e.status === 'BLOCKED').length

  return (
    <div className="space-y-5">
      {/* Récapitulatif + alerte cycle : propres au plan du projet, masqués sur l'onglet Graphe */}
      {tab !== 'GRAPHE' && (
        <>
          <div className="rounded-xl border border-neutral-200 bg-white p-4">
            <div className="flex items-center gap-6 text-sm">
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <CheckCircle2 size={15} className="text-success" />
                <strong>{done}</strong> présents
              </span>
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <CircleDot size={15} className="text-primary-500" />
                <strong>{ready}</strong> prêts (dépendances satisfaites)
              </span>
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <AlertTriangle size={15} className="text-warning" />
                <strong>{blocked}</strong> bloqués
              </span>
            </div>
            <p className="text-xs text-neutral-400 mt-2">
              Les fichiers sont ordonnés par niveau : un fichier ne peut être généré que lorsque
              toutes ses dépendances (niveaux précédents) sont présentes.
            </p>
          </div>

          {cycleEntries.length > 0 && (
            <div className="flex items-start gap-2 rounded-lg bg-warning/10 px-3 py-2 text-[12px] text-warning">
              <AlertTriangle size={14} className="mt-0.5 shrink-0" />
              <span>
                Dépendances circulaires détectées pour :{' '}
                {cycleEntries.map((e) => e.fileName).join(', ')}. Vérifiez le catalogue.
              </span>
            </div>
          )}
        </>
      )}

      {/* Onglets */}
      <div className="border-b border-neutral-200">
        <div className="flex gap-0 overflow-x-auto">
          {TABS.map(({ id, label }) => (
            <button
              key={id}
              onClick={() => setTab(id)}
              className={[
                'px-4 py-2.5 text-[13px] font-medium border-b-2 whitespace-nowrap transition-colors',
                tab === id
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-800 hover:border-neutral-300',
              ].join(' ')}
            >
              {label}
            </button>
          ))}
        </div>
      </div>

      {tab === 'GRAPHE' ? (
        <DependencyGraphView />
      ) : levels.length === 0 ? (
        <div className="rounded-xl border border-dashed border-neutral-200 py-10 text-center text-sm text-neutral-400">
          Aucun fichier pour ce filtre — essayez « Tous ».
        </div>
      ) : (
        levels.map((level) => (
          <section key={level}>
            <h3 className="text-[11px] font-semibold uppercase tracking-wider text-neutral-400 mb-2">
              {level === -1
                ? 'Cycle (ordre indéterminé)'
                : level === 0
                  ? 'Niveau 0 — racines (aucune dépendance)'
                  : `Niveau ${level}`}
            </h3>
            <ul className="rounded-xl border border-neutral-200 overflow-hidden bg-white divide-y divide-neutral-100">
              {byLevel[level].map((entry) => (
                <PlanRow
                  key={entry.dataSpecId}
                  entry={entry}
                  fileNameById={fileNameById}
                  onOpen={() => openDataset.mutate(entry.dataSpecId)}
                  opening={openDataset.isPending && openDataset.variables === entry.dataSpecId}
                />
              ))}
            </ul>
          </section>
        ))
      )}
    </div>
  )
}

interface PlanRowProps {
  entry: GenerationPlanEntry
  fileNameById: Record<string, string>
  onOpen: () => void
  opening: boolean
}

function PlanRow({ entry, fileNameById, onOpen, opening }: PlanRowProps) {
  const meta = STATUS_META[entry.status]
  const isAuto = entry.generation === 'AUTO'

  return (
    <li className="flex items-center gap-3 px-4 py-3 hover:bg-neutral-50 transition-colors">
      {/* Statut */}
      <span className="shrink-0">
        {entry.status === 'DONE' ? (
          <CheckCircle2 size={18} className="text-success" />
        ) : entry.status === 'READY' ? (
          <CircleDot size={18} className="text-primary-400" />
        ) : (
          <CircleDashed size={18} className="text-warning/70" />
        )}
      </span>

      {/* Fichier + dépendances */}
      <div className="min-w-0 flex-1">
        <div className="flex items-center gap-2 flex-wrap">
          <span className="font-mono text-[13px] text-neutral-800 truncate">{entry.fileName}</span>
          <span
            className={`inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded-full ${meta.className}`}
          >
            {meta.label}
          </span>
          {isAuto ? (
            <span className="inline-flex items-center gap-1 text-[10px] font-medium px-1.5 py-0.5 rounded-full bg-primary-50 text-primary-700">
              <Sparkles size={10} />
              Généré par le prétraitement
            </span>
          ) : (
            <span className="text-[10px] font-medium px-1.5 py-0.5 rounded-full bg-neutral-100 text-neutral-500">
              À fournir
            </span>
          )}
        </div>

        {entry.dependencies.length > 0 && (
          <p className="text-xs text-neutral-500 mt-1 flex items-center gap-1 flex-wrap">
            <span className="text-neutral-400">Dépend de :</span>
            {entry.dependencies.map((dep) => {
              const missing = entry.missingDependencies.includes(dep)
              return (
                <span
                  key={dep}
                  title={missing ? `${dep} — dépendance manquante` : dep}
                  className={[
                    'font-mono text-[11px] px-1.5 py-0.5 rounded',
                    missing
                      ? 'bg-warning/10 text-warning'
                      : 'bg-neutral-100 text-neutral-500',
                  ].join(' ')}
                >
                  {fileNameById[dep] ?? dep}
                </span>
              )
            })}
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
          {opening ? <Loader2 size={12} className="animate-spin" /> : <Pencil size={11} />}
          {entry.datasetExists ? 'Modifier' : isAuto ? 'Personnaliser' : 'Saisir'}
        </button>
      </div>
    </li>
  )
}
