import { useParams, Link } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Loader2 } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import { getProject } from '@/features/create-project'
import { ModelingConfigForm } from '@/features/configure-project'
import { CompletionTable } from '@/widgets/completion-table'
import { ScenarioList } from '@/features/manage-scenario'

export function ProjectDetailPage() {
  const { id } = useParams<{ id: string }>()

  const { data: project, isLoading, isError } = useQuery({
    queryKey: queryKeys.projects.detail(id!),
    queryFn: () => getProject(id!),
    enabled: !!id,
  })

  if (isLoading) {
    return (
      <div className="flex items-center justify-center py-20 text-neutral-400">
        <Loader2 size={20} className="animate-spin mr-2" />
        Chargement du projet…
      </div>
    )
  }

  if (isError || !project) {
    return (
      <div className="py-12 text-center text-neutral-500">
        Projet introuvable.{' '}
        <Link to="/projects" className="text-primary hover:underline">Retour aux projets</Link>
      </div>
    )
  }

  return (
    <div className="max-w-3xl mx-auto space-y-8 py-6 px-4">
      <Link
        to="/projects"
        className="inline-flex items-center gap-1.5 text-sm text-neutral-500 hover:text-primary transition-colors"
      >
        <ArrowLeft size={14} />
        Projets
      </Link>

      <div>
        <h1 className="text-xl font-semibold text-neutral-900">{project.name}</h1>
        {project.description && (
          <p className="text-sm text-neutral-500 mt-1">{project.description}</p>
        )}
        <p className="text-xs text-neutral-400 mt-1">
          Zone : {project.studyArea} ·{' '}
          Créé le {new Date(project.createdAt).toLocaleDateString('fr-FR')}
        </p>
      </div>

      {/* Configuration de modélisation */}
      <section className="rounded-xl border border-neutral-200 bg-white p-5 shadow-sm space-y-4">
        <h2 className="text-base font-semibold text-neutral-800">Configuration de modélisation</h2>
        <ModelingConfigForm
          projectId={project.id}
          initialConfig={project.modelingConfiguration}
        />
      </section>

      {/* Tableau de complétude */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-neutral-800">Données d&apos;entrée requises</h2>
        <CompletionTable projectId={project.id} />
      </section>

      {/* Scénarios & simulation */}
      <section className="space-y-3">
        <h2 className="text-base font-semibold text-neutral-800">Simulations</h2>
        <p className="text-sm text-neutral-500">
          Définissez un scénario (paramètres climatiques, dates, graine…) puis lancez une simulation.
        </p>
        <ScenarioList projectId={project.id} />
      </section>
    </div>
  )
}
