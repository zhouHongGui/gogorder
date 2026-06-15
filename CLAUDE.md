# gogorder — 多门店点单系统

## 项目概述

面向奶茶/饮品连锁品牌的**多门店在线点单系统**。C端 uni-app（微信小程序 + H5），B端和管理后台基于若依基础框架二次开发（Vue 3 + Vite 6）。

**当前阶段**：项目已进入编码阶段，M02/M03/M05-M09 已有实现，当前持续完善 C 端交易链路并进行安全与性能审查修复。

## 技术栈

| 层 | 技术 |
|----|------|
| C端 | uni-app (Vue 3) |
| B端/管理后台 | Vue 3 + Vite 6 + Element Plus |
| 后端框架 | 若依基础框架（**仅作底层能力**，业务模块独立） |
| 后端语言 | Java 21 + Spring Boot 4.0.3 |
| 数据库 | MySQL 8.0 |
| 缓存 | Redis 5.0.14.1 |

## 项目文件结构

```
gogorder/
├── CLAUDE.md                          ← 当前文件
├── PRD-V1.0.md                        ← V1.0 主 PRD（入口概览）
├── RuoYi-Vue/                         ← 后端 Spring Boot + 若依管理端
├── gogorder-c/                        ← C端 uni-app 项目
└── docs/                              ← 详细模块 PRD（见下方）
    ├── README.md                      ← 文档索引 + 依赖总图 + 开发阶段
    ├── M00-系统架构与技术方案.md
    ├── M01-数据库设计.md               ← 17张建表SQL + Redis结构
    ├── M02-门店管理.md
    ├── M03-商品中心.md
    ├── M04-订单管理-管理后台.md
    ├── M05-C端-用户登录认证.md
    ├── M06-C端-门店选择与菜单浏览.md
    ├── M07-C端-购物车.md
    ├── M08-C端-下单与支付.md            ← 核心交易链路
    ├── M09-C端-订单中心与个人中心.md
    ├── M10-门店端-接单看板与制作核销.md
    ├── M11-数据统计看板.md
    ├── M12-微信消息推送.md
    ├── M13-全局枚举与接口契约.md         ← ⚠️ 所有文档的单一事实来源
    └── M14-余额账户管理.md
```

## 阅读顺序（接手后按此顺序）

1. **先读** `PRD-V1.0.md` — 建立全局认知（约10分钟）
2. **再读** `docs/README.md` — 理解模块依赖关系和开发阶段
3. **再读** `docs/M13-全局枚举与接口契约.md` — 全局枚举、状态机、接口契约，**其他文档与 M13 冲突时以 M13 为准**
4. **然后**按开发阶段读对应模块文档

## V1.0 核心设计决策（务必遵守）

### 支付
- **余额支付**，不是微信支付。直接扣款，无支付密码。
- V1.0 **仅后台手动充值**（M14 模块），C端无充值入口，新用户初始余额为 0。

### 订单
- 表名 `biz_order` / `biz_order_item`（`order` 是 MySQL 保留字）。
- 状态**三字段拆分**：`order_status` / `pay_status` / `refund_status`。
- **支付成功 = 自动接单**（order_status 直接从 0→1），无人工接单步骤。
- 订单类型：`NORMAL`（即时单）/ `PREORDER`（预订单）。
- 休息/暂停门店可浏览菜单但只能下预订单。

### 取消与退款
- 用户**仅可取消未支付订单**；支付后不允许用户主动取消。
- 管理员/门店可在制作前取消已接单订单。
- V1.0 **每单严格一次整单全额退款**，不支持部分退款。
- 开始制作后不允许退款。

### 库存
- **商品级库存**（shop_product.stock），-1 = 无限。不做 SKU 库存。
- 条件 UPDATE 原子扣减，每次变更有 `stock_ledger` 流水 + 幂等键。
- 幂等键格式：`{orderId}:{shopProductId}:{DEDUCT|RESTORE}`

### 取餐码
- 双字段分离：`pickup_token`（12位随机码，全局唯一，扫码核销）+ `pickup_display`（字母+3位数字，门店每日递增，叫号展示）。
- 增加 `pickup_date` 字段区分即时单取餐日和预订单预约日，防跨天碰撞。

### 规格
- 选项独立表 `spec_option`，**optionId 全局唯一且不可变**。
- 下单时传 optionId，价格由后端重算，不信任前端价格。
- 订单明细做完整快照（optionId + label + priceAdd）。

### 商品
- 支持**多分类**（product_category 关联表）。
- 月度销量改为从订单表实时查询近30天。

### 门店
- 逻辑删除（del_flag），保留历史订单引用。
- 营业时间支持跨午夜（如 18:00–02:00，open_time > close_time 表示跨日）。
- 预订单参数：preorder_min_minutes=30, preorder_max_days=7, make_lead_minutes=30。

### 员工
- `staff_shop` 多对多关联表，店员默认一家，店长可关联多家。
- B端接口必须校验门店越权（Header `X-Shop-Id`）。

### 购物车
- Redis Hash 存储，每个条目独立 Field。Lua 脚本原子操作防并发丢更新。
- 每个门店拥有独立购物车，Redis Key 使用 `cart:{userId}:{shopId}`；所有购物车接口必须携带 `shopId`。

### H5 端
- 纯手机号短信验证码登录，**不依赖公众号 openid/JSAPI**。
- H5 取餐通知 V1.0 不做。

### 小程序
- 微信新版 phone code 获取手机号，login code 与 phone code **分开传递**。
- 微信身份存入独立表 `c_user_wechat`。

## 开发阶段（严格顺序）

```
第一阶段（基础建设）：
  M13 → M00 → M01（建表）

第二阶段（管理后台数据，可并行）：
  M05 + M02（并行）→ M14 + M03（并行）

第三阶段（C端核心链路，严格串行）：
  M06 → M07 → M08（最关键！）→ M09

第四阶段（B端 + 管理收尾）：
  M10 → M04 → M11 → M12
```

详细依赖关系图见 `docs/README.md`。

## 接口规范

| 端 | 前缀 | 认证 |
|----|------|------|
| C端 | `/api/c/` | Bearer Token (JWT, 7天) |
| B端 | `/api/b/` | 若依 Token |
| 管理后台 | `/api/admin/` | 若依 Token |

- 统一响应：`{ code, msg, data }`
- 分页响应（若依格式）：`{ code, msg, rows, total }`
- 金额单位：**分**（int）

## Git 信息

- 本地 master 分支已有提交 `caa39a2`（文档对齐预订单和退款需求）
- 远程推送未完成（需 Gitee 凭据）

## 开发起点

文档已就绪，建议从以下顺序开始编码：

1. **M01** — 在 `gogorder-server/ruoyi-admin/src/main/resources/` 下创建 SQL 建表脚本
2. **M00** — 在 `gogorder-admin` 中配置路由和接口代理
3. **M02** — 实现门店管理 CRUD（后端 Service + Controller + 前端页面）
4. 按上述开发阶段依次推进

每个模块的验收标准见对应文档末尾的"验收标准"章节。
