import type { CalculationResult } from '../types/api'

function formatRupees(amount: number): string {
  return `\u20b9${amount.toLocaleString('en-IN')}`
}

// Purely a rendering component: every number here comes straight from the backend's _calculate
// response. The frontend never computes or guesses a fee itself.
export function FeeBreakdownCard({ result }: { result: CalculationResult | null; }) {
  if (!result) {
    return (
      <div className="rounded-lg border border-dashed border-gray-300 p-4 text-sm text-gray-500">
        Fill in the form to see a fee preview.
      </div>
    )
  }

  const rows: [string, number][] = [
    ['Area', result.areaInSqm],
    ['Restoration charge', result.restorationCharge],
    ['Permission fee', result.permissionFee],
    ['Urgency surcharge', result.urgencySurcharge],
    ['Security deposit', result.securityDeposit],
  ]

  return (
    <div className="rounded-lg border border-gray-200 bg-white p-4 shadow-sm">
      <h3 className="mb-3 text-sm font-semibold text-gray-700">Fee preview</h3>
      <dl className="space-y-1 text-sm">
        {rows.map(([label, value]) => (
          <div key={label} className="flex justify-between">
            <dt className="text-gray-500">{label}</dt>
            <dd className="font-medium text-gray-900">
              {label === 'Area' ? `${value} sqm` : formatRupees(value)}
            </dd>
          </div>
        ))}
      </dl>
      <div className="mt-3 flex justify-between border-t border-gray-200 pt-3">
        <span className="text-sm font-semibold text-gray-700">Total amount</span>
        <span className="text-lg font-bold text-gray-900">{formatRupees(result.totalAmount)}</span>
      </div>
    </div>
  )
}
