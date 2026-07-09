import { useState } from 'react'
import { useParams } from 'react-router-dom'
import { FlaskConical, History } from 'lucide-react'
import { ScenarioList, RunHistoryList } from '@/features/manage-scenario'

type Tab = 'scenarios' | 'runs'

export function ProjectScenariosSection() {
  const { id } = useParams<{ id: string }>()
  const [tab, setTab] = useState<Tab>('scenarios')
  if (!id) return null

  const tabs = [
    { id: 'scenarios' as const, label: 'Scénarios', icon: FlaskConical },
    { id: 'runs' as const, label: 'Simulations', icon: History },
  ]

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-[18px] font-semibold text-neutral-900">Scénarios &amp; Simulations</h2>
        <p className="text-sm text-neutral-500 mt-1">
          Configurez des scénarios, lancez des simulations et consultez leurs résultats.
        </p>
      </div>

      {/* Onglets */}
      <div className="border-b border-neutral-200">
        <div className="flex gap-1">
          {tabs.map(({ id: tid, label, icon: Icon }) => (
            <button
              key={tid}
              onClick={() => setTab(tid)}
              className={[
                'flex items-center gap-1.5 px-4 py-2.5 text-[13px] font-medium border-b-2 transition-colors',
                tab === tid
                  ? 'border-primary text-primary'
                  : 'border-transparent text-neutral-500 hover:text-neutral-800',
              ].join(' ')}
            >
              <Icon size={14} />
              {label}
            </button>
          ))}
        </div>
      </div>

      {tab === 'scenarios' ? <ScenarioList projectId={id} /> : <RunHistoryList projectId={id} />}
    </div>
  )
}
