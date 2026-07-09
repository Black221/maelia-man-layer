import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { createProject } from '../api/createProject.api'
import { queryKeys } from '@/shared/api'
import { toast } from '@/shared/ui'

export function useCreateProject(onSuccess?: () => void) {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: createProject,
    onSuccess: (project) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.all })
      // Pré-remplit le cache détail pour éviter un flash de chargement à l'ouverture.
      queryClient.setQueryData(queryKeys.projects.detail(project.id), project)
      toast.success(`Projet « ${project.name} » créé.`)
      onSuccess?.()
      navigate(`/projects/${project.id}`)
    },
  })
}
