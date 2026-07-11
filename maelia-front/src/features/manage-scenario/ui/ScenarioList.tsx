import { useQuery, useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { Play, Loader2, Plus, Trash2, FlaskConical, CloudSun, SlidersHorizontal, X, Pencil, Copy } from 'lucide-react'
import { useState } from 'react'
import { queryKeys } from '@/shared/api'
import { Button } from '@/shared/ui'
import type { Scenario } from '@/entities/scenario'
import { listScenarios, launchRunForScenario, deleteScenario, createScenario } from '../api/scenario.api'

interface ScenarioListProps {
  projectId: string
}

function climateLabel(s: Scenario): string {
  const v = s.parameterValues?.['nomScenarioClimatique']
  return (typeof v === 'string' && v) || 'Climat observé'
}

export function ScenarioList({ projectId }: ScenarioListProps) {
  const navigate = useNavigate()
  const queryClient = useQueryClient()
  const [confirmId, setConfirmId] = useState<string | null>(null)

  const goToCreate = () => navigate(`/projects/${projectId}/scenarios/new`)

  const { data: scenarios, isLoading } = useQuery({
    queryKey: queryKeys.scenarios.all(projectId),
    queryFn: () => listScenarios(projectId),
  })

  const launchMutation = useMutation({
    mutationFn: (scenarioId: string) => launchRunForScenario(projectId, scenarioId),
    onSuccess: (run) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.runs.all(projectId) })
      // Route imbriquée dans le ProjectShell : la sidebar reste visible pendant l'exécution.
      navigate(`/projects/${projectId}/runs/${run.id}`)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: (scenarioId: string) => deleteScenario(projectId, scenarioId),
    onSuccess: () => {
      setConfirmId(null)
      queryClient.invalidateQueries({ queryKey: queryKeys.scenarios.all(projectId) })
    },
  })

  const duplicateMutation = useMutation({
    mutationFn: (source: Scenario) =>
      createScenario(projectId, {
        name: `${source.name} (copie)`,
        description: source.description,
        parameterValues: source.parameterValues ?? {},
      }),
    onSuccess: () => queryClient.invalidateQueries({ queryKey: queryKeys.scenarios.all(projectId) }),
  })

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-400 py-4">
        <Loader2 size={14} className="animate-spin" />
        Chargement des scénarios…
      </div>
    )
  }

  const hasScenarios = scenarios && scenarios.length > 0

  return (
    <div className="space-y-4">
      {/* Barre d'action */}
      <div className="flex items-center justify-between gap-3">
        <p className="text-[13px] text-neutral-500">
          {hasScenarios ? `${scenarios!.length} scénario(s)` : 'Aucun scénario pour le moment.'}
        </p>
        <Button variant="primary" size="sm" onClick={goToCreate}>
          <Plus size={14} /> Nouveau scénario
        </Button>
      </div>

      {/* Erreur de lancement (ex. 409 : une simulation est déjà en cours pour ce projet) */}
      {launchMutation.isError && (
        <div className="flex items-start gap-2 rounded-lg border border-amber-300 bg-amber-50 px-3 py-2 text-[13px] text-amber-800">
          <span className="flex-1">
            {(launchMutation.error as { detail?: string })?.detail ?? 'Impossible de lancer la simulation.'}
          </span>
          <button
            onClick={() => launchMutation.reset()}
            className="shrink-0 text-amber-500 hover:text-amber-700"
            aria-label="Fermer"
          >
            <X size={14} />
          </button>
        </div>
      )}

      {/* Liste des scénarios */}
      {hasScenarios ? (
          <div className="grid grid-cols-1 lg:grid-cols-2 gap-3">
            {scenarios!.map((scenario) => {
              const overrideCount = Object.keys(scenario.parameterValues ?? {}).length
              const launching = launchMutation.isPending && launchMutation.variables === scenario.id
              const confirming = confirmId === scenario.id

              return (
                <div
                  key={scenario.id}
                  className="group rounded-2xl border border-neutral-200 bg-white p-4 shadow-sm hover:border-primary-300 hover:shadow-md transition-all"
                >
                  <div className="flex items-start gap-3">
                    <div className="flex h-9 w-9 shrink-0 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
                      <FlaskConical size={17} />
                    </div>
                    <div className="min-w-0 flex-1">
                      <h4 className="text-[14px] font-semibold text-neutral-900 truncate">{scenario.name}</h4>
                      {scenario.description && (
                        <p className="text-[12px] text-neutral-500 mt-0.5 line-clamp-2">{scenario.description}</p>
                      )}
                    </div>
                  </div>

                  {/* Méta */}
                  <div className="flex items-center gap-2 flex-wrap mt-3">
                    <span className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full bg-neutral-100 text-neutral-600">
                      <CloudSun size={11} /> {climateLabel(scenario)}
                    </span>
                    <span className="inline-flex items-center gap-1 text-[11px] font-medium px-2 py-0.5 rounded-full bg-primary-50 text-primary-700">
                      <SlidersHorizontal size={11} /> {overrideCount} personnalisé(s)
                    </span>
                  </div>

                  {/* Actions */}
                  <div className="flex items-center justify-between gap-2 mt-4 pt-3 border-t border-neutral-100">
                    {confirming ? (
                      <div className="flex items-center gap-2 w-full">
                        <span className="text-[12px] text-neutral-600 flex-1">Supprimer ce scénario ?</span>
                        <Button
                          variant="danger"
                          size="sm"
                          onClick={() => deleteMutation.mutate(scenario.id)}
                          loading={deleteMutation.isPending && deleteMutation.variables === scenario.id}
                        >
                          Supprimer
                        </Button>
                        <Button variant="ghost" size="sm" onClick={() => setConfirmId(null)}>
                          <X size={14} />
                        </Button>
                      </div>
                    ) : (
                      <>
                        <div className="flex items-center gap-1">
                          <button
                            onClick={() => navigate(`/projects/${projectId}/scenarios/${scenario.id}/edit`)}
                            className="inline-flex items-center gap-1 text-[12px] text-neutral-500 hover:text-primary px-1.5 py-1 rounded transition-colors"
                            title="Modifier"
                          >
                            <Pencil size={13} /> Modifier
                          </button>
                          <button
                            onClick={() => duplicateMutation.mutate(scenario)}
                            disabled={duplicateMutation.isPending && duplicateMutation.variables?.id === scenario.id}
                            className="inline-flex items-center gap-1 text-[12px] text-neutral-500 hover:text-primary px-1.5 py-1 rounded transition-colors disabled:opacity-50"
                            title="Dupliquer"
                          >
                            <Copy size={13} /> Dupliquer
                          </button>
                          <button
                            onClick={() => setConfirmId(scenario.id)}
                            className="inline-flex items-center gap-1 text-[12px] text-neutral-400 hover:text-danger px-1.5 py-1 rounded transition-colors"
                            title="Supprimer"
                          >
                            <Trash2 size={13} />
                          </button>
                        </div>
                        <Button
                          variant="primary"
                          size="sm"
                          onClick={() => launchMutation.mutate(scenario.id)}
                          loading={launching}
                        >
                          <Play size={12} /> Lancer
                        </Button>
                      </>
                    )}
                  </div>
                </div>
              )
            })}
          </div>
        ) : (
          <div className="rounded-2xl border border-dashed border-neutral-200 bg-white py-12 text-center space-y-3">
            <div className="mx-auto flex h-12 w-12 items-center justify-center rounded-full bg-primary-50 text-primary-500">
              <FlaskConical size={22} />
            </div>
            <div>
              <p className="text-[14px] font-medium text-neutral-700">Aucun scénario</p>
              <p className="text-[13px] text-neutral-500 mt-0.5">
                Créez un scénario pour configurer puis lancer une simulation.
              </p>
            </div>
            <Button variant="primary" size="sm" onClick={goToCreate}>
              <Plus size={14} /> Créer un scénario
            </Button>
          </div>
        )}

      {launchMutation.isError && (
        <p className="text-xs text-danger">
          Erreur au lancement. Vérifiez que des données ont été matérialisées.
        </p>
      )}
    </div>
  )
}
