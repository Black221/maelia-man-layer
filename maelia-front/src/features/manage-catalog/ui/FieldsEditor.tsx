import { useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, Save, ArrowUp, ArrowDown } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { DataSpec, FieldSpec } from '@/entities/dataset'
import { addField, updateField, deleteField, reorderFields, type FieldSpecUpsert } from '../api/catalog.api'

const INFO_TYPES = ['String', 'Integer', 'Double', 'Boolean', 'Date']

export function FieldsEditor({ spec, specs }: { spec: DataSpec; specs: DataSpec[] }) {
  const qc = useQueryClient()
  const invalidate = () => {
    qc.invalidateQueries({ queryKey: queryKeys.dataspecs.all })
    qc.invalidateQueries({ queryKey: queryKeys.dataspecs.detail(spec.id) })
  }

  const add = useMutation({ mutationFn: (b: FieldSpecUpsert) => addField(spec.id, b), onSuccess: invalidate })
  const upd = useMutation({
    mutationFn: (p: { id: string; body: FieldSpecUpsert }) => updateField(spec.id, p.id, p.body),
    onSuccess: invalidate,
  })
  const del = useMutation({ mutationFn: (id: string) => deleteField(spec.id, id), onSuccess: invalidate })
  const reorder = useMutation({ mutationFn: (ids: string[]) => reorderFields(spec.id, ids), onSuccess: invalidate })

  const fields = spec.fields
  const move = (idx: number, dir: -1 | 1) => {
    const next = [...fields]
    const j = idx + dir
    if (j < 0 || j >= next.length) return
    ;[next[idx], next[j]] = [next[j], next[idx]]
    reorder.mutate(next.map((f) => f.id))
  }

  return (
    <div className="space-y-3">
      <div className="flex items-center justify-between">
        <p className="text-[13px] font-medium text-neutral-700">{fields.length} champ(s)</p>
        <Button
          variant="ghost"
          size="sm"
          onClick={() => add.mutate({ label: `champ_${fields.length + 1}`, infoType: 'String', required: false })}
          loading={add.isPending}
        >
          <Plus size={14} /> Ajouter un champ
        </Button>
      </div>

      <div className="overflow-x-auto rounded-xl border border-neutral-200 bg-white">
        <table className="w-full text-sm">
          <thead>
            <tr className="bg-neutral-50 border-b border-neutral-200 text-[11px] text-neutral-500">
              <th className="px-2 py-2 w-16">Ordre</th>
              <th className="px-2 py-2 text-left">Label</th>
              <th className="px-2 py-2 text-left">Type</th>
              <th className="px-2 py-2 text-left">Unité</th>
              <th className="px-2 py-2 text-center">Requis</th>
              <th className="px-2 py-2 text-left">Référence</th>
              <th className="px-2 py-2 text-left">Sép. liste</th>
              <th className="px-2 py-2 text-left">Valeurs autorisées</th>
              <th className="px-2 py-2 w-20" />
            </tr>
          </thead>
          <tbody>
            {fields.map((f, idx) => (
              <FieldRow
                key={f.id}
                field={f}
                idx={idx}
                count={fields.length}
                specs={specs}
                onMove={move}
                onSave={(body) => upd.mutate({ id: f.id, body })}
                onDelete={() => {
                  if (confirm(`Supprimer le champ « ${f.label} » ?`)) del.mutate(f.id)
                }}
                saving={upd.isPending}
              />
            ))}
            {fields.length === 0 && (
              <tr>
                <td colSpan={9} className="text-center text-[13px] text-neutral-400 py-4">
                  Aucun champ. Cliquez sur « Ajouter un champ ».
                </td>
              </tr>
            )}
          </tbody>
        </table>
      </div>
    </div>
  )
}

function FieldRow({
  field,
  idx,
  count,
  specs,
  onMove,
  onSave,
  onDelete,
  saving,
}: {
  field: FieldSpec
  idx: number
  count: number
  specs: DataSpec[]
  onMove: (idx: number, dir: -1 | 1) => void
  onSave: (body: FieldSpecUpsert) => void
  onDelete: () => void
  saving: boolean
}) {
  const [f, setF] = useState<FieldSpecUpsert>({
    label: field.label,
    infoType: field.infoType,
    unit: field.unit,
    required: field.required,
    referencesDataSpec: field.referencesDataSpec,
    listSeparator: field.listSeparator,
    allowedValues: field.allowedValues,
    description: field.description,
    position: field.position,
  })
  const set = <K extends keyof FieldSpecUpsert>(k: K, v: FieldSpecUpsert[K]) => setF((p) => ({ ...p, [k]: v }))

  return (
    <tr className="border-b border-neutral-100 last:border-0">
      <td className="px-2 py-1 text-center">
        <div className="flex flex-col items-center">
          <button onClick={() => onMove(idx, -1)} disabled={idx === 0} className="text-neutral-300 hover:text-neutral-600 disabled:opacity-30">
            <ArrowUp size={12} />
          </button>
          <button onClick={() => onMove(idx, 1)} disabled={idx === count - 1} className="text-neutral-300 hover:text-neutral-600 disabled:opacity-30">
            <ArrowDown size={12} />
          </button>
        </div>
      </td>
      <td className="px-2 py-1"><input value={f.label} onChange={(e) => set('label', e.target.value)} className={cell} /></td>
      <td className="px-2 py-1">
        <select value={f.infoType} onChange={(e) => set('infoType', e.target.value)} className={cell}>
          {INFO_TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
        </select>
      </td>
      <td className="px-2 py-1"><input value={f.unit ?? ''} onChange={(e) => set('unit', e.target.value)} className={cell} /></td>
      <td className="px-2 py-1 text-center">
        <input type="checkbox" checked={!!f.required} onChange={(e) => set('required', e.target.checked)} />
      </td>
      <td className="px-2 py-1">
        <select value={f.referencesDataSpec ?? ''} onChange={(e) => set('referencesDataSpec', e.target.value || null)} className={cell}>
          <option value="">—</option>
          {specs.map((s) => <option key={s.id} value={s.id}>{s.id}</option>)}
        </select>
      </td>
      <td className="px-2 py-1"><input value={f.listSeparator ?? ''} maxLength={1} onChange={(e) => set('listSeparator', e.target.value)} className={cell + ' w-12'} /></td>
      <td className="px-2 py-1">
        <input
          value={(f.allowedValues ?? []).join('|')}
          placeholder="O|N"
          onChange={(e) => set('allowedValues', e.target.value ? e.target.value.split('|').map((s) => s.trim()).filter(Boolean) : [])}
          className={cell}
        />
      </td>
      <td className="px-1 py-1">
        <div className="flex items-center gap-1">
          <button onClick={() => onSave(f)} disabled={saving} title="Enregistrer" className="p-1 rounded text-primary-500 hover:bg-primary-50">
            <Save size={13} />
          </button>
          <button onClick={onDelete} title="Supprimer" className="p-1 rounded text-neutral-300 hover:text-danger hover:bg-danger/10">
            <Trash2 size={13} />
          </button>
        </div>
      </td>
    </tr>
  )
}

const cell = 'w-full min-w-[80px] rounded border border-neutral-200 px-1.5 py-1 text-[13px] focus:outline-none focus:ring-1 focus:ring-primary-300'
