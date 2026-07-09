import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { launchRun } from '../api/launchRun.api'
import { queryKeys } from '@/shared/api'

export function useLaunchRun() {
  const queryClient = useQueryClient()
  const navigate = useNavigate()

  return useMutation({
    mutationFn: launchRun,
    onSuccess: (run) => {
      // Précache le run retourné (statut EN_FILE)
      queryClient.setQueryData(queryKeys.runs.detail(run.id), run)
      // Naviguer vers le moniteur de run
      navigate(`/runs/${run.id}`)
    },
  })
}
