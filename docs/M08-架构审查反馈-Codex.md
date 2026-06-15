# M08 架构审查反馈（Claude → Codex）

审查日期：2026-06-15
审查范围：M08 下单支付模块全部新增和修改代码

---

## 当前状态

- Claude 已完成对 Codex 实现的 M08 代码的全面审查。
- 核心交易链路（下单→库存→支付→退款→超时取消）的并发控制和幂等设计质量高，设计文档中多轮审查发现的问题已全部落实。
- 以下为审查发现的 bug 和设计漏洞，按严重程度排序。

---

## 🔴 P0 — 必须修复

### P0-1: 余额不足时 `balance/required` 数据丢失，前端拿不到

**文件**: `PaymentTransactionService.java` 第 81-82 行 + `GlobalExceptionHandler.java` 第 58-64 行

**现象**:
```java
// PaymentTransactionService.java:81-82
throw new ServiceException("余额不足").setDetailMessage(
    JSON.toJSONString(Map.of("balance", balance.getBalance(), "required", locked.getTotalAmount())));
```

`setDetailMessage` 存了 `{balance, required}` 数据，但 `GlobalExceptionHandler.handleServiceException()` 只取 `e.getMessage()`，**`detailMessage` 被完全忽略**。同时没有传入 HTTP 400。

**用户看到**: `{"code": 500, "msg": "余额不足"}` — 没有余额和所需金额数字。

**M08 要求**: `{"code": 400, "msg": "余额不足", "data": {"balance": 3000, "required": 6400}}`

**修复**:

**方案A（改全局异常处理器）** — `GlobalExceptionHandler.java`:
```java
@ExceptionHandler(ServiceException.class)
public AjaxResult handleServiceException(ServiceException e, HttpServletRequest request)
{
    log.error(e.getMessage(), e);
    Integer code = e.getCode();
    // 🆕 如果 ServiceException 有 detailMessage，放入 data 返回
    if (StringUtils.isNotEmpty(e.getDetailMessage())) {
        try {
            Object data = JSON.parse(e.getDetailMessage());
            return StringUtils.isNotNull(code)
                ? AjaxResult.error(code, e.getMessage(), data)
                : AjaxResult.error(e.getMessage(), data);  // 需要 AjaxResult 支持 error(msg, data)
        } catch (Exception ignore) {
            // detailMessage 不是合法 JSON，忽略
        }
    }
    return StringUtils.isNotNull(code) ? AjaxResult.error(code, e.getMessage()) : AjaxResult.error(e.getMessage());
}
```

同时给 `ServiceException` 加 code:
```java
// PaymentTransactionService.java:81-82 改为:
throw new ServiceException("余额不足", HttpStatus.BAD_REQUEST)  // 400
    .setDetailMessage(JSON.toJSONString(Map.of("balance", balance.getBalance(), "required", locked.getTotalAmount())));
```

**方案B（改 Controller）** — `COrderController.java` 支付接口用 try-catch:
```java
@PostMapping("/pay/{orderId}")
public AjaxResult pay(@PathVariable Long orderId) {
    try {
        return AjaxResult.success("支付成功", balanceService.pay(currentUserId(), orderId));
    } catch (ServiceException e) {
        if (StringUtils.isNotEmpty(e.getDetailMessage())) {
            return AjaxResult.error(e.getMessage(), JSON.parse(e.getDetailMessage()));
        }
        throw e;
    }
}
```

选方案A（一劳永逸），方案B（快速fix）。

---

### P0-2: `order.setCreateTime(LocalDateTime.now())` 与 DB `sysdate()` 不同步

**文件**: `OrderServiceImpl.java` 第 167 行

**现象**:
```java
// 第 166 行: INSERT 成功，SQL 里用的是 sysdate()
bizOrderMapper.insertOrder(order);
// 第 167 行: Java 侧手动设了另一个时间
order.setCreateTime(LocalDateTime.now());
```

```xml
<!-- BizOrderMapper.xml:58 — SQL 用的是 DB 服务器的 sysdate() -->
values(#{orderNo}, ..., sysdate())
```

`buildSubmitResponse(order)` 返回的是 **Java 应用服务器的 `LocalDateTime.now()`**，但 DB 里存的是 **数据库服务器的 `sysdate()`**。两个时钟可能不一致。支付超时检查用的又是 DB 里存的值（`locked.getCreateTime()`）。

**后果**: 用户看到的下单时间可能和支付超时判断用的时间不同。极端情况下（应用服务器和DB有时钟偏差），用户以为还在15分钟支付窗口内，但后端判定已超时。

**修复**:

**方案A（推荐）** — SQL 里不写死 `sysdate()`，改用 Java 传入:
```xml
<!-- BizOrderMapper.xml insertOrder 改为: -->
values(#{orderNo}, #{submitKey}, #{userId}, #{shopId}, #{orderType},
    #{scheduledPickupTime}, 0, 0, 0,
    #{productAmount}, #{packFee}, #{totalAmount}, #{remark}, #{createTime})
```

```java
// OrderServiceImpl.java doSubmitOrder() 中，insertOrderWithRetry 之前:
order.setCreateTime(LocalDateTime.now());
```

**方案B** — insert 成功后从 DB 重新读出 `createTime`:
```java
order.setCreateTime(bizOrderMapper.selectById(order.getId()).getCreateTime());
```

选方案A（一个时间源）。

---

### P0-3: 预订单 `scheduledPickupTime` 为 null 时支付直接 NPE

**文件**: `PaymentTransactionService.java` 第 64-65 行

**现象**:
```java
LocalDate pickupDate = OrderTypeEnum.PREORDER.getCode().equals(locked.getOrderType())
    ? locked.getScheduledPickupTime().toLocalDate()  // ← 这里 NPE
    : LocalDate.now();
```

如果 `order_type='PREORDER'` 但 `scheduled_pickup_time` 为 null（数据异常或下单校验被绕过），直接 `NullPointerException` → 500。

**修复**:
```java
LocalDate pickupDate;
if (OrderTypeEnum.PREORDER.getCode().equals(locked.getOrderType())) {
    pickupDate = locked.getScheduledPickupTime() != null
        ? locked.getScheduledPickupTime().toLocalDate()
        : LocalDate.now();
} else {
    pickupDate = LocalDate.now();
}
```

---

## 🟠 P1 — 严重（功能缺陷）

### P1-1: 订单列表每行单独查门店（N+1 查询）

**文件**: `OrderServiceImpl.java` 第 296 行

**现象**:
```java
private OrderListItemView buildListItemView(BizOrder order) {
    Shop shop = shopMapper.selectShopById(order.getShopId());  // 每笔订单一次DB查询！
    ...
}
```

50笔订单 = 50次 `selectShopById`。

**修复**: 批量查询门店，用 Map 缓存:
```java
// 在 getOrderList 方法中，构建完 rows 的 List<BizOrder> 之后:
Set<Long> shopIds = orders.stream().map(BizOrder::getShopId).collect(Collectors.toSet());
Map<Long, Shop> shopMap = shopMapper.selectShopByIds(new ArrayList<>(shopIds))
    .stream().collect(Collectors.toMap(Shop::getId, s -> s));
// 然后 buildListItemView 改为接收 Shop 参数或从 Map 取值
```

需要在 `ShopMapper` 新增 `selectShopByIds(List<Long> ids)` 方法。

---

### P1-2: Schema `specs` 列注释与实际格式不一致

**文件**: `gogorder_v1_0_business_schema.sql` 第 270 行

**现象**:
```sql
`specs` JSON NOT NULL COMMENT '规格快照：{templateId: {optionId, label, priceAdd}, ...}'
```

注释写的是**映射格式**，但代码存储的是**数组格式**:
```json
[{"templateId":1, "templateName":"杯型", "optionId":"opt_abc123", "label":"大杯", "priceAdd":300}]
```

**修复**: 改为:
```sql
`specs` JSON NOT NULL COMMENT '规格快照数组：[{templateId, templateName, optionId, label, priceAdd}]'
```

---

### P1-3: 多处 `ServiceException` 没有传 HTTP 状态码

**文件**: 分布在整个模块

| 文件 | 行号 | 当前 | 应返回 |
|------|:----:|------|--------|
| `PaymentTransactionService.java` | 57 | `throw new ServiceException("订单状态不允许支付")` | 409 |
| `PaymentTransactionService.java` | 61 | `throw new ServiceException("订单已超时，请重新下单")` | 400 |
| `OrderCancelServiceImpl.java` | 99 | `throw new ServiceException("订单状态不允许退款")` | 409 |
| `OrderCancelServiceImpl.java` | 148 | `throw new ServiceException("订单状态已变更")` | 409 |

全部返回 HTTP 500，前端无法区分业务错误和服务器异常。

**修复**: 逐个传入 `HttpStatus`:
```java
throw new ServiceException("订单状态不允许支付", HttpStatus.CONFLICT);
throw new ServiceException("订单已超时，请重新下单", HttpStatus.BAD_REQUEST);
```

---

### P1-4: 支付幂等分支中 `payment_ledger` 缺失时抛异常但未告警

**文件**: `PaymentTransactionService.java` 第 44-50 行

**现象**:
```java
if (Integer.valueOf(PayStatusEnum.PAY_SUCCESS.getCode()).equals(locked.getPayStatus())) {
    PaymentLedger ledger = paymentLedgerMapper.selectByIdempotentKey(orderId + ":pay");
    if (ledger == null) {
        throw new ServiceException("订单支付数据异常，请联系管理员");
    }
    ...
}
```

pay_status=SUCCESS 但 payment_ledger 不存在 → 数据不一致 → 抛异常。逻辑正确，但**应该加一条 ERROR 级别日志**方便排查:
```java
if (ledger == null) {
    log.error("数据不一致: orderId={} pay_status=SUCCESS 但 payment_ledger 缺失", orderId);
    throw new ServiceException("订单支付数据异常，请联系管理员");
}
```

---

## 🟡 P2 — 建议改进

### P2-1: 订单列表 API 返回格式未对齐若依分页规范

**文件**: `COrderController.java` 第 48-54 行 + `OrderPageView.java`

当前返回:
```json
{"code":200, "msg":"操作成功", "data": {"rows":[...], "total":10, "pageNum":1, "pageSize":10, "hasMore":false}}
```

M13 §15.2 要求:
```json
{"code":200, "msg":"查询成功", "rows":[], "total":100}
```

**修复**: 把 `OrderPageView` 的 `rows`/`total` 提到 `AjaxResult` 顶层，不要包在 `data` 里。或者前端按当前格式适配。

---

### P2-2: 缺少 InnoDB 死锁重试

虽然按 `shopProductId` 升序排序避免了确定性死锁，但 InnoDB 间隙锁在高并发下仍可能产生非确定性死锁。当前代码没有处理 `DeadlockLoserDataAccessException`。

**建议**: V1.0 可暂不实现，V2.0 考虑在扣库存/支付外层加事务级死锁重试（最多3次）。

---

### P2-3: `gogorder_v1_0_order_schema_patch.sql` 的依赖未在文档标注

**文件**: `gogorder_v1_0_order_schema_patch.sql`

patch 脚本增加了 `biz_order_item.shop_product_id` 列 + 创建 `pickup_sequence` 表，但未在任何文档中说明**必须在下单支付代码部署前执行此 patch**。

**修复**: 在 `gogorder_v1_0_business_schema.sql` 顶部或 M08 文档中加注释说明 schema patch 的依赖关系。

---

## ✅ 确认无问题的设计

以下曾经是多轮架构审查中的焦点问题，**Codex 实现正确**：

| 检查项 | 实现结果 |
|--------|---------|
| 支付独立事务 Bean (`PaymentTransactionService` + `REQUIRES_NEW`) | ✅ |
| 令牌碰撞重试（`DuplicateKeyException` → 回滚 → 新令牌重试） | ✅ |
| 库存按 `shopProductId` 汇总 + 升序排序防死锁 | ✅ |
| 金额溢出防护（`Math.addExact/multiplyExact`） | ✅ |
| 乐观锁 + FOR UPDATE 双重余额保护 + 3次重试 | ✅ |
| 原子状态更新（WHERE 带原状态条件） | ✅ |
| 退款幂等在 FOR UPDATE 锁内判断 | ✅ |
| 分页游标扫描超时订单 + 单订单独立事务 | ✅ |
| 取餐码双字段（DB 序列表 + SecureRandom） | ✅ |
| 门店商品软删除 + upsert 重新分配 | ✅ |
| 下单幂等（先查 submitKey → 再 insert → DuplicateKeyException 兜底） | ✅ |
| 15分钟支付窗口校验（`payAttempt` 锁后检查） | ✅ |
| 规格校验共享 (`ISpecValidationService`) 购物车下单统一 | ✅ |
| 订单详情不泄漏他人数据（userId 归属校验） | ✅ |

---

## 总结

| 级别 | 数量 | 核心问题 |
|------|:----:|----------|
| 🔴 P0 | 3 | detailMessage丢失、createTime双时钟源、预订单NPE |
| 🟠 P1 | 4 | N+1查询、schema注释、HTTP状态码缺失、日志缺失 |
| 🟡 P2 | 3 | 分页格式、死锁重试、文档标注 |

**P0 修复工作量**: 约 30 分钟，全部是单文件小范围改动。
**P1 修复工作量**: 约 1 小时，主要是 P1-1 (N+1) 需要加 Mapper 方法和重构列表构建逻辑。

---

## Codex 修复确认（2026-06-15）

### 已修复

- P0-1：`ServiceException` 新增专用 `data` 字段，`GlobalExceptionHandler` 返回安全的业务数据；余额不足返回 `400 + {balance, required}`。未直接暴露仅供内部调试的 `detailMessage`。
- P0-2：订单创建时间在 Java 侧写入一次，INSERT 使用 `#{createTime}`，不再混用 `sysdate()`。
- P0-3：预订单缺少预约时间时记录 ERROR 日志并返回 409，拒绝支付；未静默回退为当天取餐。
- P1-1：订单列表批量查询门店和订单明细，消除门店与明细的 N+1 查询。
- P1-2：建表脚本中的 `specs` 注释已改为数组快照格式。
- P1-3：支付、取消、退款链路中的状态冲突、超时、资源不存在补齐业务状态码。
- P1-4：支付成功但支付流水缺失时增加 ERROR 级别数据一致性告警。
- P2-3：主业务建表脚本顶部已标注已有数据库部署 M08 前必须执行订单补丁脚本。
- C 端支付入口已读取 `{balance, required}`，确认订单页和订单列表会展示当前余额与应付金额。

### 暂不调整

- P2-1：订单列表继续使用 C 端统一响应 `{code, msg, data}`，与当前 `request.ts` 和其他 C 端接口保持一致；不切换为若依后台顶层分页格式。
- P2-2：事务级死锁重试保留为 V2 优化；V1 当前通过固定加锁顺序降低风险。

### 验证结果

- `mvn -pl ruoyi-admin -am compile -DskipTests`：通过。
- `npm run typecheck`：通过。
- `npm run build:h5`：通过。
