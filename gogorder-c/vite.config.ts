import { defineConfig, loadEnv } from 'vite'
import type { ProxyOptions } from 'vite'
import uniModule from '@dcloudio/vite-plugin-uni'

const uni = (uniModule as typeof uniModule & { default?: typeof uniModule }).default || uniModule

const stripBrowserOrigin: ProxyOptions['configure'] = (proxy) => {
  proxy.on('proxyReq', (proxyReq) => {
    // Dev proxy is server-to-server; keeping the browser Origin makes backend CORS reject LAN hosts.
    proxyReq.removeHeader('origin')
  })
}

export default defineConfig(({ mode }) => {
  const env = loadEnv(mode, '.')
  return {
    plugins: [uni()],
    server: {
      port: 5173,
      host: true,
      proxy: {
        '/api': {
          target: env.VITE_PROXY_TARGET || 'http://localhost:8080',
          changeOrigin: true,
          configure: stripBrowserOrigin
        }
      }
    }
  }
})
