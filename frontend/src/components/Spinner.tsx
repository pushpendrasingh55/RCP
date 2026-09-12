// A minimal loading indicator, used everywhere a request is in flight so the user never sees a
// silently-frozen screen.
export function Spinner({ label = 'Loading...' }: { label?: string }) {
  return (
    <div className="flex items-center gap-2 py-4 text-sm text-gray-500">
      <span className="h-4 w-4 animate-spin rounded-full border-2 border-gray-300 border-t-blue-600" />
      {label}
    </div>
  )
}
