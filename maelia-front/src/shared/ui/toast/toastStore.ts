// Store de notifications (snackbar) minimal, sans dépendance.
// Utilisable depuis React (useToasts) ET hors React (intercepteurs, QueryClient) via `toast`.

export type ToastType = 'success' | 'error' | 'info' | 'warning'

export interface Toast {
  id: number
  type: ToastType
  message: string
}

const DEFAULT_DURATION = 4500
let toasts: Toast[] = []
let seq = 0
const listeners = new Set<() => void>()

function emit() {
  // nouvelle référence de tableau pour useSyncExternalStore
  toasts = [...toasts]
  listeners.forEach((l) => l())
}

export const toastStore = {
  subscribe(listener: () => void): () => void {
    listeners.add(listener)
    return () => listeners.delete(listener)
  },
  getSnapshot(): Toast[] {
    return toasts
  },
  push(type: ToastType, message: string, duration = DEFAULT_DURATION): number {
    const id = ++seq
    toasts.push({ id, type, message })
    emit()
    if (duration > 0) {
      setTimeout(() => toastStore.dismiss(id), duration)
    }
    return id
  },
  dismiss(id: number) {
    toasts = toasts.filter((t) => t.id !== id)
    emit()
  },
}

/** API impérative (succès / erreur / info / avertissement). */
export const toast = {
  success: (message: string) => toastStore.push('success', message),
  error: (message: string) => toastStore.push('error', message),
  info: (message: string) => toastStore.push('info', message),
  warning: (message: string) => toastStore.push('warning', message),
}
