import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// During development the page runs on http://localhost:5173 and every /api call
// is passed on to the Spring Boot server on port 8080, so no CORS setup is needed.
// API_URL can point it somewhere else (used later by Docker).
export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    proxy: {
      '/api': process.env.API_URL ?? 'http://localhost:8080',
    },
  },
});
