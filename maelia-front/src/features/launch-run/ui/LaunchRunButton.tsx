import { Play } from 'lucide-react'
import { Button } from '@/shared/ui'
import { useLaunchRun } from '../model/useLaunchRun'

export function LaunchRunButton() {
  const { mutate, isPending } = useLaunchRun()

  return (
    <Button
      variant="primary"
      size="md"
      loading={isPending}
      onClick={() => mutate(undefined)}
    >
      <Play size={15} />
      Lancer une simulation
    </Button>
  )
}
