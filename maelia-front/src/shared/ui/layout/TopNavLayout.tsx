import type { ReactNode } from 'react'
import { NavLink } from 'react-router-dom'

interface TopNavLayoutProps {
  children: ReactNode
}

const navItems = [
  { to: '/projects', label: 'Projets' },
  { to: '/catalog', label: 'Catalogue' },
  { to: '/scenario-parameters', label: 'Paramètres' },
  { to: '/test', label: 'Test GAMA' },
]

export function TopNavLayout({ children }: TopNavLayoutProps) {
  return (
    <div className="min-h-screen bg-neutral-50 flex flex-col">
      <header className="h-14 bg-white border-b border-neutral-200 flex items-center px-8 shrink-0">
        <span className="text-[18px] font-semibold text-primary-700 tracking-tight">MAELIA</span>
        <span className="mx-3 text-neutral-300 select-none">·</span>
        <span className="text-[13px] text-neutral-500">Plateforme de simulation</span>

        <nav className="ml-auto flex items-center gap-1">
          {navItems.map(({ to, label }) => (
            <NavLink
              key={to}
              to={to}
              className={({ isActive }) =>
                [
                  'px-3 py-1.5 rounded-lg text-[13px] font-medium transition-colors',
                  isActive
                    ? 'bg-primary-50 text-primary-700'
                    : 'text-neutral-500 hover:bg-neutral-100 hover:text-neutral-800',
                ].join(' ')
              }
            >
              {label}
            </NavLink>
          ))}
        </nav>
      </header>
      <main className="flex-1">{children}</main>
    </div>
  )
}
