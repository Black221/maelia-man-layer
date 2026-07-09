import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { FileText, Loader2, Lock, Save, Sparkles } from 'lucide-react'
import { Button, Modal } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { ModelingConfiguration } from '@/entities/project'
import type { DataSpec } from '@/entities/dataset'
import { getApplicableDataSpecs } from '../api/configureProject.api'
import { useConfigureProject } from '../model/useConfigureProject'

const MODULES = [
  { value: 'agricole', label: 'Module agricole' },
  { value: 'hydrographique', label: 'Module hydrographique' },
  { value: 'normatif', label: 'Module normatif' },
  { value: 'usages', label: 'Module usages' },
  { value: 'barrage', label: 'Module barrage' },
]

const MODULE_LABELS: Record<string, string> = {
  COMMUN: 'Commun',
  AGRICOLE: 'Agricole',
  HYDROGRAPHIQUE: 'Hydrologique',
  NORMATIF: 'Normatif',
}
const MODULE_ORDER = ['COMMUN', 'AGRICOLE', 'HYDROGRAPHIQUE', 'NORMATIF']

interface ModelingConfigFormProps {
  projectId: string
  initialConfig: ModelingConfiguration
}

/**
 * Choix des modules de simulation. Les autres options de modélisation (assolement,
 * irrigation, modèle de culture, restriction) se règlent dans les paramètres de scénario.
 * Les fichiers nécessaires pour la sélection en cours sont consultables dans un dialogue,
 * pour ne pas alourdir la page.
 */
export function ModelingConfigForm({ projectId, initialConfig }: ModelingConfigFormProps) {
  const [config, setConfig] = useState<ModelingConfiguration>(initialConfig)
  const [showFiles, setShowFiles] = useState(false)
  const { mutate, isPending, isSuccess } = useConfigureProject(projectId)

  const toggleModule = (mod: string) => {
    setConfig((prev) => ({
      ...prev,
      modules: prev.modules.includes(mod)
        ? prev.modules.filter((m) => m !== mod)
        : [...prev.modules, mod],
    }))
  }

  // Aperçu en direct : fichiers applicables pour la configuration en cours d'édition.
  const { data: applicable, isFetching } = useQuery({
    queryKey: queryKeys.dataspecs.forConfig([...config.modules].sort().join('+') || 'aucun'),
    queryFn: () => getApplicableDataSpecs(config),
    staleTime: 5 * 60 * 1000,
  })

  const requiredCount = applicable?.filter((s) => s.generation === 'MANUAL' && s.required).length ?? 0

  return (
    <div className="space-y-4">
      <form
        onSubmit={(e) => { e.preventDefault(); mutate(config) }}
        className="space-y-4"
      >
        <div>
          <p className="text-sm font-medium text-neutral-700 mb-2">Modules activés</p>
          <div className="flex flex-wrap gap-2">
            {MODULES.map(({ value, label }) => (
              <button
                key={value}
                type="button"
                onClick={() => toggleModule(value)}
                className={`px-3 py-1.5 rounded-full text-sm font-medium border transition-colors ${
                  config.modules.includes(value)
                    ? 'bg-primary text-white border-primary'
                    : 'bg-white text-neutral-600 border-neutral-300 hover:border-primary'
                }`}
              >
                {label}
              </button>
            ))}
          </div>
          <p className="text-xs text-neutral-400 mt-2">
            Les autres options de modélisation (assolement, irrigation, modèle de culture,
            restriction) se règlent dans les paramètres du scénario.
          </p>
        </div>

        <div className="flex items-center gap-3 flex-wrap">
          <Button variant="primary" size="sm" type="submit" loading={isPending}>
            <Save size={14} />
            Enregistrer les modules
          </Button>
          <button
            type="button"
            onClick={() => setShowFiles(true)}
            className="inline-flex items-center gap-1.5 text-sm font-medium text-primary-700 hover:text-primary-900 transition-colors"
          >
            <FileText size={14} />
            Voir les fichiers nécessaires
            {isFetching ? (
              <Loader2 size={12} className="animate-spin" />
            ) : applicable ? (
              <span className="text-xs text-neutral-400">
                ({applicable.length}, {requiredCount} à saisir)
              </span>
            ) : null}
          </button>
          {isSuccess && <span className="text-sm text-success">Modules sauvegardés</span>}
        </div>
      </form>

      <Modal
        open={showFiles}
        onClose={() => setShowFiles(false)}
        title="Fichiers nécessaires pour cette sélection"
        size="lg"
      >
        <FilesPreview applicable={applicable} isFetching={isFetching} />
      </Modal>
    </div>
  )
}

function FilesPreview({ applicable, isFetching }: { applicable: DataSpec[] | undefined; isFetching: boolean }) {
  if (isFetching && !applicable) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-500 py-6">
        <Loader2 size={14} className="animate-spin" />
        Chargement…
      </div>
    )
  }

  const byModule = (applicable ?? []).reduce<Record<string, DataSpec[]>>((acc, spec) => {
    ;(acc[spec.module] ??= []).push(spec)
    return acc
  }, {})
  const moduleKeys = MODULE_ORDER.filter((m) => byModule[m]?.length)

  if (moduleKeys.length === 0) {
    return <p className="text-sm text-neutral-400">Aucun fichier applicable.</p>
  }

  return (
    <div className="space-y-4">
      <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
        {moduleKeys.map((mod) => (
          <div key={mod} className="rounded-lg border border-neutral-100 p-3">
            <p className="text-[11px] font-semibold uppercase tracking-wider text-neutral-400 mb-2">
              {MODULE_LABELS[mod] ?? mod} · {byModule[mod].length}
            </p>
            <ul className="space-y-1">
              {byModule[mod].map((spec) => (
                <li key={spec.id} className="flex items-center gap-2 text-[12px]">
                  <span className="font-mono text-neutral-700 truncate">{spec.fileName}</span>
                  {spec.required && (
                    <Lock size={10} className="shrink-0 text-warning" aria-label="Obligatoire" />
                  )}
                  {spec.generation === 'AUTO' && (
                    <Sparkles
                      size={10}
                      className="shrink-0 text-primary-400"
                      aria-label="Généré par le prétraitement"
                    />
                  )}
                </li>
              ))}
            </ul>
          </div>
        ))}
      </div>
      <p className="text-[11px] text-neutral-400 flex items-center gap-3">
        <span className="inline-flex items-center gap-1"><Lock size={10} className="text-warning" /> obligatoire</span>
        <span className="inline-flex items-center gap-1"><Sparkles size={10} className="text-primary-400" /> généré par le prétraitement</span>
      </p>
    </div>
  )
}
