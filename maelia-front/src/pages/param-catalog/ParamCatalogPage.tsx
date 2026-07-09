import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search, SlidersHorizontal, Settings2, Sparkles, Lock } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { ParameterSpec } from '@/entities/scenario'
import { listParameters, listParameterGroups, ParameterEditor } from '@/features/manage-param-catalog'
import { listDataSpecs } from '@/features/manage-catalog'

export function ParamCatalogPage() {
  const { data: specs = [], isLoading, isError } = useQuery({
    queryKey: queryKeys.scenarioParameters.all,
    queryFn: listParameters,
  })
  const { data: groups = [] } = useQuery({
    queryKey: queryKeys.scenarioParameters.groups,
    queryFn: listParameterGroups,
  })
  const { data: dataSpecs = [] } = useQuery({
    queryKey: queryKeys.dataspecs.all,
    queryFn: listDataSpecs,
  })

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [filter, setFilter] = useState('')

  const selected = useMemo(
    () => (selectedId ? specs.find((s) => s.gamlName === selectedId) ?? null : null),
    [specs, selectedId],
  )

  const groupLabel = useMemo(() => {
    const m = new Map<string, string>()
    groups.forEach((g) => m.set(g.id, g.label))
    return m
  }, [groups])

  const grouped = useMemo(() => {
    const q = filter.trim().toLowerCase()
    const map = new Map<string, ParameterSpec[]>()
    for (const s of specs) {
      if (q && !`${s.gamlName} ${s.label} ${s.group}`.toLowerCase().includes(q)) continue
      ;(map.get(s.group) ?? map.set(s.group, []).get(s.group)!).push(s)
    }
    // ordre des groupes selon le catalogue
    return groups
      .map((g) => [g.id, map.get(g.id) ?? []] as const)
      .filter(([, items]) => items.length > 0)
  }, [specs, groups, filter])

  const dataSpecIds = useMemo(() => dataSpecs.map((d) => d.id).sort(), [dataSpecs])

  const select = (id: string) => {
    setCreating(false)
    setSelectedId(id)
  }

  return (
    <div className="max-w-[1200px] mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
            <Settings2 size={20} />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-neutral-900">Paramètres de scénario</h1>
            <p className="text-[13px] text-neutral-500">
              Gérer le catalogue des paramètres de simulation, leurs dépendances et sources de données.
            </p>
          </div>
        </div>
        <Button variant="primary" size="md" onClick={() => { setCreating(true); setSelectedId(null) }}>
          <Plus size={16} /> Nouveau paramètre
        </Button>
      </div>

      <div className="grid grid-cols-[340px_1fr] gap-5">
        {/* Arbre par module */}
        <aside className="rounded-2xl border border-neutral-200 bg-white p-3 h-[calc(100vh-180px)] overflow-y-auto">
          <div className="relative mb-3">
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Rechercher un paramètre…"
              className="w-full rounded-lg border border-neutral-200 pl-8 pr-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300"
            />
          </div>

          {isLoading && <p className="text-[13px] text-neutral-400 px-2">Chargement…</p>}
          {isError && <p className="text-[13px] text-danger px-2">Erreur de chargement.</p>}

          {grouped.map(([groupId, items]) => (
            <div key={groupId} className="mb-3">
              <p className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wide px-2 mb-1">
                {groupLabel.get(groupId) ?? groupId}
              </p>
              <ul>
                {items.map((s) => (
                  <li key={s.gamlName}>
                    <button
                      onClick={() => select(s.gamlName)}
                      className={`w-full text-left rounded-lg px-2 py-1.5 flex items-center gap-2 ${
                        selectedId === s.gamlName ? 'bg-primary-50 text-primary-700' : 'hover:bg-neutral-50 text-neutral-700'
                      }`}
                    >
                      <SlidersHorizontal size={13} className="shrink-0 text-neutral-400" />
                      <span className="flex-1 min-w-0">
                        <span className="block text-[13px] truncate">{s.label}</span>
                        <span className="block text-[10px] text-neutral-400 truncate font-mono">{s.gamlName}</span>
                      </span>
                      {s.enabledIf && <Lock size={11} className="shrink-0 text-amber-500" />}
                      {s.optionsDataSpec && <Sparkles size={11} className="shrink-0 text-primary-400" />}
                    </button>
                  </li>
                ))}
              </ul>
            </div>
          ))}
        </aside>

        {/* Éditeur */}
        <section className="rounded-2xl border border-neutral-200 bg-white p-5 h-[calc(100vh-180px)] overflow-y-auto">
          {creating || selected ? (
            <ParameterEditor
              spec={selected}
              groups={groups}
              dataSpecIds={dataSpecIds}
              onSaved={(s) => { setCreating(false); setSelectedId(s.gamlName) }}
              onDeleted={() => { setCreating(false); setSelectedId(null) }}
            />
          ) : (
            <div className="h-full flex items-center justify-center text-center">
              <p className="text-[13px] text-neutral-400">
                Sélectionnez un paramètre à gauche, ou créez-en un nouveau.
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
