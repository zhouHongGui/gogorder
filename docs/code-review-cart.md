# 购物车模块 Code Review 反馈

## 需修（2项）

### 1. UPDATE_SCRIPT 使用过期库存值 —— 更新数量时库存校验形同虚设

`CCartServiceImpl.java` —— UPDATE_SCRIPT Lua 第 81-83 行：

```lua
if item.stock >= 0 and quantity > item.stock then
    return 'STOCK_NOT_ENOUGH'
end
```

`item.stock` 是**加购时写入购物车条目的快照**，不会随真实库存变化而更新。用户加购时库存 100，后来真实库存降到 3，用户把数量改到 99 不会触发拦截。

**修复**：`updateItem()` 应先查当前商品最新库存，作为 ARGV 传入 Lua，和 ADD_SCRIPT 的 `maxStock` 参数保持一致。

### 2. removeItem() 没调 handleScriptResult

`CCartServiceImpl.java` 第 179-184 行 —— `redisTemplate.execute(REMOVE_SCRIPT, ...)` 的返回值没传进 `handleScriptResult()`。当前脚本只返回 `"SUCCESS"` 所以没炸，但 ADD/UPDATE 都走了统一处理，这里漏了，以后往 REMOVE_SCRIPT 加错误码会静默吞掉。

**修复**：加上一行 `handleScriptResult(result);`。

---

## 建议修（3项）

### 3. 合并时单价被新请求覆盖

ADD_SCRIPT 合并已有条目时，`newItem.unitPrice` 直接覆盖旧条目的单价。如果两次加购之间商家调了价，之前加的商品也按新价格算了。购物车不是订单，不算 bug，但和"下单时后端重算价格"的思路不一致，建议要么合并时保留旧单价，要么在文档里写明此行为。

### 4. 前端 GET 请求用 data 传参 —— 小程序兼容性风险

`gogorder-c/src/api/cart.ts` getCart：

```ts
method: 'GET',
data: { shopId }
```

部分小程序平台 GET 请求不自动把 data 序列化到 query string。建议改成：

```ts
url: `/api/c/cart/get?shopId=${shopId}`,
method: 'GET'
```

### 5. clearCart 用 redisTemplate.delete 而非 Lua

`clearCart` 和 `addItem` 并发时，可能出现 addItem 的 Lua 刚执行完、cart 就被 delete 删掉，用户看到"加入成功"但购物车是空的。概率极低，但其余三个操作都走 Lua 保证原子性了，这里也统一用 Lua 吧。

---

## 做得好的地方

- **Lua 原子操作**：ADD/UPDATE/REMOVE 三个核心操作全走 Lua，防并发丢更新，设计正确。
- **cartItemId 哈希**：TreeMap + sorted optionIds → SHA-256 前 24 位，同一商品同一规格必然命中同一 Field，合并逻辑正确。
- **规格校验**：`validateSelectionCount` 覆盖了必选、最小选择数、单选约束、最大选择数，和 M13 规范一致。
- **价格后端重算**：`buildCartItem` 遍历 optionId 查 `SpecOption.priceAdd` 重新求和，不信任前端价格。
- **Redis Hash 结构**：每个条目独立 Field，和 M13/M07 设计完全对齐，TTL 7 天正确。
- **DTO 校验**：`@NotNull @Min @Max` 注解齐全，参数校验到位。
