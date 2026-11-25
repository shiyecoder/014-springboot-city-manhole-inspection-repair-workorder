import { defineConfig } from 'vite'
import vue from '@vitejs/plugin-vue'
import { fileURLToPath, URL } from 'node:url'

// 前端端口固定 5173（通道 4），/api 与 /uploads 代理到本通道后端 8083
export default defineConfig({
  plugins: [vue()],
  resolve: {
    alias: { '@': fileURLToPath(new URL('./src', import.meta.url)) }
  },
  server: {
    port: 5173,
    strictPort: true,
    proxy: {
      '/api': { target: 'http://127.0.0.1:8083', changeOrigin: true },
      '/uploads': { target: 'http://127.0.0.1:8083', changeOrigin: true }
    }
  },
  build: { chunkSizeWarningLimit: 1500 }
})
