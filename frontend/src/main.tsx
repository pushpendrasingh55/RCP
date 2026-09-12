import React from 'react'
import ReactDOM from 'react-dom/client'
import App from './App'
import './index.css'

// Standard Vite/React 18 bootstrap - mounts <App/> into the single #root div in index.html.
ReactDOM.createRoot(document.getElementById('root')!).render(
  <React.StrictMode>
    <App />
  </React.StrictMode>,
)
