// These types mirror the backend DTOs (see backend/src/main/java/com/rcp/dto) field-for-field.
// Keeping the two in sync by hand is a known trade-off of not sharing a schema across the boundary -
// see README.md "Assumptions" for why we accepted this for a one-day take-home.

export type Role = 'APPLICANT' | 'VERIFIER' | 'APPROVER'

export type ApplicationStatus =
  | 'APPLIED'
  | 'PENDING_APPROVAL'
  | 'APPROVED'
  | 'REJECTED'
  | 'CANCELLED'

export type WorkflowActionCode = 'VERIFY' | 'SEND_BACK' | 'APPROVE' | 'REJECT' | 'CANCEL'

export type ApplicantType = 'PRIVATE' | 'GOVERNMENT_AGENCY'

export type RoadTypeCode = 'BT' | 'CC' | 'WBM' | 'KUTCHA'

// The caller's identity, standing in for a real login system (auth is explicitly out of scope).
export interface UserInfo {
  uuid: string
  userName: string
  tenantId: string
  roles: { code: Role }[]
}

export interface RequestInfo {
  apiId: string
  msgId: string
  userInfo: UserInfo
}

export interface CalculationInput {
  tenantId: string
  roadType: RoadTypeCode | string
  lengthInMeters: number
  widthInMeters: number
  durationInDays: number
  applicantType: ApplicantType
  proposedStartDate: string // ISO date, e.g. "2026-03-02"
}

export interface CalculationResult {
  areaInSqm: number
  restorationCharge: number
  permissionFee: number
  urgencySurcharge: number
  securityDeposit: number
  totalAmount: number
  reviewRef: string
}

export interface TransitionHistoryEntry {
  action: string
  previousState: ApplicationStatus | null
  resultingState: ApplicationStatus
  actorUuid: string
  actorRole: Role
  comment: string | null
  timestamp: string
}

export interface ApplicationResponse {
  applicationNumber: string
  tenantId: string
  roadType: string
  lengthInMeters: number
  widthInMeters: number
  areaInSqm: number
  durationInDays: number
  applicantType: ApplicantType
  proposedStartDate: string
  applicationDate: string
  restorationCharge: number
  permissionFee: number
  urgencySurcharge: number
  securityDeposit: number
  totalAmount: number
  status: ApplicationStatus
  // The actions THIS caller may legally take right now - the UI must build its buttons from this,
  // never from a hardcoded switch on `status`.
  availableActions: WorkflowActionCode[]
  applicantMobileNumber: string
  createdTime: string
  lastModifiedTime: string
  history: TransitionHistoryEntry[]
}

export interface ErrorDetail {
  code: string
  message: string
}

export interface ErrorResponse {
  ResponseInfo: { status: 'failed' }
  Errors: ErrorDetail[]
}
