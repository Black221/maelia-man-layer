import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { useNavigate } from 'react-router-dom'
import { FlaskConical, Play, Radio, Activity, CheckCircle2, Server, SlidersHorizontal } from 'lucide-react'
import { Button } from '@/shared/ui'
import { launchTestRun, launchMaeliaTestRun } from '@/features/launch-run'
import { queryKeys } from '@/shared/api'

const STEPS = [
  { icon: Server, label: 'Backend met le run en file (RabbitMQ)' },
  { icon: Radio, label: 'Le worker ouvre un WebSocket vers GAMA headless' },
  { icon: Activity, label: 'GAMA exécute le modèle et pousse la progression en temps réel' },
  { icon: CheckCircle2, label: 'Le run se termine et le cycle final s’affiche' },
]

/**
 * Paramètres de l'expérience test_simulation (simple_test.gaml).
 * `name` = nom de la variable GAML ; les défauts reflètent ceux du modèle.
 * À l'init, le modèle écrit les valeurs reçues dans le journal du run :
 * on peut donc vérifier visuellement que les paramètres saisis sont appliqués.
 */
const TEST_PARAMS = [
  { name: 'nb_people', label: 'Taille de la population', default: 200, min: 10, max: 2000, step: 1 },
  { name: 'nb_steps', label: 'Nombre de pas (arrêt)', default: 100, min: 10, max: 1000, step: 1 },
  { name: 'infection_distance', label: "Distance d'infection", default: 2.0, min: 0.5, max: 10, step: 0.5 },
  { name: 'infection_proba', label: "Probabilité d'infection", default: 0.05, min: 0, max: 1, step: 0.01 },
  { name: 'people_speed', label: 'Vitesse des agents', default: 2.0, min: 0.5, max: 10, step: 0.5 },
] as const

export function SimulationTestPage() {
  const navigate = useNavigate()
  const queryClient = useQueryClient()

  const [values, setValues] = useState<Record<string, number>>(() =>
    Object.fromEntries(TEST_PARAMS.map((p) => [p.name, p.default])),
  )

  const onLaunched = (run: { id: string }) => {
    queryClient.setQueryData(queryKeys.runs.detail(run.id), run)
    navigate(`/runs/${run.id}`)
  }

  const launch = useMutation({ mutationFn: () => launchTestRun(values), onSuccess: onLaunched })
  const launchMaelia = useMutation({ mutationFn: () => launchMaeliaTestRun(), onSuccess: onLaunched })

  return (
    <div className="max-w-2xl mx-auto px-4 py-10 space-y-8">
      <div className="flex items-start gap-4">
        <div className="flex h-12 w-12 shrink-0 items-center justify-center rounded-2xl bg-primary-50 text-primary-600">
          <FlaskConical size={24} />
        </div>
        <div>
          <h1 className="text-xl font-semibold text-neutral-900">Simulation de test</h1>
          <p className="text-sm text-neutral-500 mt-1">
            Lance un <strong>modèle GAMA autonome</strong> (propagation d’une infection sur des agents
            mobiles), totalement indépendant de MAELIA et sans aucune donnée d’entrée. Sert à vérifier
            que la communication front ↔ back ↔ GAMA fonctionne de bout en bout.
          </p>
        </div>
      </div>

      <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm space-y-4">
        <p className="text-[13px] font-medium text-neutral-700">Ce que ce test valide</p>
        <ol className="space-y-3">
          {STEPS.map(({ icon: Icon, label }, i) => (
            <li key={i} className="flex items-center gap-3">
              <span className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-neutral-100 text-neutral-500">
                <Icon size={15} />
              </span>
              <span className="text-[13px] text-neutral-600">{label}</span>
            </li>
          ))}
        </ol>
      </div>

      <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm space-y-4">
        <div className="flex items-center gap-2">
          <SlidersHorizontal size={15} className="text-neutral-500" />
          <p className="text-[13px] font-medium text-neutral-700">Paramètres de la simulation</p>
        </div>
        <p className="text-[13px] text-neutral-500">
          Ces valeurs sont envoyées à GAMA au chargement du modèle. Le journal du run affiche à
          l’init les paramètres reçus : vérifiez qu’ils correspondent à votre saisie.
        </p>
        <div className="grid grid-cols-2 gap-4 sm:grid-cols-3">
          {TEST_PARAMS.map((p) => (
            <label key={p.name} className="space-y-1">
              <span className="block text-xs font-medium text-neutral-600">{p.label}</span>
              <input
                type="number"
                min={p.min}
                max={p.max}
                step={p.step}
                value={values[p.name]}
                onChange={(e) =>
                  setValues((v) => ({ ...v, [p.name]: e.target.valueAsNumber }))
                }
                className="w-full rounded-lg border border-neutral-300 px-3 py-1.5 text-sm text-neutral-900 focus:border-primary-500 focus:outline-none focus:ring-1 focus:ring-primary-500"
              />
              <span className="block text-[11px] text-neutral-400 font-mono">{p.name}</span>
            </label>
          ))}
        </div>
      </div>

      <div className="flex items-center gap-3">
        <Button variant="primary" size="md" onClick={() => launch.mutate()} loading={launch.isPending}>
          <Play size={16} /> Lancer la simulation de test
        </Button>
        <span className="text-xs text-neutral-400 font-mono">test/models/simple_test.gaml</span>
      </div>

      {launch.isError && (
        <p className="text-sm text-danger">
          Échec du lancement : {(launch.error as { detail?: string; message?: string })?.detail
            ?? (launch.error as Error)?.message
            ?? 'erreur inconnue'}. Vérifiez que GAMA headless est démarré (docker compose).
        </p>
      )}

      <div className="rounded-2xl border border-neutral-200 bg-white p-5 shadow-sm space-y-3">
        <p className="text-[13px] font-medium text-neutral-700">Test du modèle MAELIA réel</p>
        <p className="text-[13px] text-neutral-600">
          Lance le <strong>vrai modèle MAELIA</strong> via <span className="font-mono">launcherTest.gaml</span> sur
          les includes de base (échantillon SASSEME), sans gestion de projet. Plus exigeant : nécessite
          que les données d’entrée soient présentes dans <span className="font-mono">gama-workspace/maelia/includes/</span>.
        </p>
        <div className="flex items-center gap-3">
          <Button variant="secondary" size="md" onClick={() => launchMaelia.mutate()} loading={launchMaelia.isPending}>
            <Play size={16} /> Lancer MAELIA (test)
          </Button>
          <span className="text-xs text-neutral-400 font-mono">maelia/models/main/launcherTest.gaml</span>
        </div>
        {launchMaelia.isError && (
          <p className="text-sm text-danger">
            Échec du lancement : {(launchMaelia.error as { detail?: string; message?: string })?.detail
              ?? (launchMaelia.error as Error)?.message
              ?? 'erreur inconnue'}.
          </p>
        )}
      </div>
    </div>
  )
}
