import { useEffect, useState } from 'react'
import type { Identity } from '../hooks/useIdentity'
import { ApiError, searchApplications } from '../api/client'
import type { ApplicationResponse, ApplicationStatus } from '../types/api'
import { StatusBadge } from '../components/StatusBadge'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'

function formatRupees(amount: number): string {
  return `\u20b9${amount.toLocaleString('en-IN')}`
}

const STATUS_FILTERS: (ApplicationStatus | 'ALL')[] = [
  'ALL',
  'APPLIED',
  'PENDING_APPROVAL',
  'APPROVED',
  'REJECTED',
  'CANCELLED',
]

// A desktop-oriented queue for VERIFIER/APPROVER users. The backend already restricts what an
// officer can see to their own tenant (see ApplicationService.search); this page's status filter is
// just an additional, optional narrowing on top of that.
export function OfficerQueuePage({
  identity,
  onSelect,
}: {
  identity: Identity
  onSelect: (applicationNumber: string) => void
}) {
  const [statusFilter, setStatusFilter] = useState<ApplicationStatus | 'ALL'>('ALL')
  const [applications, setApplications] = useState<ApplicationResponse[] | null>(null)
  const [totalCount, setTotalCount] = useState(0)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)

  function load() {
    setLoading(true)
    setError(null)
    searchApplications(identity, statusFilter === 'ALL' ? {} : { status: statusFilter })
      .then((res) => {
        setApplications(res.applications)
        setTotalCount(res.totalCount)
      })
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Could not load the queue.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [identity.tenantId, identity.role, statusFilter])

  return (
    <div className="space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        {STATUS_FILTERS.map((s) => (
          <button
            key={s}
            onClick={() => setStatusFilter(s)}
            className={`rounded-full px-3 py-1 text-xs font-medium ${
              statusFilter === s ? 'bg-blue-600 text-white' : 'bg-gray-100 text-gray-700 hover:bg-gray-200'
            }`}
          >
            {s.replace('_', ' ')}
          </button>
        ))}
      </div>

      {loading && <Spinner label="Loading queue..." />}
      {error && <ErrorBanner message={error} onRetry={load} />}

      {applications && (
        <>
          <p className="text-xs text-gray-500">{totalCount} application(s)</p>
          {applications.length === 0 ? (
            <p className="py-8 text-center text-sm text-gray-500">Nothing in this queue.</p>
          ) : (
            <table className="w-full border-collapse overflow-hidden rounded-lg border border-gray-200 text-sm">
              <thead className="bg-gray-50 text-left text-xs uppercase text-gray-500">
                <tr>
                  <th className="p-3">Application #</th>
                  <th className="p-3">Road type</th>
                  <th className="p-3">Applicant mobile</th>
                  <th className="p-3">Total</th>
                  <th className="p-3">Status</th>
                  <th className="p-3" />
                </tr>
              </thead>
              <tbody className="divide-y divide-gray-100 bg-white">
                {applications.map((app) => (
                  <tr key={app.applicationNumber} className="hover:bg-gray-50">
                    <td className="p-3 font-mono">{app.applicationNumber}</td>
                    <td className="p-3">{app.roadType}</td>
                    <td className="p-3">{app.applicantMobileNumber}</td>
                    <td className="p-3">{formatRupees(app.totalAmount)}</td>
                    <td className="p-3">
                      <StatusBadge status={app.status} />
                    </td>
                    <td className="p-3 text-right">
                      <button
                        onClick={() => onSelect(app.applicationNumber)}
                        className="font-medium text-blue-600 hover:underline"
                      >
                        Open
                      </button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          )}
        </>
      )}
    </div>
  )
}
