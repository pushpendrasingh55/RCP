import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

// Standard Vite + React setup. The dev server proxies /rcp calls to the backend so the frontend
// can just call relative URLs (see src/api/client.ts) both in dev and once built/served for real.
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/rcp': {
        target: 'http://localhost:8080',
        changeOrigin: true,
      },
    },
  },
})
