# gogorder-staff

门店员工独立 uni-app 工程，目标端为微信小程序和 H5。

```bash
npm install
npm run dev:h5
npm run build:mp-weixin
```

开发环境默认通过 Vite 将 `/api` 和 `/logout` 代理到 `http://localhost:8080`。

短信联调前需在后台创建 `shop_staff` 员工并通过 `staff_shop.staff_id` 绑定门店，同时设置后端环境变量
`STAFF_SMS_MOCK_ENABLED=true`。生产环境必须关闭 mock 并接入真实短信供应商。
