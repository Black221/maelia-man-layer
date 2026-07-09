import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { FolderOpen, Plus } from 'lucide-react'
import { Button } from '@/shared/ui'
import { ProjectCard } from '@/entities/project'
import { CreateProjectModal, listProjects } from '@/features/create-project'
import { queryKeys } from '@/shared/api'

export function ProjectsPage() {
  const [showCreate, setShowCreate] = useState(false)

  const { data: projects, isLoading } = useQuery({
    queryKey: queryKeys.projects.all,
    queryFn: listProjects,
  })

  return (
    <div className="max-w-5xl mx-auto px-8 py-10 space-y-8">
      {/* En-tête */}
      <div className="flex items-start justify-between gap-4">
        <div>
          <h1 className="text-[24px] font-semibold text-neutral-900">Mes projets</h1>
          <p className="text-[14px] text-neutral-500 mt-1">
            Gérez vos projets de simulation MAELIA.
          </p>
        </div>
        <Button variant="primary" size="md" onClick={() => setShowCreate(true)}>
          <Plus size={16} />
          Nouveau projet
        </Button>
      </div>

      {/* Squelette de chargement */}
      {isLoading && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {[1, 2, 3].map((i) => (
            <div key={i} className="h-24 rounded-xl bg-neutral-100 animate-pulse" />
          ))}
        </div>
      )}

      {/* État vide */}
      {!isLoading && (!projects || projects.length === 0) && (
        <div className="bg-white rounded-[12px] border border-neutral-200 shadow-sm flex flex-col items-center justify-center py-24 px-8 text-center">
          <FolderOpen size={44} className="text-neutral-300 mb-4" />
          <h2 className="text-[16px] font-semibold text-neutral-700 mb-2">Aucun projet</h2>
          <p className="text-[14px] text-neutral-500 max-w-sm">
            Créez votre premier projet pour commencer à configurer une simulation MAELIA.
          </p>
          <Button variant="primary" size="md" className="mt-6" onClick={() => setShowCreate(true)}>
            <Plus size={16} />
            Créer un projet
          </Button>
        </div>
      )}

      {/* Grille de projets */}
      {projects && projects.length > 0 && (
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {projects.map((p) => (
            <ProjectCard key={p.id} project={p} />
          ))}
        </div>
      )}

      <CreateProjectModal open={showCreate} onClose={() => setShowCreate(false)} />
    </div>
  )
}
