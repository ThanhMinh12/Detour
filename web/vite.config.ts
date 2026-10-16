import { defineConfig } from 'vite';
import react from '@vitejs/plugin-react';

export default defineConfig({
  plugins: [react()],
  build: {outDir: '../target/web', emptyOutDir: true},
  server: {proxy: {'/api': 'http://127.0.0.1:8080', '/actuator': 'http://127.0.0.1:8080'}},
});
