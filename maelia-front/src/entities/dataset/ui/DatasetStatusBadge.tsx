import { Badge } from '@/shared/ui'
import type { DatasetStatus } from '../model/dataset.types'

const STATUS_CONFIG: Record<DatasetStatus, { label: string; variant: 'default' | 'success' | 'warning' | 'danger' | 'info' }> = {
  VIDE:      { label: 'Vide',      variant: 'default' },
  EN_COURS:  { label: 'En cours',  variant: 'warning' },
  VALIDE:    { label: 'Valide',    variant: 'success' },
  INVALIDE:  { label: 'Invalide',  variant: 'danger'  },
}

export function DatasetStatusBadge({ status }: { status: DatasetStatus }) {
  const cfg = STATUS_CONFIG[status] ?? { label: status, variant: 'neutral' }
  return <Badge variant={cfg.variant}>{cfg.label}</Badge>
}
