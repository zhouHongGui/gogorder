<template>
  <div class="app-container admin-order-page">
    <el-card shadow="never" class="query-card">
      <el-form ref="queryRef" :model="queryParams" :inline="true" v-show="showSearch">
        <el-form-item label="订单号" prop="orderNo">
          <el-input v-model="queryParams.orderNo" placeholder="请输入订单号" clearable style="width: 220px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="门店" prop="shopId">
          <el-select v-model="queryParams.shopId" placeholder="请选择门店" clearable filterable style="width: 220px">
            <el-option v-for="shop in shopOptions" :key="shop.id" :label="shop.name" :value="shop.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="手机号" prop="phone">
          <el-input v-model="queryParams.phone" placeholder="请输入用户手机号" clearable style="width: 180px" @keyup.enter="handleQuery" />
        </el-form-item>
        <el-form-item label="订单状态" prop="orderStatus">
          <el-select v-model="queryParams.orderStatus" placeholder="请选择" clearable style="width: 150px">
            <el-option v-for="item in orderStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="支付状态" prop="payStatus">
          <el-select v-model="queryParams.payStatus" placeholder="请选择" clearable style="width: 150px">
            <el-option v-for="item in payStatusOptions" :key="item.value" :label="item.label" :value="item.value" />
          </el-select>
        </el-form-item>
        <el-form-item label="下单时间">
          <el-date-picker
            v-model="dateRange"
            type="datetimerange"
            value-format="YYYY-MM-DD HH:mm:ss"
            start-placeholder="开始时间"
            end-placeholder="结束时间"
            range-separator="-"
            style="width: 360px"
          />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" icon="Search" @click="handleQuery">搜索</el-button>
          <el-button icon="Refresh" @click="resetQuery">重置</el-button>
        </el-form-item>
      </el-form>
    </el-card>

    <el-row :gutter="10" class="mb8 action-row">
      <right-toolbar v-model:showSearch="showSearch" @queryTable="getList" />
    </el-row>

    <el-table v-loading="loading" :data="orderList" class="order-table">
      <el-table-column label="订单号" prop="orderNo" min-width="185" show-overflow-tooltip />
      <el-table-column label="门店" prop="shopName" min-width="160" show-overflow-tooltip />
      <el-table-column label="用户手机号" prop="userPhoneMasked" width="135" align="center" />
      <el-table-column label="取餐号" prop="pickupDisplay" width="95" align="center">
        <template #default="{ row }">{{ row.pickupDisplay || '-' }}</template>
      </el-table-column>
      <el-table-column label="类型" width="95" align="center">
        <template #default="{ row }">
          <el-tag :type="row.orderType === 'PREORDER' ? 'warning' : 'success'" effect="plain">{{ row.orderTypeDesc || orderTypeName(row.orderType) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="订单状态" width="105" align="center">
        <template #default="{ row }">
          <el-tag :type="orderStatusTag(row.orderStatus)">{{ row.orderStatusDesc || orderStatusName(row.orderStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="支付状态" width="110" align="center">
        <template #default="{ row }">
          <el-tag :type="payStatusTag(row.payStatus)" effect="plain">{{ row.payStatusDesc || payStatusName(row.payStatus) }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="杯数" prop="itemCount" width="80" align="center" />
      <el-table-column label="实付金额" prop="totalAmount" width="110" align="right">
        <template #default="{ row }">{{ formatMoney(row.totalAmount) }}</template>
      </el-table-column>
      <el-table-column label="下单时间" prop="createTime" width="170" align="center" />
      <el-table-column label="预计取餐" prop="estimatedReadyTime" width="170" align="center">
        <template #default="{ row }">{{ row.estimatedReadyTime || '-' }}</template>
      </el-table-column>
      <el-table-column label="操作" align="center" width="100" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" icon="View" @click="handleDetail(row)" v-hasPermi="['admin:order:query']">详情</el-button>
        </template>
      </el-table-column>
    </el-table>

    <pagination
      v-show="total > 0"
      :total="total"
      v-model:page="queryParams.pageNum"
      v-model:limit="queryParams.pageSize"
      @pagination="getList"
    />

    <el-drawer v-model="detailOpen" title="订单详情" size="860px" destroy-on-close append-to-body>
      <div v-loading="detailLoading" class="detail-panel" v-if="detail">
        <div class="detail-header">
          <div>
            <div class="detail-order-no">{{ detail.orderNo }}</div>
            <div class="detail-sub">{{ detail.shopName }} · {{ detail.userPhoneMasked || '-' }}</div>
          </div>
          <div class="detail-amount">{{ formatMoney(detail.totalAmount) }}</div>
        </div>

        <el-descriptions :column="2" border class="detail-section">
          <el-descriptions-item label="订单状态">{{ detail.orderStatusDesc || orderStatusName(detail.orderStatus) }}</el-descriptions-item>
          <el-descriptions-item label="支付状态">{{ detail.payStatusDesc || payStatusName(detail.payStatus) }}</el-descriptions-item>
          <el-descriptions-item label="退款状态">{{ detail.refundStatusDesc || refundStatusName(detail.refundStatus) }}</el-descriptions-item>
          <el-descriptions-item label="订单类型">{{ detail.orderTypeDesc || orderTypeName(detail.orderType) }}</el-descriptions-item>
          <el-descriptions-item label="取餐号">{{ detail.pickupDisplay || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取餐日">{{ detail.pickupDate || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预约取餐">{{ detail.scheduledPickupTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="预计取餐">{{ detail.estimatedReadyTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="下单时间">{{ detail.createTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="支付时间">{{ detail.payTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="开始制作">{{ detail.makeStartTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="通知取餐">{{ detail.completeMakeTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="完成时间">{{ detail.verifyTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消时间">{{ detail.cancelTime || '-' }}</el-descriptions-item>
          <el-descriptions-item label="取消原因" :span="2">{{ detail.cancelReason || '-' }}</el-descriptions-item>
          <el-descriptions-item label="用户备注" :span="2">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <div class="card-title">商品明细</div>
          </template>
          <el-table :data="detail.items || []" border>
            <el-table-column label="商品" prop="productName" min-width="180" show-overflow-tooltip />
            <el-table-column label="规格" prop="specText" min-width="160" show-overflow-tooltip />
            <el-table-column label="单价" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(row.unitPrice) }}</template>
            </el-table-column>
            <el-table-column label="数量" prop="quantity" width="80" align="center" />
            <el-table-column label="小计" width="100" align="right">
              <template #default="{ row }">{{ formatMoney(row.subtotal) }}</template>
            </el-table-column>
          </el-table>
        </el-card>

        <el-card shadow="never" class="detail-section">
          <template #header>
            <div class="card-title">金额</div>
          </template>
          <div class="amount-line"><span>商品金额</span><b>{{ formatMoney(detail.productAmount) }}</b></div>
          <div class="amount-line"><span>包装费</span><b>{{ formatMoney(detail.packFee) }}</b></div>
          <div class="amount-line total"><span>应付合计</span><b>{{ formatMoney(detail.totalAmount) }}</b></div>
        </el-card>
      </div>
    </el-drawer>
  </div>
</template>

<script setup name="AdminOrder">
import { getOrder, listOrder } from "@/api/admin/order"
import { listShop } from "@/api/admin/shop"

const { proxy } = getCurrentInstance()

const loading = ref(false)
const detailLoading = ref(false)
const showSearch = ref(true)
const detailOpen = ref(false)
const total = ref(0)
const orderList = ref([])
const shopOptions = ref([])
const detail = ref(null)
const dateRange = ref([])

const queryParams = reactive({
  pageNum: 1,
  pageSize: 10,
  orderNo: undefined,
  shopId: undefined,
  phone: undefined,
  orderStatus: undefined,
  payStatus: undefined,
  refundStatus: undefined,
  startTime: undefined,
  endTime: undefined
})

const orderStatusOptions = [
  { label: "待支付", value: 0 },
  { label: "待制作", value: 1 },
  { label: "制作中", value: 2 },
  { label: "待取餐", value: 3 },
  { label: "已完成", value: 4 },
  { label: "已取消", value: 5 }
]

const payStatusOptions = [
  { label: "待支付", value: 0 },
  { label: "支付成功", value: 1 },
  { label: "支付超时", value: 2 },
  { label: "退款处理中", value: 3 },
  { label: "已全额退款", value: 4 }
]

const refundStatusOptions = [
  { label: "无退款", value: 0 },
  { label: "退款处理中", value: 1 },
  { label: "退款成功", value: 2 },
  { label: "退款失败", value: 3 }
]

function buildQuery() {
  const params = { ...queryParams }
  if (dateRange.value && dateRange.value.length === 2) {
    params.startTime = dateRange.value[0]
    params.endTime = dateRange.value[1]
  } else {
    params.startTime = undefined
    params.endTime = undefined
  }
  return params
}

function getList() {
  loading.value = true
  listOrder(buildQuery()).then(response => {
    orderList.value = response.rows || []
    total.value = response.total || 0
  }).finally(() => {
    loading.value = false
  })
}

function loadShops() {
  listShop({ pageNum: 1, pageSize: 1000 }).then(response => {
    shopOptions.value = response.rows || []
  })
}

function handleQuery() {
  queryParams.pageNum = 1
  getList()
}

function resetQuery() {
  dateRange.value = []
  proxy.resetForm("queryRef")
  handleQuery()
}

function handleDetail(row) {
  detailOpen.value = true
  detailLoading.value = true
  detail.value = null
  getOrder(row.orderId).then(response => {
    detail.value = response.data
  }).finally(() => {
    detailLoading.value = false
  })
}

function formatMoney(value) {
  const cents = Number(value || 0)
  return "¥" + (cents / 100).toFixed(2)
}

function orderTypeName(value) {
  return value === "PREORDER" ? "预订单" : "即时单"
}

function nameByValue(options, value) {
  const item = options.find(option => option.value === value)
  return item ? item.label : "-"
}

function orderStatusName(value) {
  return nameByValue(orderStatusOptions, value)
}

function payStatusName(value) {
  return nameByValue(payStatusOptions, value)
}

function refundStatusName(value) {
  return nameByValue(refundStatusOptions, value)
}

function orderStatusTag(value) {
  const map = { 0: "info", 1: "warning", 2: "", 3: "success", 4: "success", 5: "info" }
  return map[value] || "info"
}

function payStatusTag(value) {
  const map = { 0: "info", 1: "success", 2: "warning", 3: "warning", 4: "info" }
  return map[value] || "info"
}

loadShops()
getList()
</script>

<style scoped>
.admin-order-page {
  background: #f5f7fb;
}

.query-card {
  margin-bottom: 14px;
  border: 1px solid #edf0f7;
}

.action-row {
  justify-content: flex-end;
  align-items: center;
}

.order-table {
  border-radius: 10px;
  overflow: hidden;
}

.detail-panel {
  padding: 0 4px 20px;
}

.detail-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 18px 20px;
  margin-bottom: 16px;
  border-radius: 14px;
  background: linear-gradient(135deg, #fff7ed 0%, #ffffff 55%, #f7fbff 100%);
  border: 1px solid #f2e3d3;
}

.detail-order-no {
  font-size: 18px;
  font-weight: 700;
  color: #1f2937;
}

.detail-sub {
  margin-top: 6px;
  color: #6b7280;
}

.detail-amount {
  font-size: 24px;
  font-weight: 800;
  color: #f97316;
}

.detail-section {
  margin-bottom: 16px;
}

.card-title {
  font-weight: 700;
  color: #1f2937;
}

.amount-line {
  display: flex;
  justify-content: space-between;
  line-height: 34px;
  color: #4b5563;
}

.amount-line.total {
  margin-top: 8px;
  padding-top: 10px;
  border-top: 1px solid #edf0f7;
  font-size: 16px;
  color: #111827;
}
</style>
