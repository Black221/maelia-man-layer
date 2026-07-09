import { Badge } from '@/shared/ui'
import type { RunStatus } from '../model/run.types'

const config: Record<RunStatus, { label: string; variant: 'default' | 'success' | 'warning' | 'danger' | 'info' | 'primary' }> = {
  EN_FILE:  { label: 'En file',   variant: 'default'  },
  EN_COURS: { label: 'En cours',  variant: 'primary'  },
  TERMINE:  { label: 'Terminé',   variant: 'success'  },
  ECHEC:    { label: 'Échec',     variant: 'danger'   },
  ANNULE:   { label: 'Annulé',    variant: 'warning'  },
}

export function RunStatusBadge({ status }: { status: RunStatus }) {
  const { label, variant } = config[status] ?? { label: status, variant: 'default' }
  return <Badge variant={variant}>{label}</Badge>
}
