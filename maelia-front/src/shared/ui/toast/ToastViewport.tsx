import { useSyncExternalStore } from 'react'
import { CheckCircle2, AlertCircle, Info, AlertTriangle, X } from 'lucide-react'
import { toastStore, type ToastType } from './toastStore'

const STYLES: Record<ToastType, { border: string; icon: typeof Info; color: string }> = {
  success: { border: 'border-l-success', icon: CheckCircle2, color: 'text-success' },
  error: { border: 'border-l-danger', icon: AlertCircle, color: 'text-danger' },
  warning: { border: 'border-l-warning', icon: AlertTriangle, color: 'text-warning' },
  info: { border: 'border-l-primary', icon: Info, color: 'text-primary' },
}

/** Conteneur des notifications, monté une fois près de la racine de l'app. */
export function ToastViewport() {
  const toasts = useSyncExternalStore(toastStore.subscribe, toastStore.getSnapshot, toastStore.getSnapshot)

  if (toasts.length === 0) return null

  return (
    <div className="fixed bottom-4 right-4 z-[100] flex flex-col gap-2 w-80 max-w-[calc(100vw-2rem)]">
      {toasts.map((t) => {
        const s = STYLES[t.type]
        const Icon = s.icon
        return (
          <div
            key={t.id}
            role="status"
            className={`flex items-start gap-2.5 rounded-lg border border-neutral-200 border-l-4 ${s.border} bg-white px-3.5 py-2.5 shadow-md`}
          >
            <Icon size={16} className={`shrink-0 mt-0.5 ${s.color}`} />
            <p className="flex-1 text-[13px] leading-snug text-neutral-700">{t.message}</p>
            <button
              onClick={() => toastStore.dismiss(t.id)}
              className="shrink-0 text-neutral-300 hover:text-neutral-600 transition-colors"
              aria-label="Fermer"
            >
              <X size={14} />
            </button>
          </div>
        )
      })}
    </div>
  )
}
