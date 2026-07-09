import { useState, useCallback } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { Plus, Trash2, Save, CheckCircle2, AlertCircle, ShieldCheck } from 'lucide-react'
import { Button } from '@/shared/ui'
import { upsertRecords, validateDataset } from '../api/dataset.api'
import { queryKeys } from '@/shared/api'
import type { ValidationReportDto, Orientation } from '@/entities/dataset'

interface FieldDef {
  label: string
  infoType: string
  unit?: string | null
  description?: string | null
  required: boolean
  referencesDataSpec?: string | null
  allowedValues: string[]
}

interface DatasetGridProps {
  datasetId: string
  projectId: string
  fields: FieldDef[]
  initialRecords: Record<string, unknown>[]
  referentialOptions?: Record<string, string[]>
  /** Oriente le rendu : champs en colonnes (défaut) ou champs en lignes (transposé). */
  orientation?: Orientation
}

function htmlInputType(infoType: string): 'number' | 'text' {
  return infoType === 'Integer' || infoType === 'Double' ? 'number' : 'text'
}

function slug(s: string): string {
  return s.replace(/[^a-zA-Z0-9_-]/g, '_')
}

export function DatasetGrid({
  datasetId,
  projectId,
  fields,
  initialRecords,
  referentialOptions = {},
  orientation = 'FIELDS_AS_COLUMNS',
}: DatasetGridProps) {
  const queryClient = useQueryClient()
  const transposed = orientation === 'FIELDS_AS_ROWS'
  const [rows, setRows] = useState<Record<string, unknown>[]>(
    initialRecords.length > 0 ? initialRecords : [emptyRow(fields)],
  )
  const [report, setReport] = useState<ValidationReportDto | null>(null)
  const [dirty, setDirty] = useState(false)

  const saveMutation = useMutation({
    mutationFn: () => upsertRecords(datasetId, rows),
    onSuccess: (ds) => {
      setDirty(false)
      queryClient.setQueryData(queryKeys.datasets.detail(datasetId), ds)
      queryClient.invalidateQueries({ queryKey: queryKeys.datasets.all(projectId) })
    },
  })

  const validateMutation = useMutation({
    mutationFn: () => validateDataset(datasetId),
    onSuccess: (r) => {
      setReport(r)
      queryClient.invalidateQueries({ queryKey: queryKeys.datasets.all(projectId) })
    },
  })

  const updateCell = useCallback((rowIdx: number, field: string, value: string) => {
    setDirty(true)
    setRows((prev) => prev.map((row, i) => (i === rowIdx ? { ...row, [field]: value } : row)))
  }, [])

  const addRecord = () => {
    setDirty(true)
    setRows((prev) => [...prev, emptyRow(fields)])
  }

  const removeRecord = (idx: number) => {
    setDirty(true)
    setRows((prev) => prev.filter((_, i) => i !== idx))
  }

  const issuesByRow =
    report?.issues.reduce<Record<number, string[]>>((acc, issue) => {
      const r = issue.rowIndex ?? -1
      ;(acc[r] ??= []).push(`${issue.field ?? ''}: ${issue.message}`)
      return acc
    }, {}) ?? {}

  const cell = (rowIdx: number, f: FieldDef) => {
    const refValues = f.referencesDataSpec ? referentialOptions[f.referencesDataSpec] ?? [] : []
    const value = String(rows[rowIdx]?.[f.label] ?? '')
    const onChange = (v: string) => updateCell(rowIdx, f.label, v)
    if (f.allowedValues.length > 0) {
      return (
        <select value={value} onChange={(e) => onChange(e.target.value)} className={selectCls}>
          <option value="">—</option>
          {f.allowedValues.map((v) => <option key={v} value={v}>{v}</option>)}
        </select>
      )
    }
    if (refValues.length > 0) {
      return (
        <>
          <input type="text" list={`ref-${slug(f.label)}`} value={value} onChange={(e) => onChange(e.target.value)} className={inputCls} />
          <datalist id={`ref-${slug(f.label)}`}>
            {refValues.map((v) => <option key={v} value={v} />)}
          </datalist>
        </>
      )
    }
    return (
      <input
        type={htmlInputType(f.infoType)}
        inputMode={htmlInputType(f.infoType) === 'number' ? 'decimal' : undefined}
        placeholder={f.infoType === 'Date' ? 'j/m/aaaa' : undefined}
        value={value}
        onChange={(e) => onChange(e.target.value)}
        className={inputCls}
      />
    )
  }

  return (
    <div className="space-y-3">
      {/* Barre d'outils */}
      <div className="flex items-center justify-between gap-3 flex-wrap">
        <p className="text-[13px] text-neutral-500">
          {rows.length} enregistrement(s)
          {transposed && <span className="ml-2 text-[11px] text-amber-600">· fichier transposé (champs en lignes)</span>}
          {dirty && <span className="ml-2 text-warning font-medium">· modifications non enregistrées</span>}
        </p>
        <div className="flex items-center gap-2">
          <Button variant="ghost" size="sm" onClick={addRecord}>
            <Plus size={14} /> {transposed ? 'Ajouter une colonne' : 'Ajouter une ligne'}
          </Button>
          <Button variant="secondary" size="sm" onClick={() => saveMutation.mutate()} loading={saveMutation.isPending}>
            <Save size={14} /> Enregistrer
          </Button>
          <Button variant="primary" size="sm" onClick={() => validateMutation.mutate()} loading={validateMutation.isPending}>
            <ShieldCheck size={14} /> Valider
          </Button>
        </div>
      </div>

      {/* Grille */}
      <div className="overflow-x-auto rounded-xl border border-neutral-200 bg-white">
        {transposed ? (
          /* ----- Champs en LIGNES, enregistrements en COLONNES ----- */
          <table className="w-full text-sm border-collapse">
            <thead className="sticky top-0 z-10">
              <tr className="bg-neutral-50 border-b border-neutral-200">
                <th className="text-left px-3 py-2 font-medium text-neutral-600 whitespace-nowrap">Champ</th>
                {rows.map((_, idx) => (
                  <th key={idx} className="px-2 py-2 text-[11px] font-medium text-neutral-500 whitespace-nowrap">
                    <span className="inline-flex items-center gap-1">
                      #{idx + 1}
                      <button onClick={() => removeRecord(idx)} className="text-neutral-300 hover:text-danger" title="Supprimer cet enregistrement">
                        <Trash2 size={12} />
                      </button>
                    </span>
                  </th>
                ))}
              </tr>
            </thead>
            <tbody>
              {fields.map((f) => (
                <tr key={f.label} className="border-b border-neutral-100 last:border-0">
                  <td className="px-3 py-1 font-medium text-neutral-700 whitespace-nowrap" title={f.description ?? undefined}>
                    {f.label}
                    {f.required && <span className="text-danger ml-0.5">*</span>}
                    {f.unit && <span className="ml-1 font-normal text-neutral-400">({f.unit})</span>}
                  </td>
                  {rows.map((_, rowIdx) => (
                    <td key={rowIdx} className="px-2 py-1">{cell(rowIdx, f)}</td>
                  ))}
                </tr>
              ))}
            </tbody>
          </table>
        ) : (
          /* ----- Champs en COLONNES, enregistrements en LIGNES (standard) ----- */
          <table className="w-full text-sm border-collapse">
            <thead className="sticky top-0 z-10">
              <tr className="bg-neutral-50 border-b border-neutral-200">
                <th className="w-10 px-2 py-2 text-[11px] font-medium text-neutral-400 text-center">#</th>
                {fields.map((f) => (
                  <th key={f.label} title={f.description ?? undefined} className="text-left px-3 py-2 font-medium text-neutral-600 whitespace-nowrap">
                    {f.label}
                    {f.required && <span className="text-danger ml-0.5">*</span>}
                    {f.unit && <span className="ml-1 font-normal text-neutral-400">({f.unit})</span>}
                  </th>
                ))}
                <th className="w-10" />
              </tr>
            </thead>
            <tbody>
              {rows.map((_, rowIdx) => {
                const rowHasIssue = !!issuesByRow[rowIdx]
                return (
                  <tr key={rowIdx} className={`border-b border-neutral-100 last:border-0 ${rowHasIssue ? 'bg-danger/5' : 'hover:bg-neutral-50/60'}`}>
                    <td className="px-2 py-1 text-center text-[11px] text-neutral-300 tabular-nums select-none">{rowIdx + 1}</td>
                    {fields.map((f) => (
                      <td key={f.label} className="px-2 py-1">{cell(rowIdx, f)}</td>
                    ))}
                    <td className="px-1 text-center">
                      <button onClick={() => removeRecord(rowIdx)} className="p-1 rounded text-neutral-300 hover:text-danger hover:bg-danger/10 transition-colors" title="Supprimer la ligne">
                        <Trash2 size={13} />
                      </button>
                    </td>
                  </tr>
                )
              })}
            </tbody>
          </table>
        )}
      </div>

      {rows.length === 0 && (
        <p className="text-[13px] text-neutral-400 text-center py-3">
          Aucun enregistrement. Cliquez sur « {transposed ? 'Ajouter une colonne' : 'Ajouter une ligne'} » pour commencer.
        </p>
      )}

      {/* Rapport de validation */}
      {report && !report.valid && (
        <div className="rounded-xl border border-danger/30 bg-danger/5 p-3 space-y-1.5">
          <p className="text-sm font-medium text-danger flex items-center gap-1.5">
            <AlertCircle size={15} /> {report.errorCount} erreur(s) de validation
          </p>
          {report.issues.slice(0, 6).map((issue, i) => (
            <p key={i} className="text-xs text-danger/90 pl-6">
              {issue.rowIndex != null ? `Enreg. ${issue.rowIndex + 1} · ` : ''}
              {issue.field ? `${issue.field} : ` : ''}
              {issue.message}
            </p>
          ))}
          {report.issues.length > 6 && (
            <p className="text-xs text-neutral-500 pl-6">…et {report.issues.length - 6} autre(s)</p>
          )}
        </div>
      )}
      {report?.valid && (
        <div className="rounded-xl border border-success/30 bg-success/5 p-3">
          <p className="text-sm text-success flex items-center gap-1.5 font-medium">
            <CheckCircle2 size={15} /> {report.recordCount} enregistrement(s) valides.
          </p>
        </div>
      )}
    </div>
  )
}

const inputCls =
  'w-full min-w-[90px] rounded-md border border-neutral-200 px-2 py-1 text-sm focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-primary-400'
const selectCls =
  'w-full min-w-[110px] rounded-md border border-neutral-200 px-2 py-1 text-sm bg-white focus:outline-none focus:ring-2 focus:ring-primary-300 focus:border-primary-400'

function emptyRow(fields: FieldDef[]): Record<string, unknown> {
  return Object.fromEntries(fields.map((f) => [f.label, '']))
}
