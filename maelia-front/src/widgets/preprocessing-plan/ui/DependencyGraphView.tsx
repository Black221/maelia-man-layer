import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { AlertTriangle, Loader2, Sparkles } from 'lucide-react'
import { getDependencyGraph } from '@/features/preprocessing'
import { queryKeys } from '@/shared/api'
import type { DependencyNode } from '@/entities/preprocessing'

const MODULE_LABELS: Record<string, string> = {
  COMMUN: 'Commun',
  AGRICOLE: 'Agricole',
  HYDROGRAPHIQUE: 'Hydrologique',
  NORMATIF: 'Normatif',
}

// Couleurs de module (distinctes des couleurs sémantiques primary/success/warning/danger).
const MODULE_COLORS: Record<string, string> = {
  COMMUN: '#94A3B8',         // neutral-400
  AGRICOLE: '#10B981',       // emerald-500
  HYDROGRAPHIQUE: '#0EA5E9', // sky-500
  NORMATIF: '#8B5CF6',       // violet-500
}

const NODE_WIDTH = 176
const NODE_HEIGHT = 36
const COLUMN_WIDTH = 220
const ROW_HEIGHT = 48
const PADDING_X = 24
const PADDING_TOP = 28
const PADDING_BOTTOM = 16

/**
 * Graphe complet des dépendances entre TOUS les fichiers du catalogue (indépendant
 * des modules activés par un projet). Colonnes = niveaux topologiques (ordre de
 * génération), lignes = fichiers de ce niveau. Survoler un fichier met en évidence
 * ses dépendances directes.
 */
export function DependencyGraphView() {
  const [hoveredId, setHoveredId] = useState<string | null>(null)

  const { data, isLoading, isError } = useQuery({
    queryKey: queryKeys.preprocessing.graph,
    queryFn: getDependencyGraph,
  })

  const layout = useMemo(() => {
    if (!data) return null
    const levels = Array.from(new Set(data.nodes.map((n) => n.level)))
      .sort((a, b) => (a === -1 ? 1 : b === -1 ? -1 : a - b))

    const nodesByLevel = new Map<number, DependencyNode[]>()
    for (const level of levels) {
      nodesByLevel.set(
        level,
        data.nodes
          .filter((n) => n.level === level)
          .sort((a, b) => a.module.localeCompare(b.module) || a.dataSpecId.localeCompare(b.dataSpecId)),
      )
    }

    const positions = new Map<string, { x: number; y: number }>()
    levels.forEach((level, levelIndex) => {
      nodesByLevel.get(level)!.forEach((n, rowIndex) => {
        positions.set(n.dataSpecId, {
          x: PADDING_X + levelIndex * COLUMN_WIDTH,
          y: PADDING_TOP + rowIndex * ROW_HEIGHT,
        })
      })
    })

    const maxRows = Math.max(1, ...levels.map((l) => nodesByLevel.get(l)!.length))
    const width = PADDING_X + levels.length * COLUMN_WIDTH
    const height = PADDING_TOP + maxRows * ROW_HEIGHT + PADDING_BOTTOM

    return { levels, nodesByLevel, positions, width, height }
  }, [data])

  if (isLoading) {
    return (
      <div className="flex items-center gap-2 text-sm text-neutral-500 py-6">
        <Loader2 size={14} className="animate-spin" />
        Chargement du graphe…
      </div>
    )
  }

  if (isError || !data || !layout) {
    return <p className="text-sm text-danger">Impossible de charger le graphe de dépendances.</p>
  }

  // Fichiers mis en évidence au survol : le nœud lui-même + ses dépendances directes.
  const highlighted = new Set<string>()
  if (hoveredId) {
    highlighted.add(hoveredId)
    const node = data.nodes.find((n) => n.dataSpecId === hoveredId)
    node?.dependsOn.forEach((id) => highlighted.add(id))
    node?.requiredBy.forEach((id) => highlighted.add(id))
  }

  return (
    <div className="space-y-3">
      {data.hasCycle && (
        <div className="flex items-start gap-2 rounded-lg bg-warning/10 px-3 py-2 text-[12px] text-warning">
          <AlertTriangle size={14} className="mt-0.5 shrink-0" />
          <span>Dépendances circulaires détectées : {data.cycleIds.join(', ')}.</span>
        </div>
      )}
      {data.unknownReferences.length > 0 && (
        <div className="flex items-start gap-2 rounded-lg bg-neutral-100 px-3 py-2 text-[12px] text-neutral-600">
          <AlertTriangle size={14} className="mt-0.5 shrink-0 text-neutral-400" />
          <span>Références vers des fichiers inconnus : {data.unknownReferences.join(', ')}.</span>
        </div>
      )}

      {/* Légende */}
      <div className="flex items-center gap-4 flex-wrap text-[11px] text-neutral-500">
        {Object.entries(MODULE_LABELS).map(([mod, label]) => (
          <span key={mod} className="inline-flex items-center gap-1.5">
            <span className="w-2.5 h-2.5 rounded-sm shrink-0" style={{ backgroundColor: MODULE_COLORS[mod] }} />
            {label}
          </span>
        ))}
        <span className="inline-flex items-center gap-1.5">
          <svg width="16" height="6" className="shrink-0">
            <line x1="0" y1="3" x2="16" y2="3" stroke="#94A3B8" strokeWidth="1.5" />
          </svg>
          Référence par identifiant
        </span>
        <span className="inline-flex items-center gap-1.5">
          <svg width="16" height="6" className="shrink-0">
            <line x1="0" y1="3" x2="16" y2="3" stroke="#94A3B8" strokeWidth="1.5" strokeDasharray="3,2" />
          </svg>
          Implicite (par construction)
        </span>
        <span className="inline-flex items-center gap-1.5">
          <Sparkles size={11} className="text-primary-400 shrink-0" /> Généré par le prétraitement
        </span>
      </div>

      {/* Graphe */}
      <div className="rounded-xl border border-neutral-200 bg-neutral-50 overflow-auto" style={{ maxHeight: 600 }}>
        <div className="relative" style={{ width: layout.width, height: layout.height }}>
          <svg width={layout.width} height={layout.height} className="absolute inset-0 pointer-events-none">
            {data.edges.map((edge, i) => {
              const from = layout.positions.get(edge.sourceId)
              const to = layout.positions.get(edge.targetId)
              if (!from || !to) return null
              const sx = from.x + NODE_WIDTH
              const sy = from.y + NODE_HEIGHT / 2
              const tx = to.x
              const ty = to.y + NODE_HEIGHT / 2
              const mx = (sx + tx) / 2
              const isRelated = !hoveredId || edge.sourceId === hoveredId || edge.targetId === hoveredId
              return (
                <path
                  key={i}
                  d={`M ${sx},${sy} C ${mx},${sy} ${mx},${ty} ${tx},${ty}`}
                  fill="none"
                  stroke={hoveredId && isRelated ? '#0E9CA8' : '#CBD5E1'}
                  strokeWidth={hoveredId && isRelated ? 2 : 1.25}
                  strokeDasharray={edge.kind === 'IMPLICIT' ? '4,3' : undefined}
                  opacity={hoveredId && !isRelated ? 0.2 : 1}
                />
              )
            })}
          </svg>

          {layout.levels.map((level, levelIndex) => (
            <div
              key={`header-${level}`}
              className="absolute text-[10px] font-semibold uppercase tracking-wider text-neutral-400"
              style={{ left: PADDING_X + levelIndex * COLUMN_WIDTH, top: 4, width: NODE_WIDTH }}
            >
              {level === -1 ? 'Cycle' : `Niveau ${level}`}
            </div>
          ))}

          {layout.levels.map((level) =>
            layout.nodesByLevel.get(level)!.map((n) => {
              const pos = layout.positions.get(n.dataSpecId)!
              const dimmed = hoveredId != null && !highlighted.has(n.dataSpecId)
              return (
                <div
                  key={n.dataSpecId}
                  onMouseEnter={() => setHoveredId(n.dataSpecId)}
                  onMouseLeave={() => setHoveredId(null)}
                  title={`${n.dataSpecId}\nNiveau : ${n.level === -1 ? 'cycle' : n.level}\nDépend de : ${n.dependsOn.join(', ') || '—'}\nRequis par : ${n.requiredBy.join(', ') || '—'}`}
                  className={`absolute flex items-center gap-1 rounded-md border-l-4 bg-white px-2 shadow-sm text-[11px] transition-opacity ${dimmed ? 'opacity-25' : 'opacity-100'}`}
                  style={{
                    left: pos.x,
                    top: pos.y,
                    width: NODE_WIDTH,
                    height: NODE_HEIGHT,
                    borderLeftColor: MODULE_COLORS[n.module] ?? '#94A3B8',
                  }}
                >
                  <span className="font-mono text-neutral-700 truncate flex-1">{n.fileName}</span>
                  {n.generation === 'AUTO' && (
                    <Sparkles size={10} className="text-primary-400 shrink-0" aria-label="Généré par le prétraitement" />
                  )}
                </div>
              )
            }),
          )}
        </div>
      </div>

      <p className="text-[11px] text-neutral-400">
        {data.nodes.length} fichiers · {data.edges.length} dépendances. Survolez un fichier pour
        mettre en évidence ses dépendances directes.
      </p>
    </div>
  )
}
