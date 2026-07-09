import { useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Loader2, AlertCircle, BarChart3 } from 'lucide-react'
import { queryKeys } from '@/shared/api'
import type { ResultSeries } from '@/entities/result'
import { getRunResults } from '../api/result.api'
import { TimeSeriesChart } from './TimeSeriesChart'
import { ArtifactGallery } from './ArtifactGallery'

function stats(s: ResultSeries) {
  const v = s.points.map((p) => p.value)
  if (v.length === 0) return null
  const sum = v.reduce((a, b) => a + b, 0)
  return { min: Math.min(...v), max: Math.max(...v), mean: sum / v.length, last: v[v.length - 1], n: v.length }
}

const fmt = (v: number) =>
  Math.abs(v) >= 1000 || (v !== 0 && Math.abs(v) < 0.01) ? v.toExponential(2) : v.toFixed(2)

export function ResultDashboard({ runId }: { runId: string }) {
  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.runs.results(runId),
    queryFn: () => getRunResults(runId),
  })

  const [mode, setMode] = useState<'daily' | 'yearly'>('daily')
  const [indicator, setIndicator] = useState<string | null>(null)

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 py-10 text-neutral-400">
        <Loader2 size={16} className="animate-spin" /> Chargement des résultats…
      </div>
    )
  }
  if (isError || !data) {
    return (
      <div className="flex items-center gap-2 py-6 text-danger">
        <AlertCircle size={16} /> Impossible de charger les résultats.
      </div>
    )
  }

  const hasData = data.indicators.length > 0 || data.artifacts.length > 0
  if (!hasData) {
    return (
      <div className="rounded-xl border border-neutral-200 bg-white p-8 text-center space-y-1.5">
        <BarChart3 size={22} className="mx-auto text-neutral-300" />
        <p className="text-sm font-medium text-neutral-700">Aucun résultat ingéré</p>
        <p className="text-xs text-neutral-500">
          La simulation n&apos;a pas (encore) produit de sortie exploitable dans son répertoire de travail.
        </p>
      </div>
    )
  }

  const seriesSet = mode === 'yearly' ? data.yearly : data.series
  const selected = indicator ?? data.indicators[0] ?? null
  const shown = seriesSet.filter((s) => s.indicator === selected)

  return (
    <div className="space-y-6">
      {/* Indicateurs (séries temporelles) */}
      {data.indicators.length > 0 && (
        <section className="space-y-3">
          <div className="flex items-center justify-between gap-3 flex-wrap">
            <div className="flex items-center gap-2">
              <label className="text-xs font-medium text-neutral-500">Indicateur</label>
              <select
                value={selected ?? ''}
                onChange={(e) => setIndicator(e.target.value)}
                className="rounded border border-neutral-200 px-2 py-1 text-sm bg-white focus:outline-none focus:border-primary max-w-[280px]"
              >
                {data.indicators.map((i) => (
                  <option key={i} value={i}>{i}</option>
                ))}
              </select>
            </div>
            <div className="flex rounded-lg border border-neutral-200 overflow-hidden text-xs">
              {(['daily', 'yearly'] as const).map((m) => (
                <button
                  key={m}
                  onClick={() => setMode(m)}
                  disabled={m === 'yearly' && data.yearly.length === 0}
                  className={[
                    'px-3 py-1.5 font-medium transition-colors disabled:opacity-40',
                    mode === m ? 'bg-primary text-white' : 'bg-white text-neutral-600 hover:bg-neutral-50',
                  ].join(' ')}
                >
                  {m === 'daily' ? 'Journalier' : 'Annuel'}
                </button>
              ))}
            </div>
          </div>

          {shown.length === 0 ? (
            <p className="text-sm text-neutral-400">Pas de série pour cet indicateur en vue {mode === 'yearly' ? 'annuelle' : 'journalière'}.</p>
          ) : (
            shown.map((s) => {
              const st = stats(s)
              return (
                <div key={`${s.indicator}-${s.zone ?? 'global'}`} className="rounded-xl border border-neutral-200 bg-white p-4 space-y-2">
                  <div className="flex items-baseline justify-between gap-2">
                    <h4 className="text-sm font-medium text-neutral-800">
                      {s.indicator}
                      {s.zone && <span className="ml-1.5 text-xs font-normal text-neutral-400">· {s.zone}</span>}
                      {s.unit && <span className="ml-1 text-xs font-normal text-neutral-400">({s.unit})</span>}
                    </h4>
                    {st && (
                      <span className="text-[11px] text-neutral-400">
                        min {fmt(st.min)} · moy {fmt(st.mean)} · max {fmt(st.max)} · {st.n} pts
                      </span>
                    )}
                  </div>
                  <TimeSeriesChart series={s} />
                </div>
              )
            })
          )}
        </section>
      )}

      {/* Artefacts (snapshots, CSV, XML) */}
      <section className="space-y-3">
        <h3 className="text-sm font-medium text-neutral-700">Artefacts de sortie</h3>
        <ArtifactGallery artifacts={data.artifacts} />
      </section>
    </div>
  )
}
