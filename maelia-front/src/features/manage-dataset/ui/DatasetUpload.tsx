import { useRef, useState } from 'react'
import { UploadCloud, CheckCircle2, AlertCircle, FileText, Loader2 } from 'lucide-react'
import { useMutation, useQueryClient } from '@tanstack/react-query'
import { importCsv } from '../api/dataset.api'
import { queryKeys } from '@/shared/api'

import type { Orientation } from '@/entities/dataset'

interface DatasetUploadProps {
  projectId: string
  dataSpecId: string
  csvFormat?: string | null
  orientation?: Orientation
  delimiter?: string | null
  onImported?: (datasetId: string) => void
}

export function DatasetUpload({ projectId, dataSpecId, csvFormat, orientation = 'FIELDS_AS_COLUMNS', delimiter, onImported }: DatasetUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [fileName, setFileName] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const queryClient = useQueryClient()

  const { mutate, isPending, isSuccess, isError, error } = useMutation({
    mutationFn: (file: File) => importCsv(projectId, dataSpecId, file),
    onSuccess: (dataset) => {
      queryClient.invalidateQueries({ queryKey: queryKeys.datasets.all(projectId) })
      queryClient.setQueryData(queryKeys.datasets.detail(dataset.id), dataset)
      onImported?.(dataset.id)
    },
  })

  const handleFile = (file: File | undefined) => {
    if (!file) return
    setFileName(file.name)
    mutate(file)
  }

  const onDrop = (e: React.DragEvent) => {
    e.preventDefault()
    setDragging(false)
    handleFile(e.dataTransfer.files?.[0])
  }

  return (
    <div className="space-y-3">
      <input
        ref={inputRef}
        type="file"
        accept=".csv,.txt"
        onChange={(e) => handleFile(e.target.files?.[0])}
        className="hidden"
      />

      {/* Zone de dépôt */}
      <div
        onClick={() => inputRef.current?.click()}
        onDragOver={(e) => {
          e.preventDefault()
          setDragging(true)
        }}
        onDragLeave={() => setDragging(false)}
        onDrop={onDrop}
        className={[
          'flex flex-col items-center justify-center gap-2.5 rounded-2xl border-2 border-dashed p-10 cursor-pointer transition-colors',
          dragging
            ? 'border-primary bg-primary-50'
            : 'border-neutral-300 bg-neutral-50 hover:border-primary-400 hover:bg-primary-50/50',
        ].join(' ')}
      >
        <div className="flex h-12 w-12 items-center justify-center rounded-full bg-white border border-neutral-200 shadow-sm">
          {isPending ? (
            <Loader2 size={22} className="text-primary animate-spin" />
          ) : (
            <UploadCloud size={22} className="text-primary-500" />
          )}
        </div>
        <p className="text-[14px] font-medium text-neutral-700">
          {fileName ?? 'Glissez un fichier CSV ici, ou cliquez pour parcourir'}
        </p>
        <p className="text-[12px] text-neutral-400">Formats acceptés : .csv, .txt</p>
      </div>

      {/* Format attendu */}
      <div className="rounded-xl border border-neutral-200 bg-white p-3 space-y-1.5">
        <p className="text-[12px] font-medium text-neutral-600 flex items-center gap-1.5">
          <FileText size={13} className="text-neutral-400" /> Format attendu
        </p>
        <p className="text-[12px] text-neutral-500">
          {orientation === 'FIELDS_AS_ROWS' ? (
            <>Fichier <strong>transposé</strong> : chaque <strong>ligne</strong> = un champ (1re colonne = nom du champ), chaque <strong>colonne</strong> = un enregistrement.</>
          ) : (
            <>Fichier standard : 1re <strong>ligne</strong> = entête des champs, chaque ligne suivante = un enregistrement.</>
          )}
          {' '}Délimiteur : <code className="font-mono">{delimiter || ';'}</code>.
        </p>
        {csvFormat && (
          <code className="block text-[11px] font-mono text-neutral-400 break-all">csvFormat: {csvFormat}</code>
        )}
      </div>

      {/* États */}
      {isSuccess && (
        <p className="text-sm text-success flex items-center gap-1.5">
          <CheckCircle2 size={15} /> Fichier importé avec succès.
        </p>
      )}
      {isError && (
        <p className="text-sm text-danger flex items-center gap-1.5">
          <AlertCircle size={15} /> {(error as Error).message}
        </p>
      )}
    </div>
  )
}
