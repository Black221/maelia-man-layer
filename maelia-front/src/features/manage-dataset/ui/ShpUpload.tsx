import { useRef, useState } from 'react'
import { UploadCloud, CheckCircle2, AlertCircle, Loader2, Map as MapIcon, FileArchive } from 'lucide-react'
import { useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { uploadShp, listDatasetFiles } from '../api/dataset.api'
import { queryKeys } from '@/shared/api'

interface ShpUploadProps {
  projectId: string
  dataSpecId: string
  /** Nom de fichier attendu par le modèle (ex. ilots.shp) : les fichiers uploadés y sont renommés. */
  expectedFileName: string
}

const filesKey = (projectId: string, dataSpecId: string) => ['dataset-files', projectId, dataSpecId]

/**
 * C8 — Upload d'un shapefile (archive .zip contenant .shp + .shx + .dbf, + .prj…).
 * Les fichiers remplacent ceux du socle à chaque matérialisation du projet.
 */
export function ShpUpload({ projectId, dataSpecId, expectedFileName }: ShpUploadProps) {
  const inputRef = useRef<HTMLInputElement>(null)
  const [fileName, setFileName] = useState<string | null>(null)
  const [dragging, setDragging] = useState(false)
  const queryClient = useQueryClient()

  const { data: files } = useQuery({
    queryKey: filesKey(projectId, dataSpecId),
    queryFn: () => listDatasetFiles(projectId, dataSpecId),
  })

  const { mutate, isPending, isSuccess, isError, error } = useMutation({
    mutationFn: (file: File) => uploadShp(projectId, dataSpecId, file),
    onSuccess: (uploaded) => {
      queryClient.setQueryData(filesKey(projectId, dataSpecId), uploaded)
      queryClient.invalidateQueries({ queryKey: queryKeys.datasets.all(projectId) })
    },
  })

  const handleFile = (file: File | undefined) => {
    if (!file) return
    setFileName(file.name)
    mutate(file)
  }

  return (
    <div className="space-y-3">
      <input
        ref={inputRef}
        type="file"
        accept=".zip"
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
        onDrop={(e) => {
          e.preventDefault()
          setDragging(false)
          handleFile(e.dataTransfer.files?.[0])
        }}
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
          {fileName ?? 'Glissez une archive .zip ici, ou cliquez pour parcourir'}
        </p>
        <p className="text-[12px] text-neutral-400">
          Archive .zip contenant le jeu shapefile : .shp, .shx, .dbf (+ .prj recommandé)
        </p>
      </div>

      {/* Format attendu */}
      <div className="rounded-xl border border-neutral-200 bg-white p-3 space-y-1.5">
        <p className="text-[12px] font-medium text-neutral-600 flex items-center gap-1.5">
          <MapIcon size={13} className="text-neutral-400" /> Couche géographique
        </p>
        <p className="text-[12px] text-neutral-500">
          Les fichiers de l&apos;archive sont renommés sur le nom attendu par le modèle
          (<code className="font-mono">{expectedFileName}</code>) et remplaceront la couche du
          socle à la prochaine simulation. Sans upload, la couche de référence du territoire est utilisée.
        </p>
      </div>

      {/* Fichiers en place */}
      {(files?.length ?? 0) > 0 && (
        <div className="rounded-xl border border-neutral-200 bg-white p-3 space-y-1.5">
          <p className="text-[12px] font-medium text-neutral-600 flex items-center gap-1.5">
            <FileArchive size={13} className="text-neutral-400" /> Fichiers uploadés
          </p>
          <ul className="space-y-0.5">
            {files!.map((f) => (
              <li key={f.fileName} className="flex items-center justify-between text-[12px] text-neutral-600">
                <span className="font-mono">{f.fileName}</span>
                <span className="text-neutral-400 tabular-nums">{formatSize(f.sizeBytes)}</span>
              </li>
            ))}
          </ul>
        </div>
      )}

      {/* États */}
      {isSuccess && (
        <p className="text-sm text-success flex items-center gap-1.5">
          <CheckCircle2 size={15} /> Shapefile uploadé : il remplacera la couche du socle à la prochaine simulation.
        </p>
      )}
      {isError && (
        <p className="text-sm text-danger flex items-center gap-1.5">
          <AlertCircle size={15} />
          {(error as { response?: { data?: { detail?: string } } })?.response?.data?.detail
            ?? (error as Error).message}
        </p>
      )}
    </div>
  )
}

function formatSize(bytes: number) {
  if (bytes >= 1_048_576) return `${(bytes / 1_048_576).toFixed(1)} Mo`
  if (bytes >= 1024) return `${(bytes / 1024).toFixed(0)} Ko`
  return `${bytes} o`
}
