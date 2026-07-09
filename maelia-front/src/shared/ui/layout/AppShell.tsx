import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'
import { LayoutDashboard, Settings, FlaskConical, BarChart3 } from 'lucide-react'

interface AppShellProps {
  children: ReactNode
}

const navItems = [
  { to: '/projects', label: 'Projets', icon: LayoutDashboard },
  { to: '/settings', label: 'Configuration', icon: Settings },
  { to: '/scenarios', label: 'Scénarios', icon: FlaskConical },
  { to: '/results',  label: 'Résultats',  icon: BarChart3 },
]

export function AppShell({ children }: AppShellProps) {
  return (
    <div className="flex min-h-screen bg-neutral-50">
      {/* Barre latérale 240px */}
      <aside className="w-60 shrink-0 bg-white border-r border-neutral-200 flex flex-col">
        {/* Logo / nom de l'app */}
        <div className="h-14 flex items-center px-5 border-b border-neutral-200">
          <span className="text-[18px] font-semibold text-primary-700 tracking-tight">MAELIA</span>
        </div>

        {/* Navigation */}
        <nav className="flex-1 py-3 px-2 space-y-0.5">
          {navItems.map(({ to, label, icon: Icon }) => (
            <NavLink
              key={to}
              to={to}
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
          ))}
        </nav>

        {/* Version */}
        <div className="p-4 text-[12px] text-neutral-400">v0.1.0 · M0</div>
      </aside>

      {/* Zone de contenu */}
      <div className="flex-1 flex flex-col min-w-0">
        {/* En-tête 56px */}
        <header className="h-14 shrink-0 bg-neutral-50 border-b border-neutral-200 flex items-center px-6 gap-4">
          <div className="flex-1" />
          <span className="text-[13px] text-neutral-500">Plateforme de simulation MAELIA</span>
        </header>

        {/* Contenu de la page */}
        <main className="flex-1 p-6 overflow-auto">
          {children}
        </main>
      </div>
    </div>
  )
}
