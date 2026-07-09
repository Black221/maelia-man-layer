import { useMemo, useState } from 'react'
import { useQuery } from '@tanstack/react-query'
import { Plus, Search, Database, FileType2 } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { DataSpec } from '@/entities/dataset'
import { listDataSpecs, DataSpecEditor, FieldsEditor } from '@/features/manage-catalog'

export function CatalogPage() {
  const { data: specs = [], isLoading, isError } = useQuery({
    queryKey: queryKeys.dataspecs.all,
    queryFn: listDataSpecs,
  })

  const [selectedId, setSelectedId] = useState<string | null>(null)
  const [creating, setCreating] = useState(false)
  const [filter, setFilter] = useState('')

  const selected = useMemo(
    () => (selectedId ? specs.find((s) => s.id === selectedId) ?? null : null),
    [specs, selectedId],
  )

  const grouped = useMemo(() => {
    const q = filter.trim().toLowerCase()
    const map = new Map<string, DataSpec[]>()
    for (const s of specs) {
      if (q && !`${s.id} ${s.fileName} ${s.module}`.toLowerCase().includes(q)) continue
      ;(map.get(s.module) ?? map.set(s.module, []).get(s.module)!).push(s)
    }
    return [...map.entries()].sort(([a], [b]) => a.localeCompare(b))
  }, [specs, filter])

  const select = (id: string) => {
    setCreating(false)
    setSelectedId(id)
  }

  return (
    <div className="max-w-[1200px] mx-auto px-4 py-6">
      <div className="flex items-center justify-between mb-5">
        <div className="flex items-center gap-3">
          <div className="flex h-10 w-10 items-center justify-center rounded-xl bg-primary-50 text-primary-600">
            <Database size={20} />
          </div>
          <div>
            <h1 className="text-lg font-semibold text-neutral-900">Catalogue de données</h1>
            <p className="text-[13px] text-neutral-500">Gérer les fichiers d'entrée MAELIA, leurs champs et leur orientation.</p>
          </div>
        </div>
        <Button variant="primary" size="md" onClick={() => { setCreating(true); setSelectedId(null) }}>
          <Plus size={16} /> Nouveau fichier
        </Button>
      </div>

      <div className="grid grid-cols-[320px_1fr] gap-5">
        {/* Arbre */}
        <aside className="rounded-2xl border border-neutral-200 bg-white p-3 h-[calc(100vh-180px)] overflow-y-auto">
          <div className="relative mb-3">
            <Search size={14} className="absolute left-2.5 top-1/2 -translate-y-1/2 text-neutral-400" />
            <input
              value={filter}
              onChange={(e) => setFilter(e.target.value)}
              placeholder="Rechercher…"
              className="w-full rounded-lg border border-neutral-200 pl-8 pr-2 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300"
            />
          </div>

          {isLoading && <p className="text-[13px] text-neutral-400 px-2">Chargement…</p>}
          {isError && <p className="text-[13px] text-danger px-2">Erreur de chargement du catalogue.</p>}

          {grouped.map(([module, items]) => (
            <div key={module} className="mb-3">
              <p className="text-[11px] font-semibold text-neutral-400 uppercase tracking-wide px-2 mb-1">{module}</p>
              <ul>
                {items.map((s) => (
                  <li key={s.id}>
                    <button
                      onClick={() => select(s.id)}
                      className={`w-full text-left rounded-lg px-2 py-1.5 flex items-center gap-2 ${
                        selectedId === s.id ? 'bg-primary-50 text-primary-700' : 'hover:bg-neutral-50 text-neutral-700'
                      }`}
                    >
                      <FileType2 size={14} className="shrink-0 text-neutral-400" />
                      <span className="flex-1 min-w-0">
                        <span className="block text-[13px] truncate">{s.fileName}</span>
                        <span className="block text-[10px] text-neutral-400 truncate">{s.id}</span>
                      </span>
                      {s.orientation === 'FIELDS_AS_ROWS' && (
                        <span className="text-[9px] bg-amber-100 text-amber-700 rounded px-1">transposé</span>
                      )}
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
            <div className="space-y-6">
              <DataSpecEditor
                spec={selected}
                onSaved={(s) => { setCreating(false); setSelectedId(s.id) }}
                onDeleted={() => { setCreating(false); setSelectedId(null) }}
              />
              {selected && (
                <div className="pt-4 border-t border-neutral-200">
                  <h3 className="text-sm font-semibold text-neutral-800 mb-3">Champs</h3>
                  <FieldsEditor spec={selected} specs={specs} />
                </div>
              )}
            </div>
          ) : (
            <div className="h-full flex items-center justify-center text-center">
              <p className="text-[13px] text-neutral-400">
                Sélectionnez un fichier à gauche, ou créez-en un nouveau.
              </p>
            </div>
          )}
        </section>
      </div>
    </div>
  )
}
