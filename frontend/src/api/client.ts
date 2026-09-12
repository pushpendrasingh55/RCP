import type {
  ApplicationResponse,
  CalculationInput,
  CalculationResult,
  ErrorResponse,
} from '../types/api'
import type { Identity } from '../hooks/useIdentity'

// Everything in this file talks to the backend using relative URLs (/rcp/v1/...). In dev, Vite's
// proxy (see vite.config.ts) forwards these to http://localhost:8081; in production the portal is
// expected to be served from behind the same origin/gateway as the API.
//
// The envelope's root keys (RequestInfo/Calculation/ResponseInfo/Errors/etc.) are capitalized to
// match the spec's contract precisely; everything nested inside them (userInfo, tenantId, roadType,
// ...) stays lowercase camelCase, exactly as the spec's own worked example shows.

// A small, human-readable message id used purely for request/response correlation in logs.
function newMsgId(): string {
  return `${Date.now()}-${Math.random().toString(36).slice(2, 8)}|en_IN`
}

function buildRequestInfo(identity: Identity) {
  return {
    apiId: 'portal',
    msgId: newMsgId(),
    userInfo: {
      uuid: identity.uuid,
      userName: identity.userName,
      tenantId: identity.tenantId,
      roles: [{ code: identity.role }],
    },
  }
}

// Thrown for any non-2xx response. Carries the parsed error list from the backend's
// { ResponseInfo, Errors } envelope so callers can show an actionable message.
export class ApiError extends Error {
  errors: { code: string; message: string }[]
  status: number

  constructor(status: number, body: ErrorResponse) {
    const first = body.Errors?.[0]
    super(first ? first.message : `Request failed with status ${status}`)
    this.status = status
    this.errors = body.Errors ?? []
  }
}

async function post<TResponse>(path: string, body: unknown): Promise<TResponse> {
  const res = await fetch(path, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })

  const payload = await res.json().catch(() => null)

  if (!res.ok) {
    // The backend always returns { ResponseInfo, Errors } for 4xx/5xx - if for some reason it
    // didn't (e.g. a network-level failure returned HTML), fall back to a generic error.
    throw new ApiError(res.status, payload ?? { ResponseInfo: { status: 'failed' }, Errors: [] })
  }

  return payload as TResponse
}

export async function calculateFee(
  identity: Identity,
  calculation: CalculationInput,
): Promise<CalculationResult> {
  const response = await post<{ Calculation: CalculationResult }>('/rcp/v1/_calculate', {
    RequestInfo: buildRequestInfo(identity),
    Calculation: calculation,
  })
  return response.Calculation
}

export async function createApplication(
  identity: Identity,
  calculation: CalculationInput,
  requestReferenceId?: string,
): Promise<ApplicationResponse> {
  const response = await post<{ Application: ApplicationResponse }>('/rcp/v1/_create', {
    RequestInfo: buildRequestInfo(identity),
    Calculation: calculation,
    RequestReferenceId: requestReferenceId,
  })
  return response.Application
}

export async function performAction(
  identity: Identity,
  applicationNumber: string,
  action: string,
  comment?: string,
): Promise<ApplicationResponse> {
  const response = await post<{ Application: ApplicationResponse }>('/rcp/v1/_action', {
    RequestInfo: buildRequestInfo(identity),
    ApplicationNumber: applicationNumber,
    Action: action,
    Comment: comment,
  })
  return response.Application
}

export interface SearchFilters {
  applicationNumber?: string
  status?: string
  mobileNumber?: string
}

export async function searchApplications(
  identity: Identity,
  filters: SearchFilters,
  offset = 0,
  limit = 20,
): Promise<{ applications: ApplicationResponse[]; totalCount: number }> {
  const response = await post<{ Applications: ApplicationResponse[]; TotalCount: number }>(
    '/rcp/v1/_search',
    {
      RequestInfo: buildRequestInfo(identity),
      SearchCriteria: filters,
      Offset: offset,
      Limit: limit,
    },
  )
  return { applications: response.Applications, totalCount: response.TotalCount }
}
