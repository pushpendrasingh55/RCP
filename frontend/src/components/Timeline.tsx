import type { TransitionHistoryEntry } from '../types/api'

function formatTimestamp(iso: string): string {
  return new Date(iso).toLocaleString('en-IN', {
    day: 'numeric',
    month: 'short',
    year: 'numeric',
    hour: '2-digit',
    minute: '2-digit',
  })
}

// IMPORTANT: this component does not know the lifecycle's stage list in advance. It simply renders
// whatever history entries the API returned, in order. If the backend's workflow-config.json grows a
// new state/action tomorrow, this component needs no changes to display it correctly.
export function Timeline({ history }: { history: TransitionHistoryEntry[] }) {
  if (history.length === 0) {
    return <p className="text-sm text-gray-500">No history yet.</p>
  }

  return (
    <ol className="relative border-l border-gray-200 pl-4">
      {history.map((entry, index) => (
        <li key={index} className="mb-4 last:mb-0">
          <span className="absolute -left-1.5 mt-1.5 h-3 w-3 rounded-full bg-blue-500" />
          <div className="flex flex-wrap items-baseline gap-x-2">
            <span className="text-sm font-semibold text-gray-900">{entry.action}</span>
            <span className="text-xs text-gray-400">{formatTimestamp(entry.timestamp)}</span>
          </div>
          <p className="text-xs text-gray-500">
            {entry.previousState ? `${entry.previousState} \u2192 ${entry.resultingState}` : entry.resultingState}
            {' \u00b7 '}
            {entry.actorRole}
          </p>
          {entry.comment && <p className="mt-1 text-sm italic text-gray-600">"{entry.comment}"</p>}
        </li>
      ))}
    </ol>
  )
}
