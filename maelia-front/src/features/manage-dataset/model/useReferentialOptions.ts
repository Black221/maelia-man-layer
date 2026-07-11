import { useQuery } from '@tanstack/react-query'
import { getReferentialOptions } from '../api/dataset.api'

/** Sous-ensemble d'un ParameterSpec nécessaire pour résoudre ses valeurs proposées. */
export interface OptionSpec {
  gamlName: string
  optionsDataSpec: string | null
  optionsColumn?: string | null
  optionsSource?: string | null
}

/**
 * Résout les valeurs disponibles pour les champs `referencesDataSpec` (grille de données, FK).
 * Pour chaque DataSpec référencé, on interroge l'endpoint référentiel (colonne = 1er champ / ID)
 * qui lit les données saisies du projet sinon le socle (CSV et SHP/DBF).
 *
 * Retourne { [dataSpecId]: string[] }.
 */
export function useReferentialOptions(projectId: string | undefined, refSpecIds: string[]) {
  const key = [...new Set(refSpecIds)].sort()
  return useQuery({
    queryKey: ['referential-options', projectId, key],
    enabled: !!projectId && key.length > 0,
    staleTime: 30_000,
    queryFn: async (): Promise<Record<string, string[]>> => {
      const entries = await Promise.all(
        key.map(async (refId) => [refId, await safeOptions(projectId!, refId)] as const),
      )
      return Object.fromEntries(entries)
    },
  })
}

/**
 * Résout les valeurs proposées des paramètres de scénario « select depuis données ».
 * Chaque spec porte optionsDataSpec (+ optionsColumn / optionsSource). On déduplique par le triplet
 * (dataSpec|column|source) pour ne pas requêter deux fois, puis on remappe par gamlName.
 *
 * Retourne { [gamlName]: string[] }.
 */
export function useParamOptions(projectId: string | undefined, specs: OptionSpec[]) {
  const targets = specs.filter((s) => !!s.optionsDataSpec)
  const requests = new Map<string, { dataSpec: string; column?: string | null; source?: string | null }>()
  for (const s of targets) {
    const k = `${s.optionsDataSpec}|${s.optionsColumn ?? ''}|${s.optionsSource ?? ''}`
    if (!requests.has(k)) requests.set(k, { dataSpec: s.optionsDataSpec!, column: s.optionsColumn, source: s.optionsSource })
  }
  const reqKey = [...requests.keys()].sort()

  return useQuery({
    queryKey: ['param-options', projectId, reqKey],
    enabled: !!projectId && reqKey.length > 0,
    staleTime: 30_000,
    queryFn: async (): Promise<Record<string, string[]>> => {
      const byRequest = new Map<string, string[]>()
      await Promise.all(
        [...requests.entries()].map(async ([k, r]) => {
          byRequest.set(k, await safeOptions(projectId!, r.dataSpec, r.column, r.source))
        }),
      )
      const result: Record<string, string[]> = {}
      for (const s of targets) {
        const k = `${s.optionsDataSpec}|${s.optionsColumn ?? ''}|${s.optionsSource ?? ''}`
        result[s.gamlName] = byRequest.get(k) ?? []
      }
      return result
    },
  })
}

async function safeOptions(
  projectId: string,
  dataSpec: string,
  column?: string | null,
  source?: string | null,
): Promise<string[]> {
  try {
    return await getReferentialOptions(projectId, dataSpec, column, source)
  } catch {
    return []
  }
}
