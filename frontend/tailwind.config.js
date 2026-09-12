/** @type {import('tailwindcss').Config} */
// Scans every source file for class names so unused styles are purged from the production build.
export default {
  content: ['./index.html', './src/**/*.{js,ts,jsx,tsx}'],
  theme: {
    extend: {},
  },
  plugins: [],
}
