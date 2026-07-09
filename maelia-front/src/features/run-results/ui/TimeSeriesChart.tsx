import { useMemo } from 'react'
import type { ResultSeries } from '@/entities/result'

interface TimeSeriesChartProps {
  series: ResultSeries
  height?: number
  color?: string
}

const W = 600
const PAD = { top: 12, right: 14, bottom: 26, left: 48 }

/**
 * Graphe de série temporelle léger en SVG (aucune dépendance externe).
 * Axe X = date ou cycle ; axe Y = valeur (auto-échelle).
 */
export function TimeSeriesChart({ series, height = 200, color = '#2563eb' }: TimeSeriesChartProps) {
  const pts = series.points
  const chart = useMemo(() => {
    if (pts.length === 0) return null
    const values = pts.map((p) => p.value)
    let min = Math.min(...values)
    let max = Math.max(...values)
    if (min === max) { min -= 1; max += 1 }
    const span = max - min
    const innerW = W - PAD.left - PAD.right
    const innerH = height - PAD.top - PAD.bottom
    const x = (i: number) => PAD.left + (pts.length === 1 ? innerW / 2 : (i / (pts.length - 1)) * innerW)
    const y = (v: number) => PAD.top + innerH - ((v - min) / span) * innerH
    const path = pts.map((p, i) => `${i === 0 ? 'M' : 'L'}${x(i).toFixed(1)},${y(p.value).toFixed(1)}`).join(' ')
    const label = (p: (typeof pts)[number]) => p.date ?? (p.cycle != null ? `#${p.cycle}` : '')
    return {
      path, x, y, min, max,
      first: label(pts[0]),
      last: label(pts[pts.length - 1]),
      // n'affiche les points que si la série est courte (lisibilité)
      dots: pts.length <= 60,
    }
  }, [pts, height])

  if (!chart) {
    return <p className="text-xs text-neutral-400 py-6 text-center">Aucun point à tracer.</p>
  }

  const fmt = (v: number) =>
    Math.abs(v) >= 1000 || (v !== 0 && Math.abs(v) < 0.01) ? v.toExponential(1) : v.toFixed(2)

  return (
    <svg viewBox={`0 0 ${W} ${height}`} className="w-full" role="img" aria-label={`Série ${series.indicator}`}>
      {/* grille horizontale + libellés Y (min, milieu, max) */}
      {[0, 0.5, 1].map((t) => {
        const v = chart.min + (chart.max - chart.min) * t
        const yy = chart.y(v)
        return (
          <g key={t}>
            <line x1={PAD.left} y1={yy} x2={W - PAD.right} y2={yy} stroke="#f1f5f9" />
            <text x={PAD.left - 6} y={yy + 3} textAnchor="end" className="fill-neutral-400" fontSize="9">
              {fmt(v)}
            </text>
          </g>
        )
      })}

      {/* courbe */}
      <path d={chart.path} fill="none" stroke={color} strokeWidth="1.6" />
      {chart.dots &&
        pts.map((p, i) => (
          <circle key={i} cx={chart.x(i)} cy={chart.y(p.value)} r="2" fill={color}>
            <title>{`${p.date ?? `cycle ${p.cycle}`} : ${p.value}`}</title>
          </circle>
        ))}

      {/* libellés X (premier / dernier) */}
      <text x={PAD.left} y={height - 8} textAnchor="start" className="fill-neutral-400" fontSize="9">
        {chart.first}
      </text>
      <text x={W - PAD.right} y={height - 8} textAnchor="end" className="fill-neutral-400" fontSize="9">
        {chart.last}
      </text>
    </svg>
  )
}
