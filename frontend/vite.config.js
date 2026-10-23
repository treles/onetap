import { defineConfig } from 'vite'
import react from '@vitejs/plugin-react'
import tailwindcss from '@tailwindcss/vite'

export default defineConfig({
  plugins: [react(), tailwindcss()],
  server: {
    // Em desenvolvimento, o Vite repassa /api para o Spring Boot (padrão 8080; mude com PAUTA_API).
    proxy: { '/api': process.env.PAUTA_API ?? 'http://localhost:8080' },
  },
})
