import { Link } from 'react-router-dom'
import { FolderOpen } from 'lucide-react'
import type { Project } from '../model/project.types'

interface ProjectCardProps {
  project: Project
}

export function ProjectCard({ project }: ProjectCardProps) {
  return (
    <Link
      to={`/projects/${project.id}`}
      className="block rounded-xl border border-neutral-200 bg-white p-5 shadow-sm hover:border-primary hover:shadow-md transition-all group"
    >
      <div className="flex items-start gap-3">
        <div className="p-2 rounded-lg bg-primary/10 text-primary shrink-0">
          <FolderOpen size={18} />
        </div>
        <div className="min-w-0">
          <p className="font-semibold text-neutral-900 truncate group-hover:text-primary transition-colors">
            {project.name}
          </p>
          {project.description && (
            <p className="text-sm text-neutral-500 mt-0.5 line-clamp-2">{project.description}</p>
          )}
          <p className="text-xs text-neutral-400 mt-2">
            {project.studyArea} · {new Date(project.createdAt).toLocaleDateString('fr-FR')}
          </p>
        </div>
      </div>
    </Link>
  )
}
