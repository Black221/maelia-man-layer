import { useId, useMemo, useState } from 'react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { Loader2, ChevronRight, RotateCcw, Pencil, X } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { ParameterGroup, ParameterSpec, Scenario } from '@/entities/scenario'
import {
  createScenario,
  updateScenario,
  getScenarioParameters,
  getScenarioParameterGroups,
} from '../api/scenario.api'
import { isVisible } from '../model/visibleIf'
import { useParamOptions } from '@/features/manage-dataset'

interface ScenarioFormProps {
  projectId: string
  /** Présent = édition d'un scénario existant ; absent = création. */
  scenarioId?: string
  /** Valeurs initiales (édition ou duplication). */
  initialValues?: {
    name: string
    description?: string | null
    parameterValues: Record<string, unknown>
  }
  onSaved?: (scenario: Scenario) => void
  onCancel?: () => void
}

function isOverridden(value: unknown, def: unknown): boolean {
  return JSON.stringify(value) !== JSON.stringify(def)
}

// Libellés courts pour les onglets (un onglet par module + Sorties).
const TAB_LABELS: Record<string, string> = {
  general:  'Général',
  hydro:    'Hydrologique',
  agricole: 'Agricole',
  filiere:  'Filière',
  normatif: 'Normatif',
  sorties:  'Sorties',
}

export function ScenarioForm({ projectId, scenarioId, initialValues, onSaved, onCancel }: ScenarioFormProps) {
  const queryClient = useQueryClient()
  const isEdit = !!scenarioId
  const [name, setName] = useState(initialValues?.name ?? '')
  const [description, setDescription] = useState(initialValues?.description ?? '')
  const [overrides, setOverrides] = useState<Record<string, unknown>>(initialValues?.parameterValues ?? {})
  const [activeTab, setActiveTab] = useState<string | null>(null)
  // Dialog d'édition du nom/description : ouvert d'emblée en création (nom requis).
  const [infoOpen, setInfoOpen] = useState(!isEdit && !(initialValues?.name))
  const [openSub, setOpenSub] = useState<Record<string, boolean>>({})
  const [showAdvanced, setShowAdvanced] = useState(false)

  const { data: specs, isLoading: specsLoading } = useQuery({
    queryKey: queryKeys.scenarioParameters.all,
    queryFn: getScenarioParameters,
    staleTime: 5 * 60_000,
  })
  const { data: groups } = useQuery({
    queryKey: queryKeys.scenarioParameters.groups,
    queryFn: getScenarioParameterGroups,
    staleTime: 5 * 60_000,
  })

  // Valeurs proposées issues des données du projet, pour les paramètres « select depuis données ».
  // Clé = gamlName (une même DataSpec peut être proposée par colonne/source différentes).
  const optionSpecs = useMemo(
    () => (specs ?? [])
      .filter((s) => !!s.optionsDataSpec)
      .map((s) => ({
        gamlName: s.gamlName,
        optionsDataSpec: s.optionsDataSpec,
        optionsColumn: s.optionsColumn,
        optionsSource: s.optionsSource,
      })),
    [specs],
  )
  const { data: dataOptions } = useParamOptions(projectId, optionSpecs)

  const byName = useMemo(() => {
    const m = new Map<string, ParameterSpec>()
    ;(specs ?? []).forEach((s) => m.set(s.gamlName, s))
    return m
  }, [specs])

  const resolve = (gamlName: string): unknown =>
    gamlName in overrides ? overrides[gamlName] : byName.get(gamlName)?.defaultValue

  const setValue = (gamlName: string, value: unknown) =>
    setOverrides((prev) => ({ ...prev, [gamlName]: value }))

  const resetValue = (gamlName: string) =>
    setOverrides((prev) => {
      const next = { ...prev }
      delete next[gamlName]
      return next
    })

  const mutation = useMutation({
    mutationFn: () => {
      const parameterValues: Record<string, unknown> = {}
      for (const s of specs ?? []) {
        if (!(s.gamlName in overrides)) continue
        const v = overrides[s.gamlName]
        if (!isOverridden(v, s.defaultValue)) continue // n'envoie que les écarts
        parameterValues[s.gamlName] = v
      }
      const payload = { name, description: description || undefined, parameterValues }
      return isEdit
        ? updateScenario(projectId, scenarioId!, payload)
        : createScenario(projectId, payload)
    },
    onSuccess: (scenario) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.scenarios.all(projectId) })
      if (scenarioId) queryClient.invalidateQueries({ queryKey: queryKeys.scenarios.detail(scenarioId) })
      onSaved?.(scenario)
    },
  })

  // groupes top-level + enfants, et paramètres par groupe
  const topGroups = (groups ?? []).filter((g) => !g.parentId).sort((a, b) => a.order - b.order)
  const childGroups = (groups ?? []).filter((g) => g.parentId)
  const paramsOf = (groupId: string) =>
    (specs ?? [])
      .filter((s) => s.group === groupId)
      .filter((s) => showAdvanced || !s.advanced)
      .filter((s) => isVisible(s.visibleIf, resolve))
      .sort((a, b) => a.order - b.order)

  const groupHasContent = (g: ParameterGroup): boolean =>
    paramsOf(g.id).length > 0 || childGroups.filter((c) => c.parentId === g.id).some(groupHasContent)

  // nombre d'écarts dans un groupe (récursif), pour le badge d'accordéon
  const overridesInGroup = (g: ParameterGroup): number => {
    const direct = paramsOf(g.id).filter((s) => s.gamlName in overrides && isOverridden(overrides[s.gamlName], s.defaultValue)).length
    const nested = childGroups.filter((c) => c.parentId === g.id).reduce((n, c) => n + overridesInGroup(c), 0)
    return direct + nested
  }

  if (specsLoading) {
    return (
      <div className="flex items-center gap-2 py-8 text-neutral-400 text-sm">
        <Loader2 size={16} className="animate-spin" /> Chargement des paramètres…
      </div>
    )
  }

  const overrideCount = Object.keys(overrides).filter(
    (k) => byName.has(k) && isOverridden(overrides[k], byName.get(k)!.defaultValue),
  ).length

  // Onglets = groupes de tête (hors « system ») ayant du contenu, dans l'ordre du catalogue.
  const tabs = topGroups.filter((g) => g.id !== 'system' && groupHasContent(g))
  const current = activeTab && tabs.some((g) => g.id === activeTab) ? activeTab : (tabs[0]?.id ?? null)
  const currentGroup = tabs.find((g) => g.id === current) ?? null

  return (
    <div className="space-y-5">
      {/* En-tête : nom + description (édités via dialog) */}
      <div className="flex items-start justify-between gap-4 rounded-2xl border border-neutral-200 bg-white px-5 py-4 shadow-sm">
        <div className="min-w-0">
          {name ? (
            <>
              <h2 className="text-[17px] font-semibold text-neutral-900 truncate">{name}</h2>
              {description ? (
                <p className="text-[13px] text-neutral-500 mt-0.5 line-clamp-2">{description}</p>
              ) : (
                <p className="text-[13px] text-neutral-400 mt-0.5">Aucune description</p>
              )}
            </>
          ) : (
            <p className="text-[14px] text-neutral-400 italic">Aucun nom défini — cliquez sur « Modifier les infos ».</p>
          )}
        </div>
        <Button variant="secondary" size="sm" onClick={() => setInfoOpen(true)}>
          <Pencil size={13} /> Modifier les infos
        </Button>
      </div>

      {/* Paramètres — un onglet par module + Sorties */}
      <section className="rounded-2xl border border-neutral-200 bg-white shadow-sm overflow-hidden">
        <div className="flex items-center justify-between gap-3 px-5 py-3 border-b border-neutral-100">
          <div>
            <h3 className="text-sm font-semibold text-neutral-800">Paramètres de simulation</h3>
            <p className="text-[12px] text-neutral-400 mt-0.5">
              {overrideCount > 0
                ? `${overrideCount} paramètre(s) personnalisé(s)`
                : 'Tous au défaut du catalogue'}
            </p>
          </div>
          <label className="flex items-center gap-1.5 text-xs text-neutral-500 cursor-pointer select-none">
            <input
              type="checkbox"
              checked={showAdvanced}
              onChange={(e) => setShowAdvanced(e.target.checked)}
              className="accent-primary-600"
            />
            Paramètres avancés
          </label>
        </div>

        {/* Onglets modules */}
        <div className="border-b border-neutral-200 overflow-x-auto">
          <div className="flex gap-0 px-2">
            {tabs.map((g) => {
              const count = overridesInGroup(g)
              const isActive = g.id === current
              return (
                <button
                  key={g.id}
                  type="button"
                  onClick={() => setActiveTab(g.id)}
                  className={[
                    'flex items-center gap-1.5 px-3.5 py-2.5 text-[13px] font-medium border-b-2 whitespace-nowrap transition-colors',
                    isActive
                      ? 'border-primary text-primary'
                      : 'border-transparent text-neutral-500 hover:text-neutral-800',
                  ].join(' ')}
                >
                  {TAB_LABELS[g.id] ?? g.label}
                  {count > 0 && (
                    <span
                      className={[
                        'text-[10px] font-semibold px-1.5 py-0.5 rounded-full',
                        isActive ? 'bg-primary/10 text-primary' : 'bg-neutral-100 text-neutral-400',
                      ].join(' ')}
                    >
                      {count}
                    </span>
                  )}
                </button>
              )
            })}
          </div>
        </div>

        {/* Contenu de l'onglet actif */}
        {currentGroup && (
          <div className="px-5 py-4 space-y-4">
            {paramsOf(currentGroup.id).length > 0 && (
              <ParamFields specs={paramsOf(currentGroup.id)} resolve={resolve} onChange={setValue} onReset={resetValue} byName={byName} dataOptions={dataOptions ?? {}} />
            )}

            {/* Sous-groupes (cas « Sorties ») : sections repliables */}
            {childGroups
              .filter((c) => c.parentId === currentGroup.id)
              .sort((a, b) => a.order - b.order)
              .filter((c) => paramsOf(c.id).length > 0)
              .map((c) => {
                const open = openSub[c.id] ?? false
                const subCount = overridesInGroup(c)
                return (
                  <div key={c.id} className="rounded-xl border border-neutral-200 overflow-hidden">
                    <button
                      type="button"
                      onClick={() => setOpenSub((p) => ({ ...p, [c.id]: !open }))}
                      className="flex w-full items-center gap-2 px-3.5 py-2.5 text-left text-[13px] font-medium text-neutral-700 hover:bg-neutral-50 transition-colors"
                    >
                      <ChevronRight size={14} className={`text-neutral-400 transition-transform ${open ? 'rotate-90' : ''}`} />
                      {c.label}
                      {subCount > 0 && (
                        <span className="ml-1 text-[10px] font-semibold px-1.5 py-0.5 rounded-full bg-primary-50 text-primary-700">
                          {subCount}
                        </span>
                      )}
                    </button>
                    {open && (
                      <div className="px-3.5 pb-3.5 pt-1 border-t border-neutral-100">
                        <ParamFields specs={paramsOf(c.id)} resolve={resolve} onChange={setValue} onReset={resetValue} byName={byName} dataOptions={dataOptions ?? {}} />
                      </div>
                    )}
                  </div>
                )
              })}
          </div>
        )}
      </section>

      {mutation.isError && (
        <p className="text-xs text-danger">
          {(mutation.error as { detail?: string })?.detail
            ?? `Erreur lors de l'${isEdit ? 'enregistrement' : 'création'} du scénario.`}
        </p>
      )}

      <div className="flex justify-end gap-2">
        {onCancel && (
          <Button variant="ghost" size="sm" onClick={onCancel}>Annuler</Button>
        )}
        <Button
          variant="primary"
          size="sm"
          onClick={() => mutation.mutate()}
          loading={mutation.isPending}
          disabled={!name.trim()}
        >
          {isEdit ? 'Enregistrer les modifications' : 'Créer le scénario'}
        </Button>
      </div>

      {infoOpen && (
        <ScenarioInfoDialog
          name={name}
          description={description ?? ''}
          onClose={() => setInfoOpen(false)}
          onSave={(n, d) => {
            setName(n)
            setDescription(d)
            setInfoOpen(false)
          }}
        />
      )}
    </div>
  )
}

interface ScenarioInfoDialogProps {
  name: string
  description: string
  onClose: () => void
  onSave: (name: string, description: string) => void
}

function ScenarioInfoDialog({ name, description, onClose, onSave }: ScenarioInfoDialogProps) {
  const [draftName, setDraftName] = useState(name)
  const [draftDesc, setDraftDesc] = useState(description)

  const submit = (e: React.FormEvent) => {
    e.preventDefault()
    if (!draftName.trim()) return
    onSave(draftName.trim(), draftDesc.trim())
  }

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center bg-black/40 p-4">
      <div className="w-full max-w-md rounded-xl bg-white shadow-lg p-6">
        <div className="flex items-center justify-between mb-5">
          <h2 className="text-base font-semibold text-neutral-900">Informations du scénario</h2>
          <button onClick={onClose} className="p-1 rounded-md hover:bg-neutral-100 text-neutral-500 transition-colors">
            <X size={16} />
          </button>
        </div>

        <form onSubmit={submit} className="space-y-4">
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">
              Nom <span className="text-danger">*</span>
            </label>
            <input
              type="text"
              value={draftName}
              onChange={(e) => setDraftName(e.target.value)}
              placeholder="Ex : Référence RCP 8.5"
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition"
              autoFocus
              maxLength={200}
            />
          </div>
          <div>
            <label className="block text-sm font-medium text-neutral-700 mb-1">Description</label>
            <textarea
              value={draftDesc}
              onChange={(e) => setDraftDesc(e.target.value)}
              rows={3}
              placeholder="Description optionnelle…"
              className="w-full rounded-lg border border-neutral-300 px-3 py-2 text-sm focus:outline-none focus:border-primary focus:ring-1 focus:ring-primary transition resize-none"
            />
          </div>
          <div className="flex justify-end gap-2 pt-1">
            <Button variant="secondary" size="sm" type="button" onClick={onClose}>Annuler</Button>
            <Button variant="primary" size="sm" type="submit" disabled={!draftName.trim()}>Valider</Button>
          </div>
        </form>
      </div>
    </div>
  )
}

interface ParamFieldsProps {
  specs: ParameterSpec[]
  resolve: (gamlName: string) => unknown
  onChange: (gamlName: string, value: unknown) => void
  onReset: (gamlName: string) => void
  byName: Map<string, ParameterSpec>
  dataOptions: Record<string, string[]>
}

/** Libellé du paramètre dont dépend l'activation (1er identifiant de l'expression enabledIf). */
function dependencyLabel(expr: string | null | undefined, byName: Map<string, ParameterSpec>): string | null {
  if (!expr) return null
  const m = expr.match(/^(\w+)/)
  if (!m) return null
  return byName.get(m[1])?.label ?? m[1]
}

function ParamFields({ specs, resolve, onChange, onReset, byName, dataOptions }: ParamFieldsProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 gap-x-5 gap-y-3">
      {specs.map((s) => {
        const value = resolve(s.gamlName)
        const def = byName.get(s.gamlName)?.defaultValue
        const overridden = isOverridden(value, def)
        const enabled = isVisible(s.enabledIf, resolve) // même grammaire que visibleIf
        const options = s.optionsDataSpec ? dataOptions[s.gamlName] ?? [] : undefined
        const depLabel = dependencyLabel(s.enabledIf, byName)

        if (s.type === 'BOOLEAN') {
          return (
            <label
              key={s.gamlName}
              className={`flex items-center gap-2 text-[13px] ${enabled ? 'text-neutral-700 cursor-pointer' : 'text-neutral-400 cursor-not-allowed'}`}
              title={s.gamlName}
            >
              <input
                type="checkbox"
                checked={Boolean(value)}
                disabled={!enabled}
                onChange={(e) => onChange(s.gamlName, e.target.checked)}
                className="accent-primary-600"
              />
              <span className={overridden ? 'font-medium text-primary-700' : ''}>{s.label}</span>
            </label>
          )
        }

        return (
          <div key={s.gamlName}>
            <div className="flex items-center justify-between gap-2 mb-1">
              <label className={`text-xs font-medium truncate ${enabled ? 'text-neutral-600' : 'text-neutral-400'}`} title={s.gamlName}>
                {s.label}
                {s.unit && <span className="ml-1 font-normal text-neutral-400">({s.unit})</span>}
              </label>
              {overridden && enabled && (
                <button
                  type="button"
                  onClick={() => onReset(s.gamlName)}
                  className="inline-flex items-center gap-0.5 text-[10px] text-neutral-400 hover:text-primary transition-colors shrink-0"
                  title="Réinitialiser au défaut"
                >
                  <RotateCcw size={10} /> défaut
                </button>
              )}
            </div>
            <ParamInput
              spec={s}
              value={value}
              overridden={overridden}
              disabled={!enabled}
              options={options}
              onChange={(v) => onChange(s.gamlName, v)}
            />
            {!enabled && depLabel && (
              <p className="mt-0.5 text-[10px] text-neutral-400">Cochez « {depLabel} » pour activer.</p>
            )}
          </div>
        )
      })}
    </div>
  )
}

function ParamInput({
  spec,
  value,
  overridden,
  disabled,
  options,
  onChange,
}: {
  spec: ParameterSpec
  value: unknown
  overridden: boolean
  disabled?: boolean
  options?: string[]
  onChange: (v: unknown) => void
}) {
  const cls = [
    'w-full rounded-lg border px-2.5 py-1.5 text-sm transition-colors',
    'focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-primary-400',
    disabled
      ? 'border-neutral-200 bg-neutral-100 text-neutral-400 cursor-not-allowed'
      : overridden
        ? 'border-primary-300 bg-primary-50/40'
        : 'border-neutral-200 bg-white',
  ].join(' ')

  const hasOptions = !!options && options.length > 0

  // Valeurs imposées par le catalogue (ENUM).
  if (spec.type === 'ENUM' && spec.allowedValues) {
    return (
      <select value={String(value ?? '')} disabled={disabled} onChange={(e) => onChange(e.target.value)} className={cls}>
        {spec.allowedValues.map((v) => (
          <option key={v} value={v}>{v === '' ? '— (défaut)' : v}</option>
        ))}
      </select>
    )
  }

  // Select issu des données (valeurs proposées par un DataSpec du projet).
  if (hasOptions && spec.type !== 'STRING_LIST') {
    const current = String(value ?? '')
    const known = options!.includes(current)
    return (
      <select value={current} disabled={disabled} onChange={(e) => onChange(e.target.value)} className={cls}>
        <option value="">— choisir —</option>
        {!known && current !== '' && <option value={current}>{current} (hors données)</option>}
        {options!.map((v) => (
          <option key={v} value={v}>{v}</option>
        ))}
      </select>
    )
  }

  if (spec.type === 'INTEGER' || spec.type === 'FLOAT') {
    return (
      <input
        type="number"
        step={spec.type === 'FLOAT' ? 'any' : 1}
        value={value === null || value === undefined ? '' : String(value)}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value === '' ? null : Number(e.target.value))}
        className={cls}
      />
    )
  }

  if (spec.type === 'STRING_LIST') {
    const arr = (Array.isArray(value) ? value : []).map(String)
    // Avec des valeurs proposées par les données : vraie multi-sélection par puces (chips).
    if (hasOptions) {
      return (
        <TagMultiSelect
          values={arr}
          options={options!}
          disabled={disabled}
          overridden={overridden}
          onChange={(next) => onChange(next)}
        />
      )
    }
    // Sans données de référence : saisie libre séparée par des virgules.
    return (
      <input
        type="text"
        value={arr.join(', ')}
        disabled={disabled}
        onChange={(e) => onChange(e.target.value.split(',').map((s) => s.trim()).filter(Boolean))}
        placeholder="valeurs séparées par des virgules"
        className={cls}
      />
    )
  }

  return (
    <input type="text" value={String(value ?? '')} disabled={disabled} onChange={(e) => onChange(e.target.value)} className={cls} />
  )
}

/**
 * Multi-sélection par puces pour les STRING_LIST alimentées par les données (IDs de parcelles,
 * d'exploitations, de ZH…). Ajout via liste déroulante filtrable (scalable pour de longues listes),
 * retrait par la croix. Une valeur hors liste reste saisissable (Entrée) — tolérance aux IDs absents.
 */
function TagMultiSelect({
  values,
  options,
  disabled,
  overridden,
  onChange,
}: {
  values: string[]
  options: string[]
  disabled?: boolean
  overridden?: boolean
  onChange: (v: string[]) => void
}) {
  const [draft, setDraft] = useState('')
  const listId = useId()
  const available = options.filter((o) => !values.includes(o))

  const add = (raw: string) => {
    const v = raw.trim()
    setDraft('')
    if (!v || values.includes(v)) return
    onChange([...values, v])
  }
  const remove = (v: string) => onChange(values.filter((x) => x !== v))

  const wrap = [
    'w-full rounded-lg border px-2 py-1.5 flex flex-wrap items-center gap-1 min-h-[2.25rem] transition-colors',
    disabled
      ? 'border-neutral-200 bg-neutral-100 cursor-not-allowed'
      : overridden
        ? 'border-primary-300 bg-primary-50/40'
        : 'border-neutral-200 bg-white',
  ].join(' ')

  return (
    <div className={wrap}>
      {values.map((v) => (
        <span
          key={v}
          className="inline-flex items-center gap-1 rounded-md bg-primary-100 text-primary-700 text-xs px-1.5 py-0.5"
        >
          {v}
          {!disabled && (
            <button type="button" onClick={() => remove(v)} className="hover:text-danger" title="Retirer">
              <X size={11} />
            </button>
          )}
        </span>
      ))}
      {!disabled && (
        <input
          list={listId}
          value={draft}
          onChange={(e) => {
            const val = e.target.value
            if (options.includes(val)) add(val) // sélection dans la liste déroulante
            else setDraft(val)
          }}
          onKeyDown={(e) => {
            if (e.key === 'Enter') {
              e.preventDefault()
              add(draft)
            } else if (e.key === 'Backspace' && draft === '' && values.length > 0) {
              remove(values[values.length - 1])
            }
          }}
          placeholder={values.length ? 'ajouter…' : 'choisir ou saisir…'}
          className="flex-1 min-w-[7rem] bg-transparent text-sm outline-none"
        />
      )}
      <datalist id={listId}>
        {available.map((o) => (
          <option key={o} value={o} />
        ))}
      </datalist>
    </div>
  )
}
