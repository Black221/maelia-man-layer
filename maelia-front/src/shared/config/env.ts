export const API_BASE_URL = import.meta.env.VITE_API_URL ?? ''
// En dev, Vite proxifie /api → localhost:8080 (vite.config.ts)
// En prod, nginx proxifie /api → api:8080 (nginx.conf)
