import { useParams, Link, useNavigate } from 'react-router-dom'
import { ArrowLeft } from 'lucide-react'
import { ScenarioForm } from '@/features/manage-scenario'

export function ScenarioCreatePage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  if (!id) return null

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
        <h1 className="text-xl font-semibold text-neutral-900">Nouveau scénario</h1>
        <p className="text-sm text-neutral-500 mt-1">
          Configurez les paramètres de simulation. Seuls les écarts à la valeur par défaut sont
          enregistrés.
        </p>
      </div>

      <ScenarioForm projectId={id} onSaved={backToList} onCancel={backToList} />
    </div>
  )
}
