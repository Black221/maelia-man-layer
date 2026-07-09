import { useEffect, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Save, Trash2, Copy } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { DataSpec, Orientation } from '@/entities/dataset'
import {
  createDataSpec,
  updateDataSpec,
  deleteDataSpec,
  duplicateDataSpec,
  type DataSpecUpsert,
} from '../api/catalog.api'

interface Props {
  /** null = mode création */
  spec: DataSpec | null
  onSaved: (spec: DataSpec) => void
  onDeleted: () => void
}

const empty: DataSpecUpsert = {
  id: '',
  module: 'AGRICOLE',
  folder: '',
  fileName: '',
  fileType: 'CSV',
  csvFormat: 'COLUMN_HEADER',
  orientation: 'FIELDS_AS_COLUMNS',
  matrixValueStartIndex: 1,
  delimiter: ';',
  generation: 'MANUAL',
  required: true,
  temporalResolution: 'NONE',
  multiInstance: false,
  saisieMode: 'GRID',
  fieldsStatus: 'PENDING',
}

const MODULES = ['COMMUN', 'AGRICOLE', 'HYDROGRAPHIQUE', 'NORMATIF', 'ELEVAGE', 'FILIERE']

export function DataSpecEditor({ spec, onSaved, onDeleted }: Props) {
  const qc = useQueryClient()
  const isNew = spec === null
  const [form, setForm] = useState<DataSpecUpsert>(empty)

  useEffect(() => {
    if (spec) {
      setForm({
        id: spec.id,
        module: spec.module,
        folder: spec.folder ?? '',
        fileName: spec.fileName,
        fileType: spec.fileType,
        csvFormat: spec.csvFormat ?? 'COLUMN_HEADER',
        orientation: spec.orientation,
        matrixValueStartIndex: spec.matrixValueStartIndex ?? 1,
        delimiter: spec.delimiter ?? ';',
        generation: spec.generation,
        required: spec.required,
        requiredIf: spec.requiredIf,
        temporalResolution: spec.temporalResolution ?? 'NONE',
        multiInstance: spec.multiInstance,
        instancePattern: spec.instancePattern,
        saisieMode: spec.saisieMode,
        description: spec.description,
        fieldsStatus: spec.fieldsStatus,
      })
    } else {
      setForm(empty)
    }
  }, [spec])

  const set = <K extends keyof DataSpecUpsert>(k: K, v: DataSpecUpsert[K]) =>
    setForm((f) => ({ ...f, [k]: v }))

  const save = useMutation({
    mutationFn: () => (isNew ? createDataSpec(form) : updateDataSpec(form.id, form)),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: queryKeys.dataspecs.all })
      onSaved(saved)
    },
  })

  const remove = useMutation({
    mutationFn: (force: boolean) => deleteDataSpec(form.id, force),
    onSuccess: () => {
      qc.invalidateQueries({ queryKey: queryKeys.dataspecs.all })
      onDeleted()
    },
  })

  const dup = useMutation({
    mutationFn: (newId: string) => duplicateDataSpec(form.id, newId),
    onSuccess: (saved) => {
      qc.invalidateQueries({ queryKey: queryKeys.dataspecs.all })
      onSaved(saved)
    },
  })

  const transposed = form.orientation === 'FIELDS_AS_ROWS'
  const isShp = form.fileType === 'SHP'

  const onDelete = () => {
    if (!confirm(`Supprimer le fichier « ${form.id} » ?`)) return
    remove.mutate(false, {
      onError: () => {
        if (confirm('Des données/champs référencent ce fichier. Forcer la suppression ?')) {
          remove.mutate(true)
        }
      },
    })
  }

  const onDuplicate = () => {
    const newId = prompt('Identifiant du nouveau fichier (copie) :', `${form.id}-copie`)
    if (newId) dup.mutate(newId)
  }

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between gap-3">
        <h2 className="text-base font-semibold text-neutral-900">
          {isNew ? 'Nouveau fichier' : form.id}
          {!isNew && spec?.origin === 'USER' && (
            <span className="ml-2 text-[11px] font-medium text-primary-600 bg-primary-50 rounded px-1.5 py-0.5">
              édité
            </span>
          )}
        </h2>
        <div className="flex items-center gap-2">
          {!isNew && (
            <>
              <Button variant="ghost" size="sm" onClick={onDuplicate}>
                <Copy size={14} /> Dupliquer
              </Button>
              <Button variant="ghost" size="sm" onClick={onDelete} loading={remove.isPending}>
                <Trash2 size={14} /> Supprimer
              </Button>
            </>
          )}
          <Button variant="primary" size="sm" onClick={() => save.mutate()} loading={save.isPending}>
            <Save size={14} /> Enregistrer
          </Button>
        </div>
      </div>

      {save.isError && (
        <p className="text-sm text-danger">
          {(save.error as { detail?: string })?.detail ?? (save.error as Error)?.message ?? 'Erreur'}
        </p>
      )}

      <div className="grid grid-cols-2 gap-4">
        <Field label="Identifiant" hint="ex. agri.culture.especesCultivees">
          <input
            disabled={!isNew}
            value={form.id}
            onChange={(e) => set('id', e.target.value)}
            className={input + (isNew ? '' : ' bg-neutral-50 text-neutral-500')}
          />
        </Field>
        <Field label="Module">
          <select value={form.module} onChange={(e) => set('module', e.target.value)} className={input}>
            {MODULES.map((m) => (
              <option key={m} value={m}>{m}</option>
            ))}
          </select>
        </Field>
        <Field label="Dossier" hint="ex. modeleAgricole/culture">
          <input value={form.folder ?? ''} onChange={(e) => set('folder', e.target.value)} className={input} />
        </Field>
        <Field label="Nom de fichier" hint="ex. especesCultivees.csv">
          <input value={form.fileName} onChange={(e) => set('fileName', e.target.value)} className={input} />
        </Field>
        <Field label="Type de fichier">
          <select value={form.fileType} onChange={(e) => set('fileType', e.target.value)} className={input}>
            <option value="CSV">CSV</option>
            <option value="SHP">SHP (géométrie)</option>
          </select>
        </Field>
        <Field label="Génération">
          <select value={form.generation} onChange={(e) => set('generation', e.target.value)} className={input}>
            <option value="MANUAL">MANUAL</option>
            <option value="AUTO">AUTO</option>
          </select>
        </Field>
      </div>

      {!isShp && (
        <div className="rounded-xl border border-neutral-200 p-4 space-y-4">
          <p className="text-[13px] font-medium text-neutral-700">Orientation & format CSV</p>
          <div className="flex flex-wrap gap-4">
            <Radio
              checked={form.orientation === 'FIELDS_AS_COLUMNS'}
              onChange={() => set('orientation', 'FIELDS_AS_COLUMNS' as Orientation)}
              title="Champs en colonnes"
              desc="Standard : 1re ligne = entête, chaque ligne = un enregistrement."
            />
            <Radio
              checked={transposed}
              onChange={() => set('orientation', 'FIELDS_AS_ROWS' as Orientation)}
              title="Champs en lignes (transposé)"
              desc="Chaque champ = une ligne ; chaque enregistrement = une colonne."
            />
          </div>
          <div className="grid grid-cols-3 gap-4">
            <Field label="Mode d'entête">
              <select value={form.csvFormat ?? 'COLUMN_HEADER'} onChange={(e) => set('csvFormat', e.target.value)} className={input}>
                <option value="COLUMN_HEADER">Entête nommée</option>
                <option value="LINE_NUMBER">Positionnel (sans entête)</option>
              </select>
            </Field>
            <Field label="Délimiteur">
              <input maxLength={1} value={form.delimiter ?? ';'} onChange={(e) => set('delimiter', e.target.value)} className={input} />
            </Field>
            {transposed && (
              <Field label="1re colonne de valeurs" hint="0=clé en col 0, valeurs dès col 1">
                <input
                  type="number"
                  min={1}
                  value={form.matrixValueStartIndex ?? 1}
                  onChange={(e) => set('matrixValueStartIndex', Number(e.target.value))}
                  className={input}
                />
              </Field>
            )}
          </div>
          <CsvPreview labels={(spec?.fields ?? []).map((f) => f.label)} transposed={transposed} delimiter={form.delimiter ?? ';'} start={form.matrixValueStartIndex ?? 1} />
        </div>
      )}

      <div className="grid grid-cols-2 gap-4">
        <Field label="Obligatoire">
          <select value={String(form.required)} onChange={(e) => set('required', e.target.value === 'true')} className={input}>
            <option value="true">Oui</option>
            <option value="false">Non</option>
          </select>
        </Field>
        <Field label="Condition (requiredIf)" hint="ex. assolementMethod==DONNEES_ENTREE">
          <input value={form.requiredIf ?? ''} onChange={(e) => set('requiredIf', e.target.value)} className={input} />
        </Field>
        <Field label="Résolution temporelle">
          <select value={form.temporalResolution ?? 'NONE'} onChange={(e) => set('temporalResolution', e.target.value)} className={input}>
            {['NONE', 'YEARLY', 'MONTHLY', 'DAILY'].map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </Field>
        <Field label="Statut des champs">
          <select value={form.fieldsStatus ?? 'PENDING'} onChange={(e) => set('fieldsStatus', e.target.value)} className={input}>
            {['PENDING', 'VERIFIED_PARTIAL', 'VERIFIED'].map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </Field>
      </div>

      <Field label="Description">
        <textarea value={form.description ?? ''} onChange={(e) => set('description', e.target.value)} rows={2} className={input} />
      </Field>
    </div>
  )
}

const input =
  'w-full rounded-md border border-neutral-200 px-2.5 py-1.5 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-primary-400'

function Field({ label, hint, children }: { label: string; hint?: string; children: React.ReactNode }) {
  return (
    <label className="block space-y-1">
      <span className="text-[12px] font-medium text-neutral-600">{label}</span>
      {children}
      {hint && <span className="block text-[11px] text-neutral-400">{hint}</span>}
    </label>
  )
}

function Radio({ checked, onChange, title, desc }: { checked: boolean; onChange: () => void; title: string; desc: string }) {
  return (
    <button
      type="button"
      onClick={onChange}
      className={`flex-1 min-w-[220px] text-left rounded-lg border p-3 transition-colors ${
        checked ? 'border-primary-400 bg-primary-50' : 'border-neutral-200 hover:bg-neutral-50'
      }`}
    >
      <span className="flex items-center gap-2 text-[13px] font-medium text-neutral-800">
        <span className={`h-3.5 w-3.5 rounded-full border ${checked ? 'border-primary-500 bg-primary-500' : 'border-neutral-300'}`} />
        {title}
      </span>
      <span className="block mt-1 text-[11px] text-neutral-500">{desc}</span>
    </button>
  )
}

function CsvPreview({ labels, transposed, delimiter, start }: { labels: string[]; transposed: boolean; delimiter: string; start: number }) {
  const d = delimiter || ';'
  const sample = labels.length ? labels : ['champ1', 'champ2']
  let lines: string[]
  if (!transposed) {
    lines = [sample.join(d), sample.map(() => 'valeur').join(d)]
  } else {
    const meta = Array.from({ length: Math.max(0, start - 1) }, () => '')
    lines = sample.map((l) => [l, ...meta, 'rec1', 'rec2'].join(d))
  }
  return (
    <div>
      <p className="text-[11px] text-neutral-400 mb-1">Aperçu du fichier généré</p>
      <pre className="text-[11px] bg-neutral-900 text-neutral-100 rounded-lg p-3 overflow-x-auto">{lines.join('\n')}</pre>
    </div>
  )
}
