import { useState } from 'react'
import type { WorkflowActionCode } from '../types/api'

// The set of buttons rendered here comes directly from `availableActions` on the application, which
// the backend computes from workflow-config.json for the CURRENT caller's role. A VERIFIER and an
// APPROVER looking at the same application will see different buttons because the API told them to,
// not because of a switch statement living in this component.
const ACTION_LABELS: Record<WorkflowActionCode, string> = {
  VERIFY: 'Verify',
  SEND_BACK: 'Send back',
  APPROVE: 'Approve',
  REJECT: 'Reject',
  CANCEL: 'Cancel application',
}

// Actions where asking for a reason first is good practice (the backend accepts a comment on any
// action, but only these make sense to prompt for one).
const ACTIONS_WITH_COMMENT: WorkflowActionCode[] = ['SEND_BACK', 'REJECT', 'CANCEL']

export function ActionControls({
  availableActions,
  onAction,
  busy,
}: {
  availableActions: WorkflowActionCode[]
  onAction: (action: WorkflowActionCode, comment?: string) => void
  busy: boolean
}) {
  const [pendingAction, setPendingAction] = useState<WorkflowActionCode | null>(null)
  const [comment, setComment] = useState('')

  if (availableActions.length === 0) {
    return <p className="text-sm text-gray-500">No actions available for you on this application.</p>
  }

  if (pendingAction) {
    return (
      <div className="space-y-2">
        <label className="block text-sm font-medium text-gray-700">
          Reason (optional)
          <textarea
            className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
            rows={2}
            value={comment}
            onChange={(e) => setComment(e.target.value)}
            placeholder="Add a note for the record..."
          />
        </label>
        <div className="flex gap-2">
          <button
            className="rounded-md bg-blue-600 px-3 py-2 text-sm font-medium text-white hover:bg-blue-700 disabled:opacity-50"
            disabled={busy}
            onClick={() => onAction(pendingAction, comment || undefined)}
          >
            Confirm {ACTION_LABELS[pendingAction]}
          </button>
          <button
            className="rounded-md border border-gray-300 px-3 py-2 text-sm text-gray-700 hover:bg-gray-50"
            disabled={busy}
            onClick={() => {
              setPendingAction(null)
              setComment('')
            }}
          >
            Cancel
          </button>
        </div>
      </div>
    )
  }

  return (
    <div className="flex flex-wrap gap-2">
      {availableActions.map((action) => (
        <button
          key={action}
          disabled={busy}
          className={`rounded-md px-3 py-2 text-sm font-medium disabled:opacity-50 ${
            action === 'REJECT' || action === 'CANCEL'
              ? 'bg-red-600 text-white hover:bg-red-700'
              : 'bg-blue-600 text-white hover:bg-blue-700'
          }`}
          onClick={() => {
            if (ACTIONS_WITH_COMMENT.includes(action)) {
              setPendingAction(action)
            } else {
              onAction(action)
            }
          }}
        >
          {ACTION_LABELS[action]}
        </button>
      ))}
    </div>
  )
}
