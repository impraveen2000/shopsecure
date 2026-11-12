import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

// The dev server proxies /api to the API Gateway. The browser therefore talks to ONE origin
// (localhost:5173), so the HttpOnly SameSite=Strict refresh cookie works and no CORS is needed.
export default defineConfig({
  plugins: [react()],
  server: {
        port: 15173,
        strictPort: true,
    proxy: {
      '/api': {
        target: process.env.GATEWAY_URL || 'http://localhost:18090',
        changeOrigin: true,
      },
    },
  },
});
