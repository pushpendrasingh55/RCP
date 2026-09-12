import type { ApplicationStatus } from '../types/api'

// Maps each lifecycle status to a colour so the officer queue and detail views are scannable at a
// glance. The set of statuses itself still comes entirely from the API - this is purely presentation.
const STYLES: Record<ApplicationStatus, string> = {
  APPLIED: 'bg-blue-100 text-blue-800',
  PENDING_APPROVAL: 'bg-amber-100 text-amber-800',
  APPROVED: 'bg-green-100 text-green-800',
  REJECTED: 'bg-red-100 text-red-800',
  CANCELLED: 'bg-gray-200 text-gray-700',
}

export function StatusBadge({ status }: { status: ApplicationStatus }) {
  return (
    <span className={`inline-block rounded-full px-3 py-1 text-xs font-semibold ${STYLES[status]}`}>
      {status.replace('_', ' ')}
    </span>
  )
}
