import { Client } from '@stomp/stompjs'

const WS_URL = import.meta.env.VITE_WS_URL ?? `ws://${window.location.hostname}:8080/ws`

/**
 * Client STOMP partagé (singleton).
 * Connexion lazy : activée à la première subscription, désactivée quand plus rien n'écoute.
 */
export const stompClient = new Client({
  brokerURL: WS_URL,
  reconnectDelay: 3000,
  heartbeatIncoming: 10000,
  heartbeatOutgoing: 10000,
})
