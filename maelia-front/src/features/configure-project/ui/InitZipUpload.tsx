import { useRef, useState } from 'react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { AlertTriangle, CheckCircle2, FileArchive, Loader2, MinusCircle, Upload, XCircle } from 'lucide-react'
import { Button } from '@/shared/ui'
import { queryKeys } from '@/shared/api'
import type { ApiError } from '@/shared/api'
import type { BulkImportEntryReport, BulkImportReport } from '@/entities/dataset'
import { importInitZip } from '../api/configureProject.api'

interface InitZipUploadProps {
  projectId: string
}

/**
 * Initialisation en masse : upload d'une archive ZIP contenant un maximum de fichiers
 * d'entrée (CSV + shapefiles). Chaque fichier est apparié au catalogue par son nom,
 * importé puis validé ; le rapport détaille le sort de chaque entrée.
 */
export function InitZipUpload({ projectId }: InitZipUploadProps) {
  const queryClient = useQueryClient()
  const inputRef = useRef<HTMLInputElement>(null)
  const [report, setReport] = useState<BulkImportReport | null>(null)

  const { mutate, isPending, error } = useMutation({
    mutationFn: (file: File) => importInitZip(projectId, file),
    onSuccess: (r) => {
      setReport(r)
      queryClient.invalidateQueries({ queryKey: queryKeys.projects.completion(projectId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.preprocessing.plan(projectId) })
      queryClient.invalidateQueries({ queryKey: queryKeys.datasets.all(projectId) })
    },
  })

  const onFile = (file: File | undefined) => {
    if (file) {
      setReport(null)
      mutate(file)
    }
    if (inputRef.current) inputRef.current.value = ''
  }

  return (
    <div className="space-y-4">
      <p className="text-sm text-neutral-500">
        Déposez une archive <strong>.zip</strong> contenant vos fichiers d&apos;entrée (CSV et
        shapefiles <span className="font-mono text-[12px]">.shp/.shx/.dbf</span>). Chaque fichier
        est reconnu par son nom (ex. <span className="font-mono text-[12px]">exploitations.csv</span>,{' '}
        <span className="font-mono text-[12px]">ZH.shp</span>), importé puis validé automatiquement.
      </p>

      <input
        ref={inputRef}
        type="file"
        accept=".zip"
        className="hidden"
        onChange={(e) => onFile(e.target.files?.[0])}
      />
      <Button variant="primary" size="sm" onClick={() => inputRef.current?.click()} loading={isPending}>
        {isPending ? <Loader2 size={14} className="animate-spin" /> : <Upload size={14} />}
        {isPending ? 'Import en cours…' : 'Importer une archive ZIP'}
      </Button>

      {error != null && (
        <p className="text-sm text-danger">
          Échec de l&apos;import : {(error as unknown as ApiError).detail ?? String(error)}
        </p>
      )}

      {report && (
        <div className="space-y-3">
          {/* Synthèse */}
          <div className="flex items-center gap-4 text-sm flex-wrap">
            <span className="inline-flex items-center gap-1.5 text-neutral-700">
              <FileArchive size={14} className="text-neutral-400" />
              <strong>{report.totalEntries}</strong> fichiers analysés
            </span>
            <span className="inline-flex items-center gap-1.5 text-neutral-700">
              <CheckCircle2 size={14} className="text-success" />
              <strong>{report.imported}</strong> importés
            </span>
            {report.invalid > 0 && (
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <AlertTriangle size={14} className="text-warning" />
                <strong>{report.invalid}</strong> à corriger
              </span>
            )}
            {report.ignored > 0 && (
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <MinusCircle size={14} className="text-neutral-400" />
                <strong>{report.ignored}</strong> non reconnus
              </span>
            )}
            {report.errors > 0 && (
              <span className="inline-flex items-center gap-1.5 text-neutral-700">
                <XCircle size={14} className="text-danger" />
                <strong>{report.errors}</strong> en erreur
              </span>
            )}
          </div>

          {/* Détail par fichier */}
          <ul className="rounded-xl border border-neutral-200 overflow-hidden bg-white divide-y divide-neutral-100 max-h-80 overflow-y-auto">
            {report.entries.map((entry) => (
              <ReportRow key={`${entry.entryName}-${entry.dataSpecId ?? 'none'}`} entry={entry} />
            ))}
          </ul>
        </div>
      )}
    </div>
  )
}

function ReportRow({ entry }: { entry: BulkImportEntryReport }) {
  const icon =
    entry.status === 'VALIDE' ? <CheckCircle2 size={15} className="text-success" />
    : entry.status === 'INVALIDE' ? <AlertTriangle size={15} className="text-warning" />
    : entry.status === 'IGNORE' ? <MinusCircle size={15} className="text-neutral-300" />
    : <XCircle size={15} className="text-danger" />

  return (
    <li className="flex items-center gap-3 px-3 py-2">
      <span className="shrink-0">{icon}</span>
      <span className="font-mono text-[12px] text-neutral-800 truncate">{entry.entryName}</span>
      {entry.recordCount > 0 && (
        <span className="text-[10px] text-neutral-400 shrink-0">{entry.recordCount} lignes</span>
      )}
      <span className="text-[12px] text-neutral-500 truncate flex-1" title={entry.message}>
        {entry.message}
      </span>
    </li>
  )
}
