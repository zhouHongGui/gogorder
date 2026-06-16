/**
 * uni-app 入口：创建应用实例。
 * uni-app 约定导出 createApp，由框架在不同端（H5/小程序）调用挂载。
 */
import { createSSRApp } from 'vue'
import App from './App.vue'

/** 创建并返回应用实例（uni-app 框架约定入口）。 */
export function createApp() {
  const app = createSSRApp(App)
  return { app }
}
