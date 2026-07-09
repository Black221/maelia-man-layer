import { FileText, FileCode, Download, ImageIcon } from 'lucide-react'
import type { OutputArtifact } from '@/entities/result'
import { artifactHref } from '../api/result.api'

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} o`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} Ko`
  return `${(bytes / (1024 * 1024)).toFixed(1)} Mo`
}

export function ArtifactGallery({ artifacts }: { artifacts: OutputArtifact[] }) {
  if (artifacts.length === 0) {
    return <p className="text-sm text-neutral-400">Aucun artefact produit.</p>
  }

  const images = artifacts.filter((a) => a.type === 'IMAGE')
  const files = artifacts.filter((a) => a.type !== 'IMAGE')

  return (
    <div className="space-y-4">
      {images.length > 0 && (
        <div className="grid grid-cols-2 sm:grid-cols-3 gap-3">
          {images.map((a) => (
            <a
              key={a.id}
              href={artifactHref(a.url)}
              target="_blank"
              rel="noreferrer"
              className="group block overflow-hidden rounded-lg border border-neutral-200 bg-neutral-50 hover:border-primary transition-colors"
            >
              <img
                src={artifactHref(a.url)}
                alt={a.name}
                loading="lazy"
                className="aspect-video w-full object-cover"
              />
              <div className="flex items-center gap-1.5 px-2 py-1.5">
                <ImageIcon size={12} className="text-neutral-400 shrink-0" />
                <span className="truncate text-xs text-neutral-600">{a.name}</span>
              </div>
            </a>
          ))}
        </div>
      )}

      {files.length > 0 && (
        <ul className="divide-y divide-neutral-100 rounded-lg border border-neutral-200">
          {files.map((a) => (
            <li key={a.id} className="flex items-center gap-2 px-3 py-2">
              {a.type === 'CSV' ? (
                <FileText size={14} className="text-neutral-400 shrink-0" />
              ) : (
                <FileCode size={14} className="text-neutral-400 shrink-0" />
              )}
              <span className="truncate text-sm text-neutral-700 flex-1">{a.name}</span>
              <span className="text-xs text-neutral-400">{formatSize(a.sizeBytes)}</span>
              <a
                href={artifactHref(a.url)}
                download={a.name}
                className="p-1 rounded text-neutral-400 hover:text-primary hover:bg-primary/10 transition-colors"
                title="Télécharger"
              >
                <Download size={14} />
              </a>
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
