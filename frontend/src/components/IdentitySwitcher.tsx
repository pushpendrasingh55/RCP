import type { Identity } from '../hooks/useIdentity'
import type { Role } from '../types/api'

const TENANTS = ['dehradun', 'haridwar']
const ROLES: Role[] = ['APPLICANT', 'VERIFIER', 'APPROVER']

// Stands in for a login screen. Authentication is explicitly out of scope for this assignment - the
// spec says caller identity comes from RequestInfo.userInfo, so this component just lets a tester
// set that identity directly and visibly, rather than hiding it behind a fake sign-in form.
export function IdentitySwitcher({
  identity,
  onChange,
}: {
  identity: Identity
  onChange: (identity: Identity) => void
}) {
  return (
    <div className="flex flex-wrap items-center gap-3 rounded-lg border border-amber-200 bg-amber-50 p-3 text-sm">
      <span className="font-semibold text-amber-800">Acting as:</span>

      <label className="flex items-center gap-1">
        <span className="text-amber-700">Tenant</span>
        <select
          className="rounded border border-amber-300 bg-white px-2 py-1"
          value={identity.tenantId}
          onChange={(e) => onChange({ ...identity, tenantId: e.target.value })}
        >
          {TENANTS.map((t) => (
            <option key={t} value={t}>
              {t}
            </option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1">
        <span className="text-amber-700">Role</span>
        <select
          className="rounded border border-amber-300 bg-white px-2 py-1"
          value={identity.role}
          onChange={(e) => onChange({ ...identity, role: e.target.value as Role })}
        >
          {ROLES.map((r) => (
            <option key={r} value={r}>
              {r}
            </option>
          ))}
        </select>
      </label>

      <label className="flex items-center gap-1">
        <span className="text-amber-700">Mobile / user id</span>
        <input
          className="w-32 rounded border border-amber-300 bg-white px-2 py-1"
          value={identity.userName}
          onChange={(e) => onChange({ ...identity, userName: e.target.value })}
        />
      </label>

      <label className="flex items-center gap-1">
        <span className="text-amber-700">uuid</span>
        <input
          className="w-24 rounded border border-amber-300 bg-white px-2 py-1"
          value={identity.uuid}
          onChange={(e) => onChange({ ...identity, uuid: e.target.value })}
        />
      </label>
    </div>
  )
}
