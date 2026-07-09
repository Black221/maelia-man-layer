import { useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { Loader2 } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import { getProject } from '@/features/create-project'
import { InitZipUpload, ModelingConfigForm, ProjectInfoForm } from '@/features/configure-project'

/**
 * Page « Initialisation » : informations générales du projet, choix des modules
 * (avec aperçu des fichiers nécessaires) et initialisation en masse par archive ZIP.
 * Les options fines de modélisation se règlent dans les paramètres de scénario.
 */
export function ProjectConfigSection() {
  const { id } = useParams<{ id: string }>()

  const { data: project, isLoading } = useQuery({
    queryKey: queryKeys.projects.detail(id!),
    queryFn: () => getProject(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-20 text-neutral-400">
        <Loader2 size={18} className="animate-spin" />
        Chargement…
      </div>
    )
  }

  if (!project) return null

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-[18px] font-semibold text-neutral-900">Initialisation</h2>
        <p className="text-sm text-neutral-500 mt-1">
          Renseignez le projet, choisissez les modules de simulation et initialisez les
          données d&apos;entrée en une fois.
        </p>
      </div>

      {/* 1. Informations du projet */}
      <section className="rounded-xl border border-neutral-200 bg-white p-6 shadow-sm">
        <h3 className="text-[15px] font-semibold text-neutral-800 mb-4">Projet</h3>
        <ProjectInfoForm project={project} />
      </section>

      {/* 2. Modules + fichiers nécessaires */}
      <section className="rounded-xl border border-neutral-200 bg-white p-6 shadow-sm">
        <h3 className="text-[15px] font-semibold text-neutral-800 mb-4">Modules de simulation</h3>
        <ModelingConfigForm
          projectId={project.id}
          initialConfig={project.modelingConfiguration}
        />
      </section>

      {/* 3. Initialisation des données */}
      <section className="rounded-xl border border-neutral-200 bg-white p-6 shadow-sm">
        <h3 className="text-[15px] font-semibold text-neutral-800 mb-4">
          Initialisation des données (ZIP)
        </h3>
        <InitZipUpload projectId={project.id} />
      </section>
    </div>
  )
}
