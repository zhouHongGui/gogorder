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
