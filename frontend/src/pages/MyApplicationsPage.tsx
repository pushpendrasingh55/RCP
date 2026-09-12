import { useEffect, useState } from 'react'
import type { Identity } from '../hooks/useIdentity'
import { ApiError, searchApplications } from '../api/client'
import type { ApplicationResponse } from '../types/api'
import { StatusBadge } from '../components/StatusBadge'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'

function formatRupees(amount: number): string {
  return `\u20b9${amount.toLocaleString('en-IN')}`
}

// The backend restricts an APPLICANT-role caller's search results to their own applications
// server-side (see ApplicationService.search) - this page does not need to, and does not, filter
// client-side. It simply renders whatever the API returns for the current identity.
export function MyApplicationsPage({
  identity,
  onSelect,
}: {
  identity: Identity
  onSelect: (applicationNumber: string) => void
}) {
  const [applications, setApplications] = useState<ApplicationResponse[] | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  function load() {
    setLoading(true)
    setError(null)
    searchApplications(identity, {})
      .then((res) => setApplications(res.applications))
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Could not load your applications.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [identity.tenantId, identity.uuid])

  if (loading) return <Spinner label="Loading your applications..." />
  if (error) return <ErrorBanner message={error} onRetry={load} />
  if (!applications || applications.length === 0) {
    return <p className="py-8 text-center text-sm text-gray-500">You have not submitted any applications yet.</p>
  }

  return (
    <ul className="mx-auto max-w-xl space-y-2">
      {applications.map((app) => (
        <li key={app.applicationNumber}>
          <button
            onClick={() => onSelect(app.applicationNumber)}
            className="flex w-full items-center justify-between rounded-lg border border-gray-200 bg-white p-3 text-left shadow-sm hover:border-blue-300"
          >
            <div>
              <p className="font-mono text-sm font-semibold text-gray-900">{app.applicationNumber}</p>
              <p className="text-xs text-gray-500">
                {app.roadType} &middot; {app.areaInSqm} sqm &middot; {formatRupees(app.totalAmount)}
              </p>
            </div>
            <StatusBadge status={app.status} />
          </button>
        </li>
      ))}
    </ul>
  )
}
