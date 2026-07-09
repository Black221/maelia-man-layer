import axios from 'axios'
import type { AxiosError } from 'axios'
import { API_BASE_URL } from '@/shared/config/env'

export interface ApiError {
  status: number
  title: string
  detail: string
  type?: string
}

export const apiClient = axios.create({
  baseURL: API_BASE_URL,
  // Ne pas forcer Content-Type globalement : axios pose `application/json` pour les
  // corps objet et `multipart/form-data; boundary=…` pour les FormData (upload CSV/SHP).
  // Le forcer casserait l'upload (« current request is not a multipart request »).
  headers: { Accept: 'application/json' },
  timeout: 30_000,
})

// Normalise les erreurs RFC 7807 (application/problem+json) en ApiError
apiClient.interceptors.response.use(
  (res) => res,
  (error: AxiosError) => {
    const data = error.response?.data as Record<string, unknown> | undefined
    const apiError: ApiError = {
      status: error.response?.status ?? 0,
      title: (data?.['title'] as string) ?? 'Error',
      detail: (data?.['detail'] as string) ?? error.message,
      type: data?.['type'] as string | undefined,
    }
    return Promise.reject(apiError)
  },
)
