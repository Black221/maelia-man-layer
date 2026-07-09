import { useEffect, useRef } from 'react'

interface RunLogProps {
  logs: string[]
}

export function RunLog({ logs }: RunLogProps) {
  const bottomRef = useRef<HTMLDivElement>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [logs.length])

  if (logs.length === 0) {
    return (
      <div className="flex items-center justify-center h-24 text-sm text-neutral-400 rounded-lg border border-neutral-200 bg-neutral-50">
        En attente des messages de simulation...
      </div>
    )
  }

  return (
    <div className="h-64 overflow-y-auto rounded-lg border border-neutral-200 bg-neutral-900 p-3 font-mono text-xs text-neutral-200">
      {logs.map((line, i) => (
        <div key={i} className="leading-5">
          <span className="select-none text-neutral-500 mr-2">{String(i + 1).padStart(3, '0')}</span>
          {line}
        </div>
      ))}
      <div ref={bottomRef} />
    </div>
  )
}
