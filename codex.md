# gogorder Codex 交接文档

> 最后更新：2026-06-07  
> 工作区：`E:\GG\gogorder`  
> 本文用于让新的 Codex 快速接手。开始工作前先阅读本文、`docs/README.md` 和 `docs/M13-全局枚举与接口契约.md`。

## 1. 当前状态摘要

gogorder 是一个多门店点单系统，目前处于：

- 若依后端和 Vue 3 管理端已克隆并合并为一个根目录 Git 单仓库。
- 项目已改为 Java 21、Spring Boot 4.0.3、Vue 3、Vite 6。
- 页面可见品牌已改为 `gogorder`，核心 Java 启动类和配置类已重命名。
- MySQL、Redis、后端和前端在本机可以正常启动。
- PRD 与 M00-M14 需求设计已经完成并经过一致性审查。
- **点单业务模块尚未实现**：目前代码主要仍是若依基础框架，不要误认为文档中的门店、商品、余额、订单等功能已经落地。

当前验证结果：

- `http://localhost:80/` 返回 200，标题为 `gogorder`。
- `http://localhost:8080/` 返回 200。
- 后端执行 `mvn -DskipTests compile` 成功。
- 前端执行 `npm run build:prod` 成功。
- 后端 `mvn -DskipTests package` 曾因正在运行的 JAR 被 Windows 锁定而无法重命名，并非编译失败。

## 2. Git 状态

- 根仓库分支：`master`
- 远程仓库：`https://gitee.com/hong-gui-zhou/gogorder.git`
- 最新本地提交：

```text
caa39a2 docs: align preorder and refund requirements
f719b46 docs: refine ordering requirements
6277339 feat: initialize gogorder monorepo
```

- 当前 `master` 与 `origin/master` 已同步，`caa39a2` 已存在于远程。
- Gitee 推送需要用户在本机完成 HTTPS 认证，Codex 非交互环境无法读取用户名或私人令牌。

推送命令：

```powershell
cd E:\GG\gogorder
git push origin master
```

原始子仓库 Git 元数据没有删除，而是移动并忽略：

```text
gogorder-server/.git-upstream/
gogorder-admin/.git-upstream/
```

不要删除或恢复这些目录，除非用户明确要求。

## 3. 目录结构

```text
E:\GG\gogorder
├── gogorder-server/           Java 21 后端
├── gogorder-admin/            Vue 3 管理后台和门店端基础项目
├── docs/                      M00-M14 模块设计
├── PRD-V1.0.md                V1.0 产品需求
├── CLAUDE-REVIEW-FEEDBACK.md  早期需求审查记录
├── CLAUDE.md                   Claude Code 生成的接手说明，当前未纳入 Git
├── README.md                  根项目简介
└── codex.md                   本交接文档
```

当前不存在独立的 uni-app C 端项目目录。文档中规划了 `uni-app-project/`，实现 C 端前需要创建该项目。

`CLAUDE.md` 是 Claude Code 后续生成的未跟踪文件，其中“远程推送未完成”的描述已经过时。除非用户明确要求，不要覆盖、删除或自动提交该文件。

## 4. 技术栈与本机环境

### 后端

- Java：21.0.10
- Maven：3.9.15
- Spring Boot：4.0.3
- MyBatis Spring Boot：4.0.1
- MySQL：8.0，Windows 服务名 `MySQL80`
- Redis：本机目录 `D:\Redis`，默认端口 `6379`
- 后端默认端口：`8080`

### 前端

- Node.js：20.15.1
- npm：10.7.0
- Vue：3.5.26
- Vite：6.4.1
- Element Plus：2.13.1
- 前端开发端口：`80`
- `/dev-api` 代理到 `http://localhost:8080`

### 本机数据库

- 数据库名：`gogorder`
- 用户：`root`
- 本地密码和 Token 密钥不得写入仓库或本文。
- 之前已初始化若依基础数据库；开始业务开发前应重新确认现有表。M01 中的业务表还没有正式落地。

## 5. 启动与验证

### 启动依赖

确认 MySQL 和 Redis 已启动：

```powershell
Get-Service MySQL80
Get-Process redis-server -ErrorAction SilentlyContinue
```

Redis 未启动时：

```powershell
Start-Process -FilePath 'D:\Redis\redis-server.exe' -WorkingDirectory 'D:\Redis' -WindowStyle Hidden
```

### 启动后端

先设置本机环境变量。不要把真实值写入 Git：

```powershell
$env:DB_USERNAME='root'
$env:DB_PASSWORD='<local-password>'
$env:TOKEN_SECRET='<at-least-64-byte-random-secret>'
cd E:\GG\gogorder\gogorder-server
mvn -pl ruoyi-admin -am spring-boot:run
```

关键配置：

- `gogorder-server/ruoyi-admin/src/main/resources/application.yml`
- `gogorder-server/ruoyi-admin/src/main/resources/application-druid.yml`
- 数据库 URL 默认指向 `localhost:3306/gogorder`
- Redis 默认指向 `localhost:6379`

### 启动前端

```powershell
cd E:\GG\gogorder\gogorder-admin
npm install
npm run dev
```

### 构建验证

```powershell
cd E:\GG\gogorder\gogorder-server
mvn -DskipTests compile

cd E:\GG\gogorder\gogorder-admin
npm run build:prod
```

如果后端正在运行，执行 `package` 可能因为 Windows 文件锁失败；先停止后端再打包。

## 6. 已完成的框架改造

- 根仓库已初始化为单仓库，分支名为 `master`。
- 后端 Java 版本升级为 21。
- Spring Boot 升级为 4.0.3，并调整相关依赖。
- 后端产品名称改为 `gogorder`。
- Java 类已重命名：
  - `GogorderApplication`
  - `GogorderServletInitializer`
  - `GogorderConfig`
- 前端标题、登录页、导航、首页和版权等可见品牌已改为 `gogorder`。
- 数据库、Token、Druid 密码已改用环境变量，仓库中不保存真实密码。
- 构建产物、日志、依赖目录、IDE 配置和 `.git-upstream` 已加入根 `.gitignore`。

## 7. 权威需求与开发顺序

需求权威来源：

1. `docs/M13-全局枚举与接口契约.md`：跨模块单一事实来源。
2. `docs/README.md`：模块索引、依赖关系和四阶段顺序。
3. `PRD-V1.0.md`：产品范围。
4. 各模块 M00-M14：详细功能和验收标准。

如果模块文档之间冲突，以 M13 为准；发现冲突时应先修订文档，再实现代码。

### 第一阶段：基础建设

1. M13 全局契约
2. M00 技术架构
3. M01 数据库设计与建表

### 第二阶段：管理后台数据

4. M05 C端登录、M02 门店管理，可并行
5. M14 余额账户管理、M03 商品中心，可并行

### 第三阶段：C端核心链路

6. M06 门店选择与菜单
7. M07 购物车
8. M08 下单与余额支付
9. M09 订单中心与个人中心

### 第四阶段：门店端与管理收尾

10. M10 门店端制作核销
11. M04 管理后台订单
12. M11 数据统计
13. M12 消息推送

## 8. 已确认产品决策

以下决策已经由用户确认，不要自行改回：

1. 支付方式为用户余额支付，不接微信支付。
2. V1.0 仅后台手动充值，C端没有充值入口。
3. 库存采用商品级库存，位置为 `shop_product`。
4. `stock=-1` 表示无限库存；无限库存扣减和恢复时保持 `-1`。
5. 不做 SKU 规格库存和原料库存。
6. H5 必须支持普通手机浏览器，使用手机号短信验证码登录。
7. 微信小程序 login code 与手机号 phone code 分开处理。
8. 员工默认属于一家门店；店长和授权岗位可关联多家门店。
9. 商品可以同时属于多个分类。
10. 支付成功后自动接单，用户支付后不允许主动取消。
11. 用户仅可取消自己的未支付订单。
12. 管理员或门店仅可在开始制作前取消已支付订单。
13. 每张订单严格只允许一次整单全额退款，不支持部分退款。
14. 所有未删除门店都允许浏览菜单。
15. 营业中门店允许即时单和预订单。
16. 休息或暂停接单门店只能下预订单。
17. 预订单时间参数为门店级配置，默认：
    - 最早提前 30 分钟
    - 最长预约 7 天
    - 预约取餐前 30 分钟进入制作窗口

## 9. 关键技术契约

### 订单状态

- `order_status`：履约状态
- `pay_status`：支付状态
- `refund_status`：退款状态
- 支付成功后 `order_status=ACCEPTED`，不存在人工接单步骤。

### 即时单与预订单

- `order_type=NORMAL|PREORDER`
- 预订单必须有 `scheduled_pickup_time`
- 预约时间必须落在门店营业时段内，支持跨午夜
- 预订单制作窗口由门店 `make_lead_minutes` 决定

### 交易安全

- 下单使用 `UNIQUE(user_id, submit_key)` 防止重复订单和重复扣库存。
- 支付、取消、退款、超时任务必须锁定订单行或使用带旧状态条件的原子更新。
- 余额支付锁定 `c_user_balance` 并在同一事务写余额流水、支付流水和订单状态。
- 已支付订单取消必须在同一事务完成全额退款、状态更新和库存恢复。

### 库存

- 有限库存使用条件 UPDATE 防超卖。
- 无限库存 `-1` 永远保持 `-1`。
- 订单库存流水幂等键：

```text
{orderId}:{shopProductId}:DEDUCT
{orderId}:{shopProductId}:RESTORE
```

- 后台库存调整幂等键：`adjust:{requestId}`

### 取餐码

- `pickup_token`：12位安全随机字母数字，扫码核销用。
- `pickup_display`：门店取餐日内字母+3位数字展示号，按 `A001...A999、B001...Z999` 递增。
- 唯一约束：`UNIQUE(shop_id, pickup_date, pickup_display)`。
- 即时单取餐日为支付日期，预订单取餐日为预约日期。

### 规格

- `optionId` 全局唯一且不可变。
- 后端必须校验规格属于商品、选项启用、必选项、单选和多选数量范围。
- 价格由后端重新计算，不能信任客户端价格。
- 订单明细保存完整规格与价格快照。

### 多门店权限

- `staff_shop` 保存员工门店关联、岗位和默认门店。
- 每个员工最多一个默认门店。
- B端每个接口必须同时校验门店关联和岗位权限。
- 所有门店端接口使用 `X-Shop-Id` 当前门店上下文。

### 分页

保持若依原生顶层分页格式：

```json
{
  "code": 200,
  "msg": "查询成功",
  "rows": [],
  "total": 100
}
```

## 10. 当前未实现事项

以下均只有设计文档，没有业务代码：

- M01 的 17 张业务表及后续新增字段建表/迁移脚本
- C端 uni-app 项目
- C端 JWT 登录、短信登录和微信手机号绑定
- 门店管理、员工门店关联
- 商品、分类、规格、门店商品和库存调整
- 余额账户管理和后台充值
- Redis 购物车
- 下单、余额支付、超时取消、整单退款
- 预订单时间槽和制作窗口
- 门店端制作、叫号和核销
- 订单管理、数据统计和消息推送
- 对应菜单、权限、测试和初始化数据

建议下一位 Codex 从第一阶段开始：先将 M01 转成可执行 SQL 迁移脚本，并实现最小的业务模块骨架和测试。

## 11. 当前仍需注意的范围边界

V1.0 暂不包含：

- 微信支付
- 外卖配送
- 优惠券、满减、积分
- 小票打印
- SKU 规格库存
- 原料库存
- 部分退款
- C端充值和提现
- 节假日营业日历
- 预约时段容量限制
- H5 取餐通知

若需要增加这些能力，先与用户确认并更新 M13 和相关模块文档。

## 12. 工作约束

- 不要提交数据库密码、Token 密钥、Gitee 私人令牌或其他真实凭据。
- 不要提交 `target/`、`node_modules/`、`dist/`、日志、IDE 配置或 `.git-upstream/`。
- 不要恢复若依品牌文案，用户要求项目名为 `gogorder`。
- 不要直接覆盖用户或 Claude Code 生成的文件；修改前先检查 Git 状态。
- 业务实现应遵循现有若依项目结构和 API 风格。
- 涉及余额、退款、库存和订单状态时，必须先设计事务边界、幂等键和并发测试。
- 完成阶段性工作后执行后端编译、前端构建，并检查 `git diff --check`。

## 13. 推荐接手检查清单

```powershell
cd E:\GG\gogorder
git status --short --branch
git log --oneline -5
git remote -v

Get-Service MySQL80
Get-Process redis-server,java,node -ErrorAction SilentlyContinue

cd E:\GG\gogorder\gogorder-server
mvn -DskipTests compile

cd E:\GG\gogorder\gogorder-admin
npm run build:prod
```

然后按顺序阅读：

```text
codex.md
docs/README.md
docs/M13-全局枚举与接口契约.md
docs/M00-系统架构与技术方案.md
docs/M01-数据库设计.md
```
