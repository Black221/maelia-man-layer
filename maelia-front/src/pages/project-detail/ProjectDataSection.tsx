import { useParams } from 'react-router-dom'
import { CompletionTable } from '@/widgets/completion-table'

export function ProjectDataSection() {
  const { id } = useParams<{ id: string }>()
  if (!id) return null

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-[18px] font-semibold text-neutral-900">Données d&apos;entrée</h2>
        <p className="text-sm text-neutral-500 mt-1">
          Saisissez ou importez les fichiers requis pour la simulation.
        </p>
      </div>

      <CompletionTable projectId={id} />
    </div>
  )
}
