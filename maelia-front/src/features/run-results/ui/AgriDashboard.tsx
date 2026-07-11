import { useMemo, useState } from 'react'
import { useQueries } from '@tanstack/react-query'
import { Loader2, BarChart3 } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import type { RunDashboard, YearPoint } from '@/entities/result'
import { getRunDashboard } from '../api/result.api'
import { MultiLineChart, colorAt, type ChartSeries } from './MultiLineChart'

export interface SelectedRun {
  runId: string
  scenarioName: string
}

/** Regroupement thématique des indicateurs (dans l'ordre d'affichage). */
const THEMES: { key: string; label: string; indicators: string[] }[] = [
  { key: 'rendement', label: 'Rendement & production', indicators: ['RECOLTE_rendement', 'BIOMASSE_export', 'N_export'] },
  { key: 'eau', label: 'Eau', indicators: ['irrigation', 'IRRIGATION_dose', 'IRRIGATION_reelle', 'satisfactionHydrique', 'evaporation', 'transpiration', 'pluie', 'percolation'] },
  { key: 'environnement', label: 'Environnement', indicators: ['bilan_net_GES', 'delta_Corg', 'N_lixivie', 'satisfactionAzote_culture', 'emissions_ferti'] },
  { key: 'fertilisation', label: 'Fertilisation', indicators: ['FERTI_apportNminReel'] },
]

const LABELS: Record<string, string> = {
  RECOLTE_rendement: 'Rendement',
  BIOMASSE_export: 'Biomasse exportée',
  N_export: 'Azote exporté',
  irrigation: 'Irrigation',
  IRRIGATION_dose: 'Dose d’irrigation',
  IRRIGATION_reelle: 'Irrigation réelle',
  satisfactionHydrique: 'Satisfaction hydrique',
  evaporation: 'Évaporation',
  transpiration: 'Transpiration',
  pluie: 'Pluie',
  percolation: 'Percolation',
  bilan_net_GES: 'Bilan net GES',
  delta_Corg: 'Stockage carbone (ΔCorg)',
  N_lixivie: 'Azote lixivié',
  satisfactionAzote_culture: 'Satisfaction azotée',
  emissions_ferti: 'Émissions fertilisants',
  FERTI_apportNminReel: 'Apport N minéral',
}

const fmt = (v: number) =>
  Math.abs(v) >= 1000 || (v !== 0 && Math.abs(v) < 0.01) ? v.toExponential(1) : (Math.round(v * 100) / 100).toString()

function pointValue(p: YearPoint, metric: 'mean' | 'total'): number | null {
  if (metric === 'total') return p.total // null si l'indicateur n'est pas surfacique
  return p.mean
}

export function AgriDashboard({ runs }: { runs: SelectedRun[] }) {
  const [metric, setMetric] = useState<'mean' | 'total'>('mean')

  const results = useQueries({
    queries: runs.map((r) => ({
      queryKey: queryKeys.runs.dashboard(r.runId),
      queryFn: () => getRunDashboard(r.runId),
      staleTime: 60_000,
    })),
  })

  const loading = results.some((q) => q.isLoading)
  const dashboards = results.map((q, i) => ({ run: runs[i], data: q.data as RunDashboard | undefined }))
    .filter((d): d is { run: SelectedRun; data: RunDashboard } => !!d.data)

  const compare = runs.length > 1

  // Indicateurs disponibles (union), unité, et si la production totale est possible.
  const meta = useMemo(() => {
    const unit = new Map<string, string | null>()
    const hasTotal = new Set<string>()
    const present = new Set<string>()
    for (const { data } of dashboards) {
      for (const m of data.indicators) { unit.set(m.indicator, m.unit); present.add(m.indicator) }
      for (const s of data.series) for (const p of s.points) if (p.total != null) hasTotal.add(s.indicator)
    }
    return { unit, hasTotal, present }
  }, [dashboards])

  if (loading) {
    return (
      <div className="flex items-center gap-2 py-10 text-neutral-400">
        <Loader2 size={16} className="animate-spin" /> Chargement du tableau de bord…
      </div>
    )
  }

  if (meta.present.size === 0) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center space-y-1.5">
        <BarChart3 size={22} className="mx-auto text-neutral-300" />
        <p className="text-sm font-medium text-neutral-700">Aucun résultat agronomique</p>
        <p className="text-xs text-neutral-500">
          Le(s) run(s) sélectionné(s) n’ont pas produit de sortie exploitable (rendement, eau, environnement).
          Un run avec module agricole produisant des récoltes est nécessaire.
        </p>
      </div>
    )
  }

  const totalPossible = metric === 'total' // le toggle est global ; fallback mean si total absent

  /** Construit les séries d'un indicateur pour le graphe. */
  function seriesFor(indicator: string): ChartSeries[] {
    if (compare) {
      // Comparaison : une ligne par scénario, moyenne (ou somme des totaux) sur les cultures / année.
      return dashboards.map((d, i) => {
        const byYear = new Map<number, { sum: number; n: number }>()
        for (const s of d.data.series) {
          if (s.indicator !== indicator) continue
          for (const p of s.points) {
            const v = pointValue(p, totalPossible && meta.hasTotal.has(indicator) ? 'total' : 'mean')
            if (v == null) continue
            const acc = byYear.get(p.year) ?? { sum: 0, n: 0 }
            // total → somme ; mean → moyenne
            acc.sum += v; acc.n += 1; byYear.set(p.year, acc)
          }
        }
        const useTotal = totalPossible && meta.hasTotal.has(indicator)
        const points = [...byYear.entries()].map(([year, a]) => ({ x: year, y: useTotal ? a.sum : a.sum / a.n }))
        return { name: d.run.scenarioName, color: colorAt(i), points }
      }).filter((s) => s.points.length > 0)
    }
    // Vue simple : une ligne par culture.
    const d = dashboards[0]
    const useTotal = totalPossible && meta.hasTotal.has(indicator)
    return d.data.series
      .filter((s) => s.indicator === indicator)
      .map((s, i) => ({
        name: s.category ?? 'global',
        color: colorAt(i),
        points: s.points
          .map((p) => ({ x: p.year, y: pointValue(p, useTotal ? 'total' : 'mean') }))
          .filter((p): p is { x: number; y: number } => p.y != null),
      }))
      .filter((s) => s.points.length > 0)
  }

  return (
    <div className="space-y-6">
      {/* Barre de contrôle */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <p className="text-[13px] text-neutral-500">
          {compare
            ? `Comparaison de ${runs.length} scénarios`
            : <>Scénario : <span className="font-medium text-neutral-800">{runs[0]?.scenarioName}</span></>}
        </p>
        <div className="flex rounded-lg border border-neutral-200 overflow-hidden text-xs">
          {(['mean', 'total'] as const).map((m) => (
            <button
              key={m}
              onClick={() => setMetric(m)}
              className={[
                'px-3 py-1.5 font-medium transition-colors',
                metric === m ? 'bg-primary text-white' : 'bg-white text-neutral-600 hover:bg-neutral-50',
              ].join(' ')}
            >
              {m === 'mean' ? 'Moyenne (par ha)' : 'Production totale'}
            </button>
          ))}
        </div>
      </div>

      {THEMES.map((theme) => {
        const inds = theme.indicators.filter((i) => meta.present.has(i))
        if (inds.length === 0) return null
        return (
          <section key={theme.key} className="space-y-3">
            <h3 className="text-sm font-semibold text-neutral-700">{theme.label}</h3>
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-4">
              {inds.map((ind) => {
                const s = seriesFor(ind)
                if (s.length === 0) return null
                const unit = meta.unit.get(ind)
                const showingTotal = metric === 'total' && meta.hasTotal.has(ind)
                const shownUnit = showingTotal ? productionUnit(unit) : unit
                return (
                  <div key={ind} className="rounded-xl border border-neutral-200 bg-white p-4 space-y-1">
                    <div className="flex items-baseline justify-between gap-2">
                      <h4 className="text-sm font-medium text-neutral-800">{LABELS[ind] ?? ind}</h4>
                      {shownUnit && <span className="text-[11px] text-neutral-400">{shownUnit}</span>}
                    </div>
                    <MultiLineChart series={s} unit={shownUnit} format={fmt} />
                  </div>
                )
              })}
            </div>
          </section>
        )
      })}
    </div>
  )
}

/** Unité de production totale déduite de l'unité surfacique (t/ha → t, kg/ha → kg). */
function productionUnit(unit: string | null | undefined): string | null {
  if (!unit) return null
  const i = unit.toLowerCase().indexOf('/ha')
  return i > 0 ? unit.slice(0, i) : unit
}
