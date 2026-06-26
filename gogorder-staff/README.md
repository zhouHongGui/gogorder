# gogorder-staff

门店员工独立 uni-app 工程，目标端为微信小程序和 H5。

```bash
npm install
npm run dev:h5
npm run build:mp-weixin
```

短信联调前需在后台创建 `shop_staff` 员工并通过 `staff_shop.staff_id` 绑定门店，同时设置后端环境变量 `STAFF_SMS_MOCK_ENABLED=true`。生产环境必须关闭 mock 并接入真实短信供应商。

## Local Network Config

- H5 dev: keep `VITE_H5_API_BASE_URL=` empty, then browser requests `/api` through the Vite proxy.
- Mini Program / real device: set `VITE_API_BASE_URL` to the computer LAN IP, for example `http://172.16.88.76:8080`; do not use `localhost`.
- When WiFi or computer IP changes, update `.env.development` values `VITE_API_BASE_URL` and `VITE_PROXY_TARGET`.
- Production should set the real HTTPS API address in `.env.production` or deployment env vars.
