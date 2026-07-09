import { useMemo } from 'react'
import { useQuery } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Loader2, BarChart3, Eye, Activity, History } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import { RunStatusBadge } from '@/entities/run'
import type { SimulationRun } from '@/entities/run'
import { listRunsForProject, listScenarios } from '../api/scenario.api'

interface RunHistoryListProps {
  projectId: string
}

function fmtDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

export function RunHistoryList({ projectId }: RunHistoryListProps) {
  const navigate = useNavigate()

  const { data: runs, isLoading } = useQuery({
    queryKey: queryKeys.runs.all(projectId),
    queryFn: () => listRunsForProject(projectId),
    refetchInterval: 5000, // rafraîchit tant que des runs sont en cours
  })

  const { data: scenarios } = useQuery({
    queryKey: queryKeys.scenarios.all(projectId),
    queryFn: () => listScenarios(projectId),
  })

  const scenarioName = useMemo(() => {
    const m = new Map<string, string>()
    ;(scenarios ?? []).forEach((s) => m.set(s.id, s.name))
    return m
  }, [scenarios])

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-400 py-4">
        <Loader2 size={14} className="animate-spin" /> Chargement des simulations…
      </div>
    )
  }

  if (!runs || runs.length === 0) {
    return (
      <div className="rounded-2xl border border-dashed border-neutral-200 bg-white py-12 text-center space-y-3">
        <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-neutral-100 text-neutral-400">
          <History size={22} />
        </div>
        <div>
          <p className="text-[14px] font-medium text-neutral-700">Aucune simulation</p>
          <p className="text-[13px] text-neutral-500 mt-0.5">
            Lancez un scénario depuis l’onglet « Scénarios » pour voir apparaître les runs ici.
          </p>
        </div>
      </div>
    )
  }

  // Plus récents d'abord
  const sorted = [...runs].sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1))

  return (
    <div className="rounded-2xl border border-neutral-200 overflow-hidden bg-white">
      <table className="w-full text-sm">
        <thead>
          <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-500">
            <th className="text-left px-4 py-2.5 font-medium">Scénario</th>
            <th className="text-left px-4 py-2.5 font-medium hidden sm:table-cell">Lancé le</th>
            <th className="text-center px-4 py-2.5 font-medium">Statut</th>
            <th className="text-right px-4 py-2.5 font-medium hidden md:table-cell">Cycle</th>
            <th className="w-28" />
          </tr>
        </thead>
        <tbody>
          {sorted.map((run) => (
            <RunRow
              key={run.id}
              run={run}
              scenarioLabel={run.scenarioId ? scenarioName.get(run.scenarioId) ?? '—' : 'Run de dev'}
              onOpen={() => navigate(`/projects/${projectId}/runs/${run.id}`)}
            />
          ))}
        </tbody>
      </table>
    </div>
  )
}

function RunRow({
  run,
  scenarioLabel,
  onOpen,
}: {
  run: SimulationRun
  scenarioLabel: string
  onOpen: () => void
}) {
  const done = run.status === 'TERMINE'
  const running = run.status === 'EN_COURS' || run.status === 'EN_FILE'

  return (
    <tr className="border-b border-neutral-100 last:border-0 hover:bg-neutral-50 transition-colors">
      <td className="px-4 py-2.5">
        <span className="font-medium text-neutral-800">{scenarioLabel}</span>
        <span className="block font-mono text-[10px] text-neutral-400 truncate max-w-[180px]">{run.id}</span>
      </td>
      <td className="px-4 py-2.5 hidden sm:table-cell text-neutral-500 text-[13px]">{fmtDate(run.createdAt)}</td>
      <td className="px-4 py-2.5 text-center">
        <span className="inline-flex items-center gap-1.5">
          {running && <Activity size={12} className="text-primary animate-pulse" />}
          <RunStatusBadge status={run.status} />
        </span>
      </td>
      <td className="px-4 py-2.5 text-right hidden md:table-cell tabular-nums text-neutral-600">
        {run.finalCycle || '—'}
      </td>
      <td className="px-3 py-2 text-right">
        <button
          onClick={onOpen}
          className={[
            'inline-flex items-center gap-1.5 text-xs font-medium rounded-lg px-2.5 py-1.5 transition-colors',
            done ? 'text-primary hover:bg-primary-50' : 'text-neutral-600 hover:bg-neutral-100',
          ].join(' ')}
        >
          {done ? <BarChart3 size={13} /> : <Eye size={13} />}
          {done ? 'Résultats' : 'Suivre'}
        </button>
      </td>
    </tr>
  )
}
