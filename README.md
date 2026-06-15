# gogorder

多门店点单系统，包含管理后台、Java 服务端和产品需求文档。

## 目录

- `RuoYi-Vue/`：Java 21 + Spring Boot 4 服务端
- `RuoYi-Vue3/`：Vue 3 管理后台
- `gogorder-c/`：uni-app Vue 3 C端（H5 + 微信小程序）
- `docs/`：模块需求文档
- `PRD-V1.0.md`：V1.0 产品需求文档
- `CLAUDE-REVIEW-FEEDBACK.md`：需求审查与已确认产品决策

## 本地环境

- MySQL 8.0
- Redis
- Java 21
- Node.js 20+

启动后端前设置 MySQL、Redis 与 Token 环境变量：

```powershell
$env:DB_HOST='localhost'
$env:DB_PORT='3306'
$env:DB_NAME='gogorder'
$env:DB_USERNAME='root'
$env:DB_PASSWORD='your-password'
$env:REDIS_HOST='localhost'
$env:REDIS_PORT='6379'
$env:REDIS_DATABASE='0'
$env:REDIS_PASSWORD=''
$env:TOKEN_SECRET='replace-with-at-least-64-random-characters-before-starting-server'
$env:C_TOKEN_SECRET='replace-with-an-independent-64-byte-random-secret'
$env:C_SMS_MOCK_ENABLED='true'
$env:WECHAT_MINI_APP_ID=''
$env:WECHAT_MINI_APP_SECRET=''
$env:LOG_PATH='E:\JavaProject\gogorder\RuoYi-Vue\logs'
$env:UPLOAD_PATH='D:\gogorder\uploadPath'
$env:AMAP_WEB_KEY='your-amap-web-js-api-key'
$env:AMAP_SECURITY_CODE='your-amap-js-api-security-code'
```

MySQL 环境变量均为必填。`TOKEN_SECRET` 必须配置且至少包含 64 个 UTF-8 字节，`C_TOKEN_SECRET` 未设置时回退到 `TOKEN_SECRET`。Redis 无密码时可不设置 `REDIS_PASSWORD`，`LOG_PATH` 未设置时默认使用后端工作目录下的 `logs`，`UPLOAD_PATH` 未设置时默认使用 `D:/gogorder/uploadPath`。开发时可显式设置 `C_SMS_MOCK_ENABLED=true`，生产环境必须保持关闭。微信小程序登录需要配置 `WECHAT_MINI_APP_ID` 和 `WECHAT_MINI_APP_SECRET`。高德地图选点配置由后端从 `AMAP_WEB_KEY` 和 `AMAP_SECURITY_CODE` 读取，再通过登录后的门店管理接口返回；未配置时仍可手动填写门店地址和经纬度。

环境变量配置完成后，按需手动启动各服务：

```powershell
# Redis
D:\Redis\redis-server.exe D:\Redis\redis.windows.conf

# 后端
java -jar .\RuoYi-Vue\ruoyi-admin\target\ruoyi-admin.jar

# 管理端前端
cd .\RuoYi-Vue3
npm run dev -- --no-open
```

详细启动方式见各子目录 README。
