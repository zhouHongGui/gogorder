# gogorder-c

gogorder C端 uni-app（Vue 3）项目，支持 H5 与微信小程序。

## 本地开发

```powershell
npm install
npm run dev:h5
```

H5 默认启动在 `http://localhost:5173`，并将 `/api` 代理到 `http://localhost:8080`。

```powershell
npm run dev:mp-weixin
```

微信小程序构建前，在 `src/manifest.json` 配置小程序 `appid`，后端配置 `WECHAT_MINI_APP_ID` 与 `WECHAT_MINI_APP_SECRET`。

## Local Network Config

- H5 dev: keep `VITE_H5_API_BASE_URL=` empty, then browser requests `/api` through the Vite proxy.
- Mini Program / real device: set `VITE_API_BASE_URL` to the computer LAN IP, for example `http://172.16.88.76:8080`; do not use `localhost`.
- When WiFi or computer IP changes, update `.env.development` values `VITE_API_BASE_URL` and `VITE_PROXY_TARGET`.
- Production should set the real HTTPS API address in `.env.production` or deployment env vars.
