import { useEffect, useState, type ReactNode } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Save, Trash2, Loader2, X } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { ParameterSpec, ParameterGroup, ParamType } from '@/entities/scenario'
import {
  createParameter,
  updateParameter,
  deleteParameter,
  type ParameterUpsertRequest,
} from '../api/paramCatalog.api'

const TYPES: ParamType[] = ['BOOLEAN', 'INTEGER', 'FLOAT', 'STRING', 'ENUM', 'STRING_LIST']

interface ParameterEditorProps {
  /** null = création. */
  spec: ParameterSpec | null
  groups: ParameterGroup[]
  dataSpecIds: string[]
  onSaved: (spec: ParameterSpec) => void
  onDeleted: () => void
}

interface FormState {
  gamlName: string
  label: string
  group: string
  type: ParamType
  defaultValue: string
  unit: string
  allowedValues: string
  visibleIf: string
  enabledIf: string
  optionsDataSpec: string
  advanced: boolean
  order: number
}

function defaultToString(v: unknown): string {
  if (v == null) return ''
  if (Array.isArray(v)) return v.join('|')
  return String(v)
}

function fromSpec(spec: ParameterSpec | null, fallbackGroup: string): FormState {
  return {
    gamlName: spec?.gamlName ?? '',
    label: spec?.label ?? '',
    group: spec?.group ?? fallbackGroup,
    type: spec?.type ?? 'STRING',
    defaultValue: defaultToString(spec?.defaultValue),
    unit: spec?.unit ?? '',
    allowedValues: (spec?.allowedValues ?? []).join(', '),
    visibleIf: spec?.visibleIf ?? '',
    enabledIf: spec?.enabledIf ?? '',
    optionsDataSpec: spec?.optionsDataSpec ?? '',
    advanced: spec?.advanced ?? false,
    order: spec?.order ?? 0,
  }
}

export function ParameterEditor({ spec, groups, dataSpecIds, onSaved, onDeleted }: ParameterEditorProps) {
  const queryClient = useQueryClient()
  const isEdit = !!spec
  const [form, setForm] = useState<FormState>(() => fromSpec(spec, groups[0]?.id ?? 'general'))
  const [confirmDelete, setConfirmDelete] = useState(false)

  useEffect(() => {
    setForm(fromSpec(spec, groups[0]?.id ?? 'general'))
    setConfirmDelete(false)
  }, [spec, groups])

  const set = <K extends keyof FormState>(k: K, v: FormState[K]) => setForm((f) => ({ ...f, [k]: v }))

  const invalidate = () => {
    queryClient.invalidateQueries({ queryKey: queryKeys.scenarioParameters.all })
    queryClient.invalidateQueries({ queryKey: queryKeys.scenarioParameters.groups })
  }

  const buildReq = (): ParameterUpsertRequest => ({
    gamlName: form.gamlName.trim(),
    label: form.label.trim(),
    group: form.group,
    type: form.type,
    defaultValue: form.defaultValue.trim() || null,
    unit: form.unit.trim() || null,
    allowedValues:
      form.type === 'ENUM'
        ? form.allowedValues.split(',').map((s) => s.trim()).filter((s) => s.length > 0)
        : null,
    visibleIf: form.visibleIf.trim() || null,
    enabledIf: form.enabledIf.trim() || null,
    optionsDataSpec: form.optionsDataSpec || null,
    advanced: form.advanced,
    order: Number(form.order) || 0,
  })

  const saveMutation = useMutation({
    mutationFn: () => (isEdit ? updateParameter(spec!.gamlName, buildReq()) : createParameter(buildReq())),
    onSuccess: (saved) => {
      invalidate()
      onSaved(saved)
    },
  })

  const deleteMutation = useMutation({
    mutationFn: () => deleteParameter(spec!.gamlName),
    onSuccess: () => {
      invalidate()
      onDeleted()
    },
  })

  const canSave = form.gamlName.trim() && form.label.trim() && form.group

  return (
    <div className="space-y-5">
      <div className="flex items-center justify-between">
        <h2 className="text-base font-semibold text-neutral-900">
          {isEdit ? 'Modifier le paramètre' : 'Nouveau paramètre'}
        </h2>
        {isEdit &&
          (confirmDelete ? (
            <div className="flex items-center gap-2">
              <span className="text-[12px] text-neutral-600">Supprimer ?</span>
              <Button variant="danger" size="sm" onClick={() => deleteMutation.mutate()} loading={deleteMutation.isPending}>
                Confirmer
              </Button>
              <button onClick={() => setConfirmDelete(false)} className="text-neutral-400 hover:text-neutral-700">
                <X size={16} />
              </button>
            </div>
          ) : (
            <button
              onClick={() => setConfirmDelete(true)}
              className="inline-flex items-center gap-1 text-[12px] text-neutral-400 hover:text-danger transition-colors"
            >
              <Trash2 size={13} /> Supprimer
            </button>
          ))}
      </div>

      <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
        <Field label="Nom GAML (identifiant) *">
          <input
            value={form.gamlName}
            onChange={(e) => set('gamlName', e.target.value)}
            disabled={isEdit}
            placeholder="ex : executerUneSeuleParcelle"
            className={inputCls + (isEdit ? ' bg-neutral-100 text-neutral-400' : '')}
          />
        </Field>
        <Field label="Libellé affiché *">
          <input value={form.label} onChange={(e) => set('label', e.target.value)} className={inputCls} placeholder="ex : simulationSurParcelle" />
        </Field>

        <Field label="Module (groupe) *">
          <select value={form.group} onChange={(e) => set('group', e.target.value)} className={inputCls}>
            {groups.map((g) => (
              <option key={g.id} value={g.id}>{g.label}</option>
            ))}
          </select>
        </Field>
        <Field label="Type *">
          <select value={form.type} onChange={(e) => set('type', e.target.value as ParamType)} className={inputCls}>
            {TYPES.map((t) => <option key={t} value={t}>{t}</option>)}
          </select>
        </Field>

        <Field label="Valeur par défaut" hint={form.type === 'STRING_LIST' ? 'séparées par |' : undefined}>
          <input value={form.defaultValue} onChange={(e) => set('defaultValue', e.target.value)} className={inputCls} />
        </Field>
        <Field label="Unité">
          <input value={form.unit} onChange={(e) => set('unit', e.target.value)} className={inputCls} placeholder="ex : année" />
        </Field>

        {form.type === 'ENUM' && (
          <Field label="Valeurs autorisées" hint="séparées par des virgules" full>
            <input value={form.allowedValues} onChange={(e) => set('allowedValues', e.target.value)} className={inputCls} placeholder="ex : , rcp4.5, rcp8.5" />
          </Field>
        )}

        <Field label="Condition d'affichage (visibleIf)" hint="ex : type == COMPLEXE">
          <input value={form.visibleIf} onChange={(e) => set('visibleIf', e.target.value)} className={inputCls} />
        </Field>
        <Field label="Condition d'activation (enabledIf)" hint="ex : executerUneSeuleParcelle == true">
          <input value={form.enabledIf} onChange={(e) => set('enabledIf', e.target.value)} className={inputCls} />
        </Field>

        <Field label="Valeurs depuis les données (DataSpec)" full>
          <select value={form.optionsDataSpec} onChange={(e) => set('optionsDataSpec', e.target.value)} className={inputCls}>
            <option value="">— aucune (saisie libre) —</option>
            {dataSpecIds.map((id) => <option key={id} value={id}>{id}</option>)}
          </select>
        </Field>

        <Field label="Ordre">
          <input type="number" value={form.order} onChange={(e) => set('order', Number(e.target.value))} className={inputCls} />
        </Field>
        <label className="flex items-center gap-2 text-[13px] text-neutral-700 self-end pb-2">
          <input type="checkbox" checked={form.advanced} onChange={(e) => set('advanced', e.target.checked)} className="accent-primary-600" />
          Paramètre avancé
        </label>
      </div>

      {saveMutation.isError && (
        <p className="text-sm text-danger">
          {(saveMutation.error as { detail?: string })?.detail ?? 'Erreur lors de l’enregistrement.'}
        </p>
      )}

      <div className="flex justify-end">
        <Button variant="primary" size="sm" onClick={() => saveMutation.mutate()} loading={saveMutation.isPending} disabled={!canSave}>
          {saveMutation.isPending ? <Loader2 size={14} className="animate-spin" /> : <Save size={14} />}
          {isEdit ? 'Enregistrer' : 'Créer le paramètre'}
        </Button>
      </div>
    </div>
  )
}

const inputCls =
  'w-full rounded-lg border border-neutral-200 px-3 py-2 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-primary-400'

function Field({ label, hint, full, children }: { label: string; hint?: string; full?: boolean; children: ReactNode }) {
  return (
    <div className={full ? 'sm:col-span-2' : ''}>
      <label className="block text-xs font-medium text-neutral-600 mb-1">
        {label}
        {hint && <span className="ml-1 font-normal text-neutral-400">· {hint}</span>}
      </label>
      {children}
    </div>
  )
}
