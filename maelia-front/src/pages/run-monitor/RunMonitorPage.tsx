import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, CheckCircle2, XCircle, Clock } from 'lucide-react'
import { RunStatusBadge } from '@/entities/run'
import { useRunProgress, RunProgressBar, RunLog } from '@/features/run-progress'
import { getRunStatus } from '@/features/launch-run'
import { ResultDashboard } from '@/features/run-results'
import { queryKeys } from '@/shared/api'
import type { RunStatus } from '@/entities/run'

export function RunMonitorPage() {
  // Deux routes mènent ici : `/runs/:id` (autonome) et `/projects/:id/runs/:runId`
  // (imbriquée dans le ProjectShell, sidebar conservée). On résout l'id du run et
  // l'éventuel projet de façon robuste.
  const params = useParams<{ id?: string; runId?: string }>()
  const runId = params.runId ?? params.id!
  const routeProjectId = params.runId ? params.id : undefined

  const { data: initialRun } = useQuery({
    queryKey: queryKeys.runs.detail(runId),
    queryFn: () => getRunStatus(runId),
    enabled: !!runId,
    refetchOnWindowFocus: false,
  })

  const progress = useRunProgress(runId, (initialRun?.status as RunStatus) ?? 'EN_FILE')

  // Le flux STOMP n'alimente que les runs EN DIRECT. Quand on revient sur un run déjà terminé
  // (ou lancé avant l'ouverture de la page), aucun message live n'arrive et le statut resterait
  // bloqué sur 'EN_FILE' avec un journal vide. On retombe alors sur le statut réellement chargé
  // via getRunStatus : on privilégie le live dès qu'il a avancé, sinon la valeur persistée.
  const status: RunStatus = progress.status !== 'EN_FILE'
    ? progress.status
    : ((initialRun?.status as RunStatus) ?? 'EN_FILE')
  const error = progress.error ?? initialRun?.errorMessage ?? null
  const cycle = progress.cycle || initialRun?.finalCycle || 0
  const isTerminal = status === 'TERMINE' || status === 'ECHEC' || status === 'ANNULE'

  const projectId = routeProjectId ?? initialRun?.projectId
  const backTo = projectId ? `/projects/${projectId}/scenarios` : '/projects'
  const backLabel = projectId ? 'Retour aux scénarios' : 'Retour aux projets'

  return (
    <div className="max-w-4xl mx-auto py-8 px-4 space-y-6">
      <Link
        to={backTo}
        className="inline-flex items-center gap-1.5 text-sm text-neutral-500 hover:text-primary transition-colors"
      >
        <ArrowLeft size={14} />
        {backLabel}
      </Link>

      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-xl font-semibold text-neutral-900">
            {initialRun?.scenarioName ? (
              <>Scénario : <span className="text-primary">{initialRun.scenarioName}</span></>
            ) : (
              'Exécution de simulation'
            )}
          </h1>
          <p className="mt-0.5 font-mono text-xs text-neutral-400 break-all">{runId}</p>
        </div>
        <RunStatusBadge status={status} />
      </div>

      <div className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm space-y-4">
        <RunProgressBar status={status} cycle={cycle} />

        {(status === 'ECHEC' || error) && (
          <div className="flex items-start gap-2 rounded-lg border border-danger/30 bg-danger/5 p-3">
            <XCircle size={16} className="shrink-0 mt-0.5 text-danger" />
            <p className="text-sm text-danger">{error ?? 'La simulation a échoué.'}</p>
          </div>
        )}

        {status === 'TERMINE' && (
          <div className="flex items-center gap-2 rounded-lg border border-success/30 bg-success/5 p-3">
            <CheckCircle2 size={16} className="text-success shrink-0" />
            <p className="text-sm text-success">
              Simulation terminée — cycle final&nbsp;:&nbsp;
              <span className="font-mono font-semibold">{cycle}</span>
            </p>
          </div>
        )}

        {status === 'EN_FILE' && (
          <div className="flex items-center gap-2 text-sm text-neutral-500">
            <Clock size={14} className="shrink-0 animate-pulse" />
            En file d&apos;attente…
          </div>
        )}
      </div>

      <div className="space-y-2">
        <h2 className="text-sm font-medium text-neutral-700">Journal GAMA</h2>
        {isTerminal && progress.logs.length === 0 ? (
          <p className="rounded-lg border border-neutral-200 bg-neutral-50 px-3 py-2 text-xs text-neutral-500">
            Le journal en direct n&apos;est disponible que pendant l&apos;exécution. Ce run est terminé —
            consultez ses sorties ci-dessous ou dans l&apos;onglet «&nbsp;Résultats&nbsp;».
          </p>
        ) : (
          <RunLog logs={progress.logs} />
        )}
      </div>

      {status === 'TERMINE' && (
        <div className="space-y-3">
          <h2 className="text-sm font-medium text-neutral-700">Résultats</h2>
          <ResultDashboard runId={runId} />
        </div>
      )}

      {isTerminal && (
        <div className="pt-2 text-center">
          <Link
            to={backTo}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-primary hover:underline"
          >
            {backLabel}
          </Link>
        </div>
      )}
    </div>
  )
}
