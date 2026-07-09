import { useQuery } from '@tanstack/react-query'
import { CheckCircle, XCircle, Loader2, Server } from 'lucide-react'
import { apiClient } from '@/shared/api'
import { queryKeys } from '@/shared/api'

interface HealthResponse {
  status: string
  service: string
  version: string
  timestamp: string
}

function useHealth() {
  return useQuery({
    queryKey: queryKeys.health,
    queryFn: () =>
      apiClient.get<HealthResponse>('/api/v1/health').then((r) => r.data),
    refetchInterval: 10_000,
  })
}

export function HealthPage() {
  const { data, isLoading, isError, error } = useHealth()

  return (
    <div className="min-h-screen bg-neutral-50 flex items-center justify-center p-6">
      <div className="bg-white rounded-[10px] border border-neutral-200 shadow-sm p-8 w-full max-w-md">
        {/* En-tête */}
        <div className="flex items-center gap-3 mb-6">
          <Server size={20} className="text-primary-600" />
          <h1 className="text-[18px] font-semibold text-neutral-900">
            MAELIA Platform
          </h1>
        </div>

        {/* Statut */}
        {isLoading && (
          <div className="flex items-center gap-3 text-neutral-500">
            <Loader2 size={20} className="animate-spin text-primary-500" />
            <span className="text-[14px]">Connexion au backend…</span>
          </div>
        )}

        {isError && (
          <div className="flex items-start gap-3">
            <XCircle size={20} className="text-red-500 mt-0.5 shrink-0" />
            <div>
              <p className="text-[14px] font-medium text-red-700">Backend inaccessible</p>
              <p className="text-[13px] text-neutral-500 mt-1">
                {(error as { detail?: string })?.detail ?? 'Impossible de joindre le serveur.'}
              </p>
              <p className="text-[12px] text-neutral-400 mt-2">
                Vérifiez que le backend tourne sur le port 8080.
              </p>
            </div>
          </div>
        )}

        {data && (
          <div className="space-y-4">
            <div className="flex items-center gap-3">
              <CheckCircle size={20} className="text-green-500 shrink-0" />
              <span className="text-[14px] font-medium text-green-700">Backend OK</span>
            </div>

            <div className="bg-neutral-50 rounded-[8px] border border-neutral-200 divide-y divide-neutral-200">
              {[
                ['Service', data.service],
                ['Version', data.version],
                ['Statut', data.status],
                ['Horodatage', new Date(data.timestamp).toLocaleString('fr-FR')],
              ].map(([label, value]) => (
                <div key={label} className="flex justify-between items-center px-4 py-2.5">
                  <span className="text-[13px] text-neutral-500">{label}</span>
                  <span className="text-[13px] font-medium text-neutral-800 font-mono">{value}</span>
                </div>
              ))}
            </div>
          </div>
        )}
      </div>
    </div>
  )
}
