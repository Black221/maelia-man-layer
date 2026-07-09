import { useParams } from 'react-router-dom'
import { PreprocessingPlan } from '@/widgets/preprocessing-plan'

export function ProjectPreprocessingSection() {
  const { id } = useParams<{ id: string }>()
  if (!id) return null

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-[18px] font-semibold text-neutral-900">Prétraitement</h2>
        <p className="text-sm text-neutral-500 mt-1">
          Plan de génération des fichiers d&apos;entrée : ordre de production, dépendances
          entre fichiers et état de chaque étape.
        </p>
      </div>

      <PreprocessingPlan projectId={id} />
    </div>
  )
}
