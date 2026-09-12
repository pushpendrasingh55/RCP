// A consistent, actionable error display. Every page uses this instead of rolling its own -
// the spec requires that "an error tells the user what to do next", not just that it appears.
export function ErrorBanner({ message, onRetry }: { message: string; onRetry?: () => void }) {
  return (
    <div className="flex items-start justify-between gap-3 rounded-md border border-red-200 bg-red-50 p-3 text-sm text-red-800">
      <span>{message}</span>
      {onRetry && (
        <button onClick={onRetry} className="shrink-0 font-medium underline hover:text-red-900">
          Retry
        </button>
      )}
    </div>
  )
}
