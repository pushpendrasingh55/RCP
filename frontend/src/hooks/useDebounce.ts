import { useEffect, useState } from 'react'

// Returns `value`, but only after it has stopped changing for `delayMs`. Used to debounce the live
// fee preview so we don't fire a _calculate request on every single keystroke.
export function useDebounce<T>(value: T, delayMs: number): T {
  const [debounced, setDebounced] = useState(value)

  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delayMs)
    // If `value` changes again before delayMs elapses, cancel the pending update and start over.
    return () => clearTimeout(timer)
  }, [value, delayMs])

  return debounced
}
