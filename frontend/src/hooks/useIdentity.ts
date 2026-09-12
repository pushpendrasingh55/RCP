import { useCallback, useEffect, useState } from 'react'
import type { Role } from '../types/api'

// Since authentication is explicitly out of scope (per spec), the portal needs SOME way to let a
// tester pick "who they are" for the purposes of building RequestInfo.userInfo on every call. This
// hook is that stand-in: it persists a small identity object to localStorage so it survives a
// refresh, and every page reads/writes it through here rather than each page reinventing storage.
// A real deployment would replace this entirely with a session/SSO-derived identity.
export interface Identity {
  uuid: string
  userName: string
  tenantId: string
  role: Role
}

const STORAGE_KEY = 'rcp.identity'

const DEFAULT_IDENTITY: Identity = {
  uuid: 'u-1',
  userName: '9990000001',
  tenantId: 'dehradun',
  role: 'APPLICANT',
}

function load(): Identity {
  try {
    const raw = localStorage.getItem(STORAGE_KEY)
    if (raw) return JSON.parse(raw) as Identity
  } catch {
    // Corrupt/blocked storage - fall back to the default identity below.
  }
  return DEFAULT_IDENTITY
}

export function useIdentity() {
  const [identity, setIdentityState] = useState<Identity>(load)

  useEffect(() => {
    localStorage.setItem(STORAGE_KEY, JSON.stringify(identity))
  }, [identity])

  const setIdentity = useCallback((next: Identity) => setIdentityState(next), [])

  return { identity, setIdentity }
}
