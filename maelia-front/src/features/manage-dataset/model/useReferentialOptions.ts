import { useQuery } from '@tanstack/react-query'
import { listProjectDatasets, getDataset, getDataSpec } from '../api/dataset.api'

/**
 * Résout les valeurs disponibles pour les champs `referencesDataSpec` (M3 : sélecteurs référentiels).
 * Pour chaque DataSpec référencé, on cherche le dataset du projet correspondant et on extrait
 * les valeurs distinctes de sa colonne identifiante (1er champ du DataSpec référencé).
 *
 * Retourne une map { [dataSpecIdRéférencé]: string[] }. Vide si le référentiel n'est pas (encore) saisi.
 */
export function useReferentialOptions(projectId: string | undefined, refSpecIds: string[]) {
  const key = [...new Set(refSpecIds)].sort()
  return useQuery({
    queryKey: ['referential-options', projectId, key],
    enabled: !!projectId && key.length > 0,
    staleTime: 30_000,
    queryFn: async (): Promise<Record<string, string[]>> => {
      const datasets = await listProjectDatasets(projectId!)
      const bySpec = new Map(datasets.map((d) => [d.dataSpecId, d]))
      const result: Record<string, string[]> = {}
      await Promise.all(
        key.map(async (refId) => {
          const summary = bySpec.get(refId)
          if (!summary) {
            result[refId] = []
            return
          }
          const [full, spec] = await Promise.all([getDataset(summary.id), getDataSpec(refId)])
          const keyLabel = spec.fields?.[0]?.label
          if (!keyLabel) {
            result[refId] = []
            return
          }
          const values = Array.from(
            new Set(
              full.records
                .map((r) => r[keyLabel])
                .filter((v) => v != null && v !== '')
                .map(String),
            ),
          )
          result[refId] = values
        }),
      )
      return result
    },
  })
}
