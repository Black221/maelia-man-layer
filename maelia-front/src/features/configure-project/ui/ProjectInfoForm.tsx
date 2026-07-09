import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Save } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { Project } from '@/entities/project'
import { updateProject } from '../api/configureProject.api'

interface ProjectInfoFormProps {
  project: Project
}

/** Édition du nom et de la description du projet (page Initialisation). */
export function ProjectInfoForm({ project }: ProjectInfoFormProps) {
  const queryClient = useQueryClient()
  const [name, setName] = useState(project.name)
  const [description, setDescription] = useState(project.description ?? '')

  const { mutate, isPending, isSuccess } = useMutation({
    mutationFn: () => updateProject(project.id, { name: name.trim(), description: description.trim() || null }),
    onSuccess: (updated) => {
      queryClient.setQueryData(queryKeys.projects.detail(project.id), updated)
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.all })
    },
  })

  return (
    <form
      onSubmit={(e) => { e.preventDefault(); if (name.trim()) mutate() }}
      className="space-y-4"
    >
      <div>
        <label className="block text-sm font-medium text-neutral-700 mb-1">Nom du projet</label>
        <input
          type="text"
          value={name}
          onChange={(e) => setName(e.target.value)}
          maxLength={120}
          required
          className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none
                     focus:border-primary focus:ring-1 focus:ring-primary transition bg-white"
        />
      </div>

      <div>
        <label className="block text-sm font-medium text-neutral-700 mb-1">Description</label>
        <textarea
          value={description}
          onChange={(e) => setDescription(e.target.value)}
          rows={3}
          maxLength={2000}
          placeholder="Objet de l'étude, territoire, hypothèses…"
          className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none
                     focus:border-primary focus:ring-1 focus:ring-primary transition bg-white resize-y"
        />
      </div>

      <div className="flex items-center gap-3">
        <Button variant="primary" size="sm" type="submit" loading={isPending} disabled={!name.trim()}>
          <Save size={14} />
          Enregistrer
        </Button>
        {isSuccess && <span className="text-sm text-success">Projet mis à jour</span>}
      </div>
    </form>
  )
}
