import { useMemo } from 'react'

export interface ChartSeries {
  name: string
  color: string
  /** Points (année → valeur). Les années absentes créent une coupure. */
  points: { x: number; y: number }[]
}

interface MultiLineChartProps {
  series: ChartSeries[]
  height?: number
  unit?: string | null
  /** Format des valeurs (axe Y + tooltips). */
  format?: (v: number) => string
}

const W = 640
const PAD = { top: 14, right: 16, bottom: 30, left: 52 }

const defaultFmt = (v: number) =>
  Math.abs(v) >= 1000 || (v !== 0 && Math.abs(v) < 0.01) ? v.toExponential(1) : v.toFixed(2)

/**
 * Graphe multi-séries léger en SVG (aucune dépendance). X = année (ordinal), Y = valeur.
 * Une ligne + points par série, légende intégrée. Adapté « rendement par culture au cours du temps ».
 */
export function MultiLineChart({ series, height = 240, unit, format = defaultFmt }: MultiLineChartProps) {
  const chart = useMemo(() => {
    const allYears = Array.from(new Set(series.flatMap((s) => s.points.map((p) => p.x)))).sort((a, b) => a - b)
    const allVals = series.flatMap((s) => s.points.map((p) => p.y))
    if (allYears.length === 0 || allVals.length === 0) return null

    let min = Math.min(0, ...allVals)
    let max = Math.max(...allVals)
    if (min === max) { min -= 1; max += 1 }
    const span = max - min
    const innerW = W - PAD.left - PAD.right
    const innerH = height - PAD.top - PAD.bottom
    const xOf = (year: number) =>
      PAD.left + (allYears.length === 1 ? innerW / 2 : (allYears.indexOf(year) / (allYears.length - 1)) * innerW)
    const yOf = (v: number) => PAD.top + innerH - ((v - min) / span) * innerH
    return { allYears, min, max, xOf, yOf }
  }, [series, height])

  if (!chart) {
    return <p className="text-xs text-neutral-400 py-8 text-center">Aucune donnée à tracer.</p>
  }

  return (
    <div>
      <svg viewBox={`0 0 ${W} ${height}`} className="w-full" role="img" aria-label="Graphe multi-séries">
        {/* grille + libellés Y */}
        {[0, 0.25, 0.5, 0.75, 1].map((t) => {
          const v = chart.min + (chart.max - chart.min) * t
          const yy = chart.yOf(v)
          return (
            <g key={t}>
              <line x1={PAD.left} y1={yy} x2={W - PAD.right} y2={yy} stroke="#eef2f6" />
              <text x={PAD.left - 6} y={yy + 3} textAnchor="end" className="fill-neutral-400" fontSize="9">
                {format(v)}
              </text>
            </g>
          )
        })}

        {/* libellés X (années) */}
        {chart.allYears.map((yr) => (
          <text key={yr} x={chart.xOf(yr)} y={height - 10} textAnchor="middle" className="fill-neutral-400" fontSize="9">
            {yr}
          </text>
        ))}

        {/* séries */}
        {series.map((s) => {
          const pts = [...s.points].sort((a, b) => a.x - b.x)
          const path = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${chart.xOf(p.x).toFixed(1)},${chart.yOf(p.y).toFixed(1)}`).join(' ')
          return (
            <g key={s.name}>
              <path d={path} fill="none" stroke={s.color} strokeWidth="2" />
              {pts.map((p) => (
                <circle key={p.x} cx={chart.xOf(p.x)} cy={chart.yOf(p.y)} r="3" fill={s.color}>
                  <title>{`${s.name} · ${p.x} : ${format(p.y)}${unit ? ' ' + unit : ''}`}</title>
                </circle>
              ))}
            </g>
          )
        })}
      </svg>

      {/* légende */}
      <div className="mt-2 flex flex-wrap gap-x-4 gap-y-1">
        {series.map((s) => (
          <span key={s.name} className="inline-flex items-center gap-1.5 text-[11px] text-neutral-600">
            <span className="inline-block h-2.5 w-2.5 rounded-sm" style={{ backgroundColor: s.color }} />
            {s.name}
          </span>
        ))}
      </div>
    </div>
  )
}

/** Palette catégorielle (lisible en clair) — inspirée du design system, distincte par teinte. */
export const CATEGORY_COLORS = [
  '#2563eb', '#16a34a', '#ea580c', '#9333ea', '#0891b2',
  '#ca8a04', '#dc2626', '#4f46e5', '#059669', '#db2777',
]

export function colorAt(i: number): string {
  return CATEGORY_COLORS[i % CATEGORY_COLORS.length]
}
