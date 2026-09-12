import { useEffect, useState } from 'react'
import type { Identity } from '../hooks/useIdentity'
import { ApiError, performAction, searchApplications } from '../api/client'
import type { ApplicationResponse, WorkflowActionCode } from '../types/api'
import { StatusBadge } from '../components/StatusBadge'
import { Timeline } from '../components/Timeline'
import { ActionControls } from '../components/ActionControls'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'

function formatRupees(amount: number): string {
  return `\u20b9${amount.toLocaleString('en-IN')}`
}

export function ApplicationDetailPage({
  identity,
  applicationNumber,
  onBack,
}: {
  identity: Identity
  applicationNumber: string
  onBack: () => void
}) {
  const [application, setApplication] = useState<ApplicationResponse | null>(null)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(true)
  const [actionBusy, setActionBusy] = useState(false)
  const [actionError, setActionError] = useState<string | null>(null)

  // There is no single-item "get by number" endpoint in the spec's API contract, so a targeted
  // search (filtered to this exact applicationNumber) is used to (re)load the detail view - the same
  // tenant-scoping and role-based visibility rules apply as for any other search.
  function load() {
    setLoading(true)
    setError(null)
    searchApplications(identity, { applicationNumber })
      .then((res) => setApplication(res.applications[0] ?? null))
      .catch((err) => setError(err instanceof ApiError ? err.message : 'Could not load this application.'))
      .finally(() => setLoading(false))
  }

  useEffect(load, [identity.tenantId, identity.uuid, identity.role, applicationNumber])

  async function handleAction(action: WorkflowActionCode, comment?: string) {
    setActionBusy(true)
    setActionError(null)
    try {
      const updated = await performAction(identity, applicationNumber, action, comment)
      setApplication(updated)
    } catch (err) {
      setActionError(err instanceof ApiError ? err.message : 'Could not perform that action.')
    } finally {
      setActionBusy(false)
    }
  }

  return (
    <div className="mx-auto max-w-xl space-y-4">
      <button onClick={onBack} className="text-sm text-blue-600 hover:underline">
        &larr; Back
      </button>

      {loading && <Spinner label="Loading application..." />}
      {error && <ErrorBanner message={error} onRetry={load} />}

      {application && (
        <>
          <div className="flex items-center justify-between">
            <h2 className="font-mono text-lg font-bold text-gray-900">{application.applicationNumber}</h2>
            <StatusBadge status={application.status} />
          </div>

          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <dl className="grid grid-cols-2 gap-2 text-sm">
              <dt className="text-gray-500">Road type</dt>
              <dd className="text-right font-medium">{application.roadType}</dd>
              <dt className="text-gray-500">Dimensions</dt>
              <dd className="text-right font-medium">
                {application.lengthInMeters}m &times; {application.widthInMeters}m ({application.areaInSqm} sqm)
              </dd>
              <dt className="text-gray-500">Duration</dt>
              <dd className="text-right font-medium">{application.durationInDays} days</dd>
              <dt className="text-gray-500">Proposed start</dt>
              <dd className="text-right font-medium">{application.proposedStartDate}</dd>
              <dt className="text-gray-500">Applicant type</dt>
              <dd className="text-right font-medium">{application.applicantType}</dd>
              <dt className="mt-2 border-t border-gray-100 pt-2 text-gray-500">Total amount</dt>
              <dd className="mt-2 border-t border-gray-100 pt-2 text-right text-base font-bold">
                {formatRupees(application.totalAmount)}
              </dd>
            </dl>
          </div>

          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <h3 className="mb-3 text-sm font-semibold text-gray-700">Actions</h3>
            {actionError && <div className="mb-2"><ErrorBanner message={actionError} /></div>}
            <ActionControls
              availableActions={application.availableActions}
              onAction={handleAction}
              busy={actionBusy}
            />
          </div>

          <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
            <h3 className="mb-3 text-sm font-semibold text-gray-700">History</h3>
            <Timeline history={application.history} />
          </div>
        </>
      )}
    </div>
  )
}
