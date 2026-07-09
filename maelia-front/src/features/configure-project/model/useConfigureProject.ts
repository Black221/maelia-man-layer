import { useMutation, useQueryClient } from '@tanstack/react-query'
import { updateModelingConfig } from '../api/configureProject.api'
import { queryKeys } from '@/shared/api'
import type { ModelingConfiguration } from '@/entities/project'

export function useConfigureProject(projectId: string) {
  const queryClient = useQueryClient()

  return useMutation({
    mutationFn: (config: ModelingConfiguration) => updateModelingConfig(projectId, config),
    onSuccess: (project) => {
      queryClient.setQueryData(queryKeys.projects.detail(projectId), project)
      // Invalider la complétude car la config a changé
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.completion(projectId) })
    },
  })
}
