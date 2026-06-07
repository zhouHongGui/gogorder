# gogorder

多门店点单系统，包含管理后台、Java 服务端和产品需求文档。

## 目录

- `RuoYi-Vue/`：Java 21 + Spring Boot 4 服务端
- `RuoYi-Vue3/`：Vue 3 管理后台
- `docs/`：模块需求文档
- `PRD-V1.0.md`：V1.0 产品需求文档
- `CLAUDE-REVIEW-FEEDBACK.md`：需求审查与已确认产品决策

## 本地环境

- MySQL 8.0
- Redis
- Java 21
- Node.js 20+

启动后端前设置数据库密码与 Token 密钥：

```powershell
$env:DB_PASSWORD='your-password'
$env:TOKEN_SECRET='replace-with-a-long-random-secret'
```

详细启动方式见各子目录 README。
