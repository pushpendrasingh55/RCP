import { useState } from 'react'
import type { ReactNode } from 'react'
import { useIdentity } from './hooks/useIdentity'
import { IdentitySwitcher } from './components/IdentitySwitcher'
import { ApplicantFormPage } from './pages/ApplicantFormPage'
import { MyApplicationsPage } from './pages/MyApplicationsPage'
import { OfficerQueuePage } from './pages/OfficerQueuePage'
import { ApplicationDetailPage } from './pages/ApplicationDetailPage'

type Tab = 'apply' | 'my-applications' | 'officer-queue'

// A small, dependency-free view router. The app is only three top-level screens plus a detail
// screen, so a full routing library would be more machinery than the task needs.
export default function App() {
  const { identity, setIdentity } = useIdentity()
  const [tab, setTab] = useState<Tab>('apply')
  const [selectedApplicationNumber, setSelectedApplicationNumber] = useState<string | null>(null)

  const isOfficer = identity.role === 'VERIFIER' || identity.role === 'APPROVER'

  function selectAndShowDetail(applicationNumber: string) {
    setSelectedApplicationNumber(applicationNumber)
  }

  function backFromDetail() {
    setSelectedApplicationNumber(null)
  }

  // The main content area shows the detail page whenever selectedApplicationNumber is set,
  // regardless of `tab` - so switching tabs while a detail page is open must also clear it,
  // otherwise the detail page keeps rendering on top of the newly-selected tab.
  function goToTab(next: Tab) {
    setSelectedApplicationNumber(null)
    setTab(next)
  }

  return (
    <div className="min-h-screen bg-gray-50 pb-16">
      <header className="border-b border-gray-200 bg-white">
        <div className="mx-auto max-w-4xl px-4 py-3">
          <h1 className="text-lg font-bold text-gray-900">Road Cutting Permission</h1>
          <p className="text-xs text-gray-500">{identity.tenantId} municipal corporation</p>
        </div>
      </header>

      <div className="mx-auto max-w-4xl px-4 py-3">
        <IdentitySwitcher identity={identity} onChange={setIdentity} />
      </div>

      {!isOfficer && (
        <nav className="mx-auto max-w-4xl px-4">
          <div className="flex gap-2 border-b border-gray-200">
            <TabButton active={tab === 'apply' && !selectedApplicationNumber} onClick={() => goToTab('apply')}>
              Apply
            </TabButton>
            <TabButton
              active={tab === 'my-applications' && !selectedApplicationNumber}
              onClick={() => goToTab('my-applications')}
            >
              My applications
            </TabButton>
          </div>
        </nav>
      )}

      <main className="mx-auto max-w-4xl px-4 py-6">
        {selectedApplicationNumber ? (
          <ApplicationDetailPage
            identity={identity}
            applicationNumber={selectedApplicationNumber}
            onBack={backFromDetail}
          />
        ) : isOfficer ? (
          <OfficerQueuePage identity={identity} onSelect={selectAndShowDetail} />
        ) : tab === 'apply' ? (
          <ApplicantFormPage identity={identity} />
        ) : (
          <MyApplicationsPage identity={identity} onSelect={selectAndShowDetail} />
        )}
      </main>
    </div>
  )
}

function TabButton({
  active,
  onClick,
  children,
}: {
  active: boolean
  onClick: () => void
  children: ReactNode
}) {
  return (
    <button
      onClick={onClick}
      className={`border-b-2 px-3 py-2 text-sm font-medium ${
        active ? 'border-blue-600 text-blue-600' : 'border-transparent text-gray-500 hover:text-gray-700'
      }`}
    >
      {children}
    </button>
  )
}
