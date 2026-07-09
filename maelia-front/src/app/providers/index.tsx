import { QueryClient, QueryClientProvider, MutationCache } from '@tanstack/react-query'
import type { ReactNode } from 'react'
import { ToastViewport, toast } from '@/shared/ui'

/** Extrait un message lisible d'une erreur API (ApiError RFC 7807) ou générique. */
function errorMessage(error: unknown): string {
  const e = error as { detail?: string; title?: string; message?: string }
  return e?.detail || e?.title || e?.message || 'Une erreur est survenue.'
}

const queryClient = new QueryClient({
  // Toute mutation échouée (create/update/delete) notifie l'utilisateur, sauf opt-out (meta.silent).
  mutationCache: new MutationCache({
    onError: (error, _vars, _ctx, mutation) => {
      if (mutation.meta?.silent) return
      toast.error(errorMessage(error))
    },
  }),
  defaultOptions: {
    queries: {
      staleTime: 30_000,        // 30s avant de considérer les données périmées
      retry: 1,
      refetchOnWindowFocus: false,
    },
    mutations: {
      retry: 0,
    },
  },
})

export function Providers({ children }: { children: ReactNode }) {
  return (
    <QueryClientProvider client={queryClient}>
      {children}
      <ToastViewport />
    </QueryClientProvider>
  )
}
