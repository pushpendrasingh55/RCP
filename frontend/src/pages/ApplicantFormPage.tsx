import { useEffect, useMemo, useState } from 'react'
import type { FormEvent } from 'react'
import type { Identity } from '../hooks/useIdentity'
import { useDebounce } from '../hooks/useDebounce'
import { ApiError, calculateFee, createApplication } from '../api/client'
import type { ApplicantType, CalculationResult } from '../types/api'
import { FeeBreakdownCard } from '../components/FeeBreakdownCard'
import { ErrorBanner } from '../components/ErrorBanner'
import { Spinner } from '../components/Spinner'

// No endpoint was specified for listing available road types, so the form hardcodes the ACTIVE
// codes from the spec's rate table (KUTCHA is deliberately excluded - it is inactive). The backend
// remains the sole source of truth: if this list ever drifts from rates.json, _calculate/_create
// will reject an inactive/unknown code regardless of what the dropdown offers.
const ROAD_TYPES: { code: string; label: string }[] = [
  { code: 'BT', label: 'Bituminous (BT)' },
  { code: 'CC', label: 'Cement Concrete (CC)' },
  { code: 'WBM', label: 'Water Bound Macadam (WBM)' },
]

interface FormState {
  roadType: string
  lengthInMeters: string
  widthInMeters: string
  durationInDays: string
  applicantType: ApplicantType
  proposedStartDate: string
}

const EMPTY_FORM: FormState = {
  roadType: 'BT',
  lengthInMeters: '',
  widthInMeters: '',
  durationInDays: '',
  applicantType: 'PRIVATE',
  proposedStartDate: '',
}

function todayIso(): string {
  return new Date().toISOString().slice(0, 10)
}

// Field-level validation, mirroring (but not replacing) the backend's Bean Validation rules -
// this only makes the form responsive; the backend still re-validates and is authoritative.
function validate(form: FormState): Partial<Record<keyof FormState, string>> {
  const errors: Partial<Record<keyof FormState, string>> = {}
  const length = Number(form.lengthInMeters)
  const width = Number(form.widthInMeters)
  const duration = Number(form.durationInDays)

  if (!form.lengthInMeters || length <= 0) errors.lengthInMeters = 'Enter a length greater than 0.'
  if (!form.widthInMeters || width <= 0) errors.widthInMeters = 'Enter a width greater than 0.'
  if (!form.durationInDays || !Number.isInteger(duration) || duration < 1) {
    errors.durationInDays = 'Enter a whole number of days, at least 1.'
  }
  if (!form.proposedStartDate) {
    errors.proposedStartDate = 'Choose a proposed start date.'
  } else if (form.proposedStartDate < todayIso()) {
    errors.proposedStartDate = 'Start date cannot be in the past.'
  }
  return errors
}

export function ApplicantFormPage({ identity }: { identity: Identity }) {
  const [form, setForm] = useState<FormState>(EMPTY_FORM)
  const [touched, setTouched] = useState<Partial<Record<keyof FormState, boolean>>>({})
  const [preview, setPreview] = useState<CalculationResult | null>(null)
  const [previewLoading, setPreviewLoading] = useState(false)
  const [previewError, setPreviewError] = useState<string | null>(null)
  const [submitting, setSubmitting] = useState(false)
  const [submitError, setSubmitError] = useState<string | null>(null)
  const [created, setCreated] = useState<{ applicationNumber: string } | null>(null)

  const errors = useMemo(() => validate(form), [form])
  const isValid = Object.keys(errors).length === 0
  const debouncedForm = useDebounce(form, 400)

  // Live preview: re-fetch from _calculate whenever the (debounced) form becomes valid. This never
  // writes anything - it is the same stateless endpoint the backend uses to compute the fee at create time.
  useEffect(() => {
    const debouncedErrors = validate(debouncedForm)
    if (Object.keys(debouncedErrors).length > 0) {
      setPreview(null)
      setPreviewError(null)
      return
    }

    let cancelled = false
    setPreviewLoading(true)
    setPreviewError(null)

    calculateFee(identity, {
      tenantId: identity.tenantId,
      roadType: debouncedForm.roadType,
      lengthInMeters: Number(debouncedForm.lengthInMeters),
      widthInMeters: Number(debouncedForm.widthInMeters),
      durationInDays: Number(debouncedForm.durationInDays),
      applicantType: debouncedForm.applicantType,
      proposedStartDate: debouncedForm.proposedStartDate,
    })
      .then((result) => {
        if (!cancelled) setPreview(result)
      })
      .catch((err) => {
        if (cancelled) return
        setPreview(null)
        setPreviewError(err instanceof ApiError ? err.message : 'Could not calculate the fee preview.')
      })
      .finally(() => {
        if (!cancelled) setPreviewLoading(false)
      })

    return () => {
      cancelled = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [debouncedForm, identity.tenantId])

  async function handleSubmit(e: FormEvent) {
    e.preventDefault()
    setTouched({
      lengthInMeters: true,
      widthInMeters: true,
      durationInDays: true,
      proposedStartDate: true,
    })
    if (!isValid) return

    setSubmitting(true)
    setSubmitError(null)
    try {
      const application = await createApplication(identity, {
        tenantId: identity.tenantId,
        roadType: form.roadType,
        lengthInMeters: Number(form.lengthInMeters),
        widthInMeters: Number(form.widthInMeters),
        durationInDays: Number(form.durationInDays),
        applicantType: form.applicantType,
        proposedStartDate: form.proposedStartDate,
      })
      setCreated({ applicationNumber: application.applicationNumber })
      setForm(EMPTY_FORM)
      setPreview(null)
    } catch (err) {
      setSubmitError(err instanceof ApiError ? err.message : 'Could not submit the application.')
    } finally {
      setSubmitting(false)
    }
  }

  if (created) {
    return (
      <div className="mx-auto max-w-md space-y-3 rounded-lg border border-green-200 bg-green-50 p-4 text-center">
        <p className="text-lg font-semibold text-green-800">Application submitted</p>
        <p className="text-sm text-green-700">
          Your application number is <span className="font-mono font-bold">{created.applicationNumber}</span>.
          Keep this for your records.
        </p>
        <button
          className="rounded-md bg-green-700 px-4 py-2 text-sm font-medium text-white hover:bg-green-800"
          onClick={() => setCreated(null)}
        >
          Apply for another permit
        </button>
      </div>
    )
  }

  return (
    <form onSubmit={handleSubmit} className="mx-auto max-w-md space-y-4">
      <div>
        <label htmlFor="roadType" className="block text-sm font-medium text-gray-700">
          Road type
        </label>
        <select
          id="roadType"
          className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
          value={form.roadType}
          onChange={(e) => setForm({ ...form, roadType: e.target.value })}
        >
          {ROAD_TYPES.map((rt) => (
            <option key={rt.code} value={rt.code}>
              {rt.label}
            </option>
          ))}
        </select>
      </div>

      <div className="grid grid-cols-2 gap-3">
        <div>
          <label htmlFor="length" className="block text-sm font-medium text-gray-700">
            Length (m)
          </label>
          <input
            id="length"
            inputMode="decimal"
            className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
            value={form.lengthInMeters}
            onChange={(e) => setForm({ ...form, lengthInMeters: e.target.value })}
            onBlur={() => setTouched({ ...touched, lengthInMeters: true })}
          />
          {touched.lengthInMeters && errors.lengthInMeters && (
            <p className="mt-1 text-xs text-red-600">{errors.lengthInMeters}</p>
          )}
        </div>
        <div>
          <label htmlFor="width" className="block text-sm font-medium text-gray-700">
            Width (m)
          </label>
          <input
            id="width"
            inputMode="decimal"
            className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
            value={form.widthInMeters}
            onChange={(e) => setForm({ ...form, widthInMeters: e.target.value })}
            onBlur={() => setTouched({ ...touched, widthInMeters: true })}
          />
          {touched.widthInMeters && errors.widthInMeters && (
            <p className="mt-1 text-xs text-red-600">{errors.widthInMeters}</p>
          )}
        </div>
      </div>

      <div>
        <label htmlFor="duration" className="block text-sm font-medium text-gray-700">
          Duration (days)
        </label>
        <input
          id="duration"
          inputMode="numeric"
          className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
          value={form.durationInDays}
          onChange={(e) => setForm({ ...form, durationInDays: e.target.value })}
          onBlur={() => setTouched({ ...touched, durationInDays: true })}
        />
        {touched.durationInDays && errors.durationInDays && (
          <p className="mt-1 text-xs text-red-600">{errors.durationInDays}</p>
        )}
      </div>

      <div>
        <label htmlFor="startDate" className="block text-sm font-medium text-gray-700">
          Proposed start date
        </label>
        <input
          id="startDate"
          type="date"
          min={todayIso()}
          className="mt-1 w-full rounded-md border border-gray-300 p-2 text-sm"
          value={form.proposedStartDate}
          onChange={(e) => setForm({ ...form, proposedStartDate: e.target.value })}
          onBlur={() => setTouched({ ...touched, proposedStartDate: true })}
        />
        {touched.proposedStartDate && errors.proposedStartDate && (
          <p className="mt-1 text-xs text-red-600">{errors.proposedStartDate}</p>
        )}
      </div>

      <fieldset>
        <legend className="block text-sm font-medium text-gray-700">Applicant type</legend>
        <div className="mt-1 flex gap-4 text-sm">
          <label className="flex items-center gap-1">
            <input
              type="radio"
              checked={form.applicantType === 'PRIVATE'}
              onChange={() => setForm({ ...form, applicantType: 'PRIVATE' })}
            />
            Private
          </label>
          <label className="flex items-center gap-1">
            <input
              type="radio"
              checked={form.applicantType === 'GOVERNMENT_AGENCY'}
              onChange={() => setForm({ ...form, applicantType: 'GOVERNMENT_AGENCY' })}
            />
            Government agency
          </label>
        </div>
      </fieldset>

      {previewLoading && <Spinner label="Calculating fee..." />}
      {previewError && <ErrorBanner message={previewError} />}
      <FeeBreakdownCard result={preview} />

      {submitError && <ErrorBanner message={submitError} />}

      <button
        type="submit"
        disabled={submitting || !isValid}
        className="w-full rounded-md bg-blue-600 py-2 text-sm font-semibold text-white hover:bg-blue-700 disabled:opacity-50"
      >
        {submitting ? 'Submitting...' : 'Submit application'}
      </button>
    </form>
  )
}
