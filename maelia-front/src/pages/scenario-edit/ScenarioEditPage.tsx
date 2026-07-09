import { useParams, Link, useNavigate } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { ScenarioForm, getScenario } from '@/features/manage-scenario'
import { queryKeys } from '@/shared/api'

export function ScenarioEditPage() {
  const { id, scenarioId } = useParams<{ id: string; scenarioId: string }>()
  const navigate = useNavigate()

  const { data: scenario, isLoading, isError } = useQuery({
    queryKey: queryKeys.scenarios.detail(scenarioId!),
    queryFn: () => getScenario(id!, scenarioId!),
    enabled: !!id && !!scenarioId,
  })

  if (!id || !scenarioId) return null

  const backToList = () => navigate(`/projects/${id}/scenarios`)

  return (
    <div className="max-w-5xl mx-auto space-y-6">
      <Link
        to={`/projects/${id}/scenarios`}
        className="inline-flex items-center gap-1.5 text-[13px] text-neutral-500 hover:text-primary transition-colors"
      >
        <ArrowLeft size={14} />
        Scénarios
      </Link>

      <div>
        <h1 className="text-xl font-semibold text-neutral-900">Modifier le scénario</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Ajustez les paramètres. Seuls les écarts à la valeur par défaut sont enregistrés.
        </p>
      </div>

      {isLoading && (
        <div className="flex items-center gap-2 py-12 text-neutral-400">
          <Loader2 size={18} className="animate-spin" /> Chargement du scénario…
        </div>
      )}

      {isError && <p className="text-sm text-danger">Scénario introuvable.</p>}

      {scenario && (
        <ScenarioForm
          projectId={id}
          scenarioId={scenarioId}
          initialValues={{
            name: scenario.name,
            description: scenario.description,
            parameterValues: scenario.parameterValues ?? {},
          }}
          onSaved={backToList}
          onCancel={backToList}
        />
      )}
    </div>
  )
}
