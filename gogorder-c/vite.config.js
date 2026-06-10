import { defineConfig, loadEnv } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

const uni = uniModule.default || uniModule

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, process.cwd())
  return {
    plugins: [uni()],
    server: {
      port: 5173,
      host: true,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true
        }
      }
    }
  }
})
