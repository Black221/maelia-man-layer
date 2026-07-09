import type { RunStatus } from '@/entities/run'

interface RunProgressBarProps {
  status: RunStatus
  cycle: number
  maxCycle?: number
}

const STATUS_LABELS: Record<RunStatus, string> = {
  EN_FILE: 'En attente...',
  EN_COURS: 'Simulation en cours',
  TERMINE: 'Terminé',
  ECHEC: 'Échec',
  ANNULE: 'Annulé',
}

export function RunProgressBar({ status, cycle, maxCycle }: RunProgressBarProps) {
  const isRunning = status === 'EN_COURS'
  const isDone = status === 'TERMINE'
  const isFailed = status === 'ECHEC' || status === 'ANNULE'

  const percent = isDone
    ? 100
    : maxCycle && maxCycle > 0
    ? Math.min(Math.round((cycle / maxCycle) * 100), 99)
    : isRunning
    ? undefined // indéterminé
    : 0

  const barColor = isFailed
    ? 'bg-danger'
    : isDone
    ? 'bg-success'
    : 'bg-primary'

  return (
    <div className="space-y-2">
      <div className="flex items-center justify-between text-sm">
        <span className="text-neutral-600">{STATUS_LABELS[status]}</span>
        {cycle > 0 && (
          <span className="font-mono text-neutral-500">
            Cycle {cycle}{maxCycle ? ` / ${maxCycle}` : ''}
          </span>
        )}
      </div>

      <div className="h-2 w-full rounded-full bg-neutral-200 overflow-hidden">
        {percent !== undefined ? (
          <div
            className={`h-full rounded-full transition-all duration-500 ${barColor}`}
            style={{ width: `${percent}%` }}
          />
        ) : (
          /* Barre animée pour progression indéterminée */
          <div
            className={`h-full w-1/3 rounded-full ${barColor} animate-progress-indeterminate`}
          />
        )}
      </div>

      {percent !== undefined && (
        <p className="text-xs text-right text-neutral-400">{percent}%</p>
      )}
    </div>
  )
}
