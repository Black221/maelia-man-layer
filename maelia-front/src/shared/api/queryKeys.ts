// Clés de cache centralisées pour TanStack Query.
// Convention : tableau typé, du plus général au plus spécifique.

export const queryKeys = {
  health: ['health'] as const,
  projects: {
    all: ['projects'] as const,
    detail: (id: string) => ['projects', id] as const,
    completion: (id: string) => ['projects', id, 'completion'] as const,
  },
  dataspecs: {
    all: ['dataspecs'] as const,
    forConfig: (configId: string) => ['dataspecs', 'config', configId] as const,
    detail: (id: string) => ['dataspecs', id] as const,
    usage: (id: string) => ['dataspecs', id, 'usage'] as const,
  },
  datasets: {
    all: (projectId: string) => ['datasets', projectId] as const,
    detail: (id: string) => ['datasets', 'detail', id] as const,
  },
  scenarios: {
    all: (projectId: string) => ['scenarios', projectId] as const,
    detail: (id: string) => ['scenarios', 'detail', id] as const,
  },
  scenarioParameters: {
    all: ['scenario-parameters'] as const,
    groups: ['scenario-parameters', 'groups'] as const,
  },
  preprocessing: {
    graph: ['preprocessing', 'graph'] as const,
    plan: (projectId: string) => ['preprocessing', 'plan', projectId] as const,
  },
  runs: {
    all: (projectId: string) => ['runs', projectId] as const,
    detail: (id: string) => ['runs', id] as const,
    results: (id: string) => ['runs', id, 'results'] as const,
    dashboard: (id: string) => ['runs', id, 'dashboard'] as const,
  },
} as const
