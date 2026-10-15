import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  server: {
    port: 5173,
    // Forward upload calls to the local Node server (keeps R2 keys off the browser).
    proxy: {
      '/api': 'http://localhost:3001',
    },
  },
});
