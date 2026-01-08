import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'

export default defineConfig({
  plugins: [react()],
  server: {
    host: "127.0.0.1",     // 🔴 ÇOK ÖNEMLİ
    port: 5173,            // 🔴 Sabit port
    strictPort: true,      // Port doluysa fail et
    open: false,           // Selenium varken tarayıcı açmasın
    proxy: {
      '/api': {
        target: 'http://localhost:8080',
        changeOrigin: true,
        secure: false
      }
    }
  }
})
