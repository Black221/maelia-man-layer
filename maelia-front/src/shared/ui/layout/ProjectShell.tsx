import type { ElementType } from 'react'
import { Outlet, NavLink, Link, useParams } from 'react-router-dom'
import { useQuery } from '@tanstack/react-query'
import { ArrowLeft, Settings, Database, FlaskConical, BarChart3, Workflow } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import { getProject } from '@/features/create-project'

interface SidebarItem {
  to: string
  label: string
  icon: ElementType
  matchSub?: boolean
  disabled?: boolean
}

const sidebarItems: SidebarItem[] = [
  { to: 'config',    label: 'Initialisation', icon: Settings },
  { to: 'data',          label: 'Données',       icon: Database,     matchSub: true },
  { to: 'preprocessing', label: 'Prétraitement', icon: Workflow },
  { to: 'scenarios',     label: 'Scénarios',     icon: FlaskConical },
  { to: 'results',   label: 'Résultats',     icon: BarChart3,     disabled: true },
]

export function ProjectShell() {
  const { id } = useParams<{ id: string }>()

  const { data: project } = useQuery({
    queryKey: queryKeys.projects.detail(id!),
    queryFn: () => getProject(id!),
    enabled: !!id,
    staleTime: 5 * 60 * 1000,
  })

  return (
    <div className="flex min-h-screen bg-neutral-50">
      {/* Sidebar */}
      <aside className="w-60 shrink-0 bg-white border-r border-neutral-200 flex flex-col sticky top-0 h-screen overflow-y-auto">
        {/* Logo + retour */}
        <div className="h-14 flex items-center gap-2 px-4 border-b border-neutral-200 shrink-0">
          <span className="text-[16px] font-semibold text-primary-700 tracking-tight">MAELIA</span>
          <div className="flex-1" />
          <Link
            to="/projects"
            className="flex items-center gap-1 text-[12px] text-neutral-400 hover:text-primary-700 transition-colors"
          >
            <ArrowLeft size={12} />
            Projets
          </Link>
        </div>

        {/* Nom du projet */}
        <div className="px-4 py-3 border-b border-neutral-100 shrink-0">
          {project ? (
            <>
              <p className="text-[10px] font-medium uppercase tracking-wider text-neutral-400 mb-1">
                Projet
              </p>
              <p className="text-[13px] font-semibold text-neutral-800 truncate leading-snug">
                {project.name}
              </p>
              {project.studyArea && (
                <p className="text-[11px] text-neutral-400 mt-0.5 truncate">{project.studyArea}</p>
              )}
            </>
          ) : (
            <div className="space-y-1.5">
              <div className="h-2.5 w-14 bg-neutral-100 rounded animate-pulse" />
              <div className="h-3.5 w-36 bg-neutral-100 rounded animate-pulse" />
            </div>
          )}
        </div>

        {/* Navigation */}
        <nav className="flex-1 py-3 px-2 space-y-0.5">
          {sidebarItems.map(({ to, label, icon: Icon, matchSub, disabled }) =>
            disabled ? (
              <div
                key={to}
                className="flex items-center gap-3 px-3 py-2 rounded-[8px] text-[14px] font-medium text-neutral-300 cursor-not-allowed select-none"
              >
                <Icon size={16} strokeWidth={1.5} />
                <span className="flex-1">{label}</span>
                <span className="text-[10px] bg-neutral-100 text-neutral-400 px-1.5 py-0.5 rounded-full">
                  bientôt
                </span>
              </div>
            ) : (
              <NavLink
                key={to}
                to={to}
                end={!matchSub}
                className={({ isActive }) =>
                  [
                    'flex items-center gap-3 px-3 py-2 rounded-[8px] text-[14px] font-medium transition-colors duration-100',
                    isActive
                      ? 'bg-primary-50 text-primary-700'
                      : 'text-neutral-600 hover:bg-neutral-100 hover:text-neutral-900',
                  ].join(' ')
                }
              >
                <Icon size={16} strokeWidth={1.5} />
                {label}
              </NavLink>
            )
          )}
        </nav>

        <div className="p-4 text-[11px] text-neutral-400 shrink-0">v0.1.0 · M4</div>
      </aside>

      {/* Contenu — chaque section gère sa propre largeur max */}
      <main className="flex-1 min-h-screen overflow-auto">
        <div className="p-8">
          <Outlet />
        </div>
      </main>
    </div>
  )
}
