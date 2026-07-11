import { useMemo, useState } from 'react'
import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Loader2, BarChart3, History, CheckSquare, Square } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import { listRunsForProject } from '@/features/manage-scenario/api/scenario.api'
import { AgriDashboard, type SelectedRun } from '@/features/run-results'
import type { SimulationRun } from '@/entities/run'

function fmtDate(iso: string | null): string {
  if (!iso) return '—'
  return new Date(iso).toLocaleString('fr-FR', {
    day: '2-digit', month: '2-digit', year: 'numeric', hour: '2-digit', minute: '2-digit',
  })
}

function runLabel(run: SimulationRun): string {
  return run.scenarioName ?? (run.scenarioId ? 'Scénario supprimé' : 'Run de dev')
}

/**
 * Section « Résultats » d'un projet : on choisit dans la liste des simulations terminées
 * lesquelles visualiser (multi-sélection = comparaison), puis on affiche le tableau de bord.
 */
export function ProjectResultsSection() {
  const { id: projectId } = useParams<{ id: string }>()
  const [selected, setSelected] = useState<string[]>([])

  const { data: runs, isLoading } = useQuery({
    queryKey: queryKeys.runs.all(projectId!),
    queryFn: () => listRunsForProject(projectId!),
    enabled: !!projectId,
    refetchInterval: 8000,
  })

  const finished = useMemo(
    () => (runs ?? []).filter((r) => r.status === 'TERMINE')
      .sort((a, b) => (a.createdAt < b.createdAt ? 1 : -1)),
    [runs],
  )

  const toggle = (runId: string) =>
    setSelected((prev) => (prev.includes(runId) ? prev.filter((x) => x !== runId) : [...prev, runId]))

  const selectedRuns: SelectedRun[] = finished
    .filter((r) => selected.includes(r.id))
    .map((r) => ({ runId: r.id, scenarioName: runLabel(r) }))

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-10 text-neutral-400 text-sm">
        <Loader2 size={16} className="animate-spin" /> Chargement des résultats…
      </div>
    )
  }

  return (
    <div className="max-w-5xl space-y-6">
      <div>
        <h1 className="text-xl font-semibold text-neutral-900">Résultats</h1>
        <p className="text-[13px] text-neutral-500 mt-0.5">
          Sélectionnez un ou plusieurs scénarios simulés à visualiser. Plusieurs sélections = comparaison.
        </p>
      </div>

      {finished.length === 0 ? (
        <div className="rounded-2xl border border-dashed border-neutral-200 bg-white py-12 text-center space-y-3">
          <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-neutral-100 text-neutral-400">
            <History size={22} />
          </div>
          <div>
            <p className="text-[14px] font-medium text-neutral-700">Aucune simulation terminée</p>
            <p className="text-[13px] text-neutral-500 mt-0.5">
              Lancez un scénario depuis l’onglet «&nbsp;
              <Link to={`/projects/${projectId}/scenarios`} className="text-primary hover:underline">Scénarios</Link>
              &nbsp;» pour voir apparaître ses résultats ici.
            </p>
          </div>
        </div>
      ) : (
        <>
          {/* Liste de sélection des résultats de scénario */}
          <div className="rounded-2xl border border-neutral-200 overflow-hidden bg-white">
            <table className="w-full text-sm">
              <thead>
                <tr className="bg-neutral-50 border-b border-neutral-200 text-neutral-500">
                  <th className="w-10 px-3 py-2.5" />
                  <th className="text-left px-4 py-2.5 font-medium">Scénario</th>
                  <th className="text-left px-4 py-2.5 font-medium hidden sm:table-cell">Terminé le</th>
                  <th className="text-right px-4 py-2.5 font-medium hidden md:table-cell">Cycle final</th>
                </tr>
              </thead>
              <tbody>
                {finished.map((run) => {
                  const checked = selected.includes(run.id)
                  return (
                    <tr
                      key={run.id}
                      onClick={() => toggle(run.id)}
                      className={[
                        'border-b border-neutral-100 last:border-0 cursor-pointer transition-colors',
                        checked ? 'bg-primary-50/50' : 'hover:bg-neutral-50',
                      ].join(' ')}
                    >
                      <td className="px-3 py-2.5 text-primary">
                        {checked ? <CheckSquare size={16} /> : <Square size={16} className="text-neutral-300" />}
                      </td>
                      <td className="px-4 py-2.5">
                        <span className="font-medium text-neutral-800">{runLabel(run)}</span>
                        <span className="block font-mono text-[10px] text-neutral-400 truncate max-w-[220px]">{run.id}</span>
                      </td>
                      <td className="px-4 py-2.5 hidden sm:table-cell text-neutral-500 text-[13px]">
                        {fmtDate(run.finishedAt ?? run.createdAt)}
                      </td>
                      <td className="px-4 py-2.5 text-right hidden md:table-cell tabular-nums text-neutral-600">
                        {run.finalCycle || '—'}
                      </td>
                    </tr>
                  )
                })}
              </tbody>
            </table>
          </div>

          {/* Tableau de bord des résultats sélectionnés */}
          {selectedRuns.length === 0 ? (
            <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center space-y-1.5">
              <BarChart3 size={22} className="mx-auto text-neutral-300" />
              <p className="text-sm font-medium text-neutral-700">Sélectionnez un résultat</p>
              <p className="text-xs text-neutral-500">
                Cochez au moins un scénario ci-dessus pour afficher son tableau de bord.
              </p>
            </div>
          ) : (
            <AgriDashboard runs={selectedRuns} />
          )}
        </>
      )}
    </div>
  )
}
