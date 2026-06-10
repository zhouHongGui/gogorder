# gogorder 管理端

gogorder 点单系统的 Vue 3 管理端。

## 技术栈

- Vue 3
- Vite
- Element Plus
- Pinia

## 开发

```bash
npm install
npm run dev
```

## 高德地图配置

门店新增和编辑页面使用高德地图 Web 端（JS API）进行地图选点。高德配置不保存在前端环境文件中，由后端从服务器环境变量读取并通过登录后的门店管理接口返回：

```powershell
$env:AMAP_WEB_KEY='高德 Web 端 Key'
$env:AMAP_SECURITY_CODE='高德 JS API 安全密钥'
```

高德控制台中的 Key 类型需选择“Web端(JS API)”，并为不同环境配置对应的域名白名单。环境变量修改后需要重新启动后端，前端无需重新构建。

## 构建

```bash
npm run build:prod
```
