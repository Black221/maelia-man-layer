import { useEffect, useReducer, useRef } from 'react'
import { useQueryClient } from '@tanstack/react-query'
import { stompClient } from '@/shared/api/stomp'
import type { StompRunUpdate, RunStatus } from '@/entities/run'
import { queryKeys } from '@/shared/api'

export interface RunProgressState {
  status: RunStatus
  cycle: number
  logs: string[]
  error: string | null
}

type Action =
  | { type: 'PROGRESS'; cycle: number; message: string | null }
  | { type: 'LOG'; message: string }
  | { type: 'STATUS'; status: RunStatus }
  | { type: 'ERROR'; error: string }
  | { type: 'ENDED' }

function reducer(state: RunProgressState, action: Action): RunProgressState {
  switch (action.type) {
    case 'PROGRESS':
      return { ...state, cycle: action.cycle,
        logs: action.message ? [...state.logs, action.message] : state.logs }
    case 'LOG':
      return { ...state, logs: [...state.logs, action.message] }
    case 'STATUS':
      return { ...state, status: action.status }
    case 'ENDED':
      return { ...state, status: 'TERMINE' }
    case 'ERROR':
      return { ...state, status: 'ECHEC', error: action.error }
    default:
      return state
  }
}

/**
 * S'abonne à /topic/runs/{runId} via STOMP et expose l'état de progression.
 * La connexion STOMP est ouverte à l'entrée du composant, fermée à la sortie.
 */
export function useRunProgress(runId: string, initialStatus: RunStatus = 'EN_FILE') {
  const queryClient = useQueryClient()
  const [state, dispatch] = useReducer(reducer, {
    status: initialStatus,
    cycle: 0,
    logs: [],
    error: null,
  })

  // Ref pour éviter les closures stale sur dispatch
  const dispatchRef = useRef(dispatch)
  dispatchRef.current = dispatch

  useEffect(() => {
    if (!runId) return

    const topic = `/topic/runs/${runId}`

    const onConnect = () => {
      stompClient.subscribe(topic, (frame) => {
        const update: StompRunUpdate = JSON.parse(frame.body)

        switch (update.type) {
          case 'PROGRESS':
            dispatchRef.current({ type: 'PROGRESS', cycle: update.cycle, message: update.message })
            break
          case 'LOG':
            if (update.message) dispatchRef.current({ type: 'LOG', message: update.message })
            break
          case 'EN_COURS':
            dispatchRef.current({ type: 'STATUS', status: 'EN_COURS' })
            break
          case 'ENDED':
          case 'TERMINE':
            dispatchRef.current({ type: 'ENDED' })
            // Invalider le cache du run pour recharger le statut final
            queryClient.invalidateQueries({ queryKey: queryKeys.runs.detail(runId) })
            break
          case 'ECHEC':
          case 'ERROR':
            dispatchRef.current({ type: 'ERROR', error: update.error ?? 'Erreur inconnue' })
            queryClient.invalidateQueries({ queryKey: queryKeys.runs.detail(runId) })
            break
        }
      })
    }

    if (stompClient.connected) {
      onConnect()
    } else {
      stompClient.onConnect = onConnect
      stompClient.activate()
    }

    return () => {
      if (stompClient.connected) {
        stompClient.deactivate()
      }
    }
  }, [runId, queryClient])

  return state
}
