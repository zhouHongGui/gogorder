<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { cancelRefund, completeOrder, getOrderList, notifyPickup, startMake } from '../../api/order'
import type { StaffOrderListItem, StaffOrderQuery } from '../../types/order'
import { getCurrentShop } from '../../utils/session'

type StatusFilter = { label: string; value?: number }
type DateFilter = 'TODAY' | 'SEVEN' | 'ALL'

const statusFilters: StatusFilter[] = [
  { label: '全部' },
  { label: '待制作', value: 1 },
  { label: '制作中', value: 2 },
  { label: '待取餐', value: 3 },
  { label: '已完成', value: 4 },
  { label: '已取消', value: 5 }
]
const dateFilters = [
  { label: '今日', value: 'TODAY' as DateFilter },
  { label: '近7天', value: 'SEVEN' as DateFilter },
  { label: '全部', value: 'ALL' as DateFilter }
]

const currentShop = ref(getCurrentShop())
const loading = ref(false)
const loadingMore = ref(false)
const keyword = ref('')
const phone = ref('')
const activeStatus = ref<number | undefined>(undefined)
const activeDate = ref<DateFilter>('TODAY')
const pageNum = ref(1)
const pageSize = 20
const total = ref(0)
const orders = ref<StaffOrderListItem[]>([])

const hasMore = computed(() => orders.value.length < total.value)
const totalText = computed(() => total.value ? `共 ${total.value} 单` : '暂无订单')

onShow(() => {
  currentShop.value = getCurrentShop()
  void loadOrders(true)
})

onPullDownRefresh(async () => {
  try {
    await loadOrders(true)
  } finally {
    uni.stopPullDownRefresh()
  }
})

onReachBottom(() => {
  if (hasMore.value && !loadingMore.value) void loadMore()
})

function buildQuery(): StaffOrderQuery {
  const range = dateRange(activeDate.value)
  return {
    keyword: keyword.value.trim(),
    phone: phone.value.trim(),
    orderStatus: activeStatus.value,
    startTime: range.startTime,
    endTime: range.endTime,
    pageNum: pageNum.value,
    pageSize
  }
}

async function loadOrders(reset = false) {
  if (loading.value) return
  if (reset) pageNum.value = 1
  loading.value = true
  try {
    const result = await getOrderList(buildQuery())
    total.value = result.total
    orders.value = result.rows
  } catch {
    // request 已统一提示。
  } finally {
    loading.value = false
  }
}

async function loadMore() {
  loadingMore.value = true
  pageNum.value += 1
  try {
    const result = await getOrderList(buildQuery())
    total.value = result.total
    orders.value = orders.value.concat(result.rows)
  } catch {
    pageNum.value -= 1
  } finally {
    loadingMore.value = false
  }
}

function switchStatus(value?: number) {
  activeStatus.value = value
  void loadOrders(true)
}

function switchDate(value: DateFilter) {
  activeDate.value = value
  void loadOrders(true)
}

function search() {
  void loadOrders(true)
}

function resetSearch() {
  keyword.value = ''
  phone.value = ''
  activeStatus.value = undefined
  activeDate.value = 'TODAY'
  void loadOrders(true)
}

function openDetail(order: StaffOrderListItem) {
  uni.navigateTo({ url: `/pages/order/detail?id=${order.orderId}` })
}

async function handleStart(order: StaffOrderListItem) {
  await startMake(order.orderId)
  uni.showToast({ title: '已开始制作', icon: 'success' })
  await loadOrders(true)
}

async function handleNotify(order: StaffOrderListItem) {
  await notifyPickup(order.orderId)
  uni.showToast({ title: '已通知取餐', icon: 'success' })
  await loadOrders(true)
}

async function handleComplete(order: StaffOrderListItem) {
  await completeOrder(order.orderId)
  uni.showToast({ title: '订单已完成', icon: 'success' })
  await loadOrders(true)
}

function handleCancel(order: StaffOrderListItem) {
  const reasons = ['商品售空', '设备故障', '用户沟通取消', '其他原因']
  uni.showActionSheet({
    itemList: reasons,
    success: ({ tapIndex }) => {
      const reason = reasons[tapIndex]
      uni.showModal({
        title: '取消并退款',
        content: `确认取消 ${order.pickupDisplay || order.orderNo}？原因：${reason}`,
        success: ({ confirm }) => {
          if (!confirm) return
          void cancelRefund(order.orderId, reason).then(async () => {
            uni.showToast({ title: '已取消退款', icon: 'success' })
            await loadOrders(true)
          })
        }
      })
    }
  })
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/workbench/index' })
  })
}

function dateRange(type: DateFilter) {
  if (type === 'ALL') return {}
  const now = new Date()
  const start = new Date(now)
  start.setHours(0, 0, 0, 0)
  if (type === 'SEVEN') start.setDate(start.getDate() - 6)
  const end = new Date(now)
  end.setHours(23, 59, 59, 999)
  return {
    startTime: formatDateTime(start),
    endTime: formatDateTime(end)
  }
}

function formatDateTime(date: Date) {
  const pad = (value: number) => String(value).padStart(2, '0')
  return `${date.getFullYear()}-${pad(date.getMonth() + 1)}-${pad(date.getDate())} ${pad(date.getHours())}:${pad(date.getMinutes())}:${pad(date.getSeconds())}`
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(5, 16)
}

function displayOrderStatusDesc(order: StaffOrderListItem) {
  return order.orderStatus === 1 ? '待制作' : order.orderStatusDesc
}

function statusClass(status: number) {
  return `status-${status}`
}
</script>

<template>
  <view class="safe-page order-list-page">
    <view class="hero">
      <view class="hero-top">
        <text class="back" @click="goBack">‹ 返回</text>
        <text class="eyebrow">ORDER SEARCH</text>
      </view>
      <text class="title">订单查询</text>
      <text class="subtitle">{{ currentShop?.shopName || '当前门店' }} · {{ totalText }}</text>
    </view>

    <view class="filter-card">
      <view class="search-row">
        <input v-model="keyword" class="search-input" placeholder="订单号 / 取餐号 / 备注" confirm-type="search" @confirm="search" />
        <text class="search-btn" @click="search">搜索</text>
      </view>
      <view class="search-row phone-row">
        <input v-model="phone" class="search-input" placeholder="顾客手机号" type="number" maxlength="11" confirm-type="search" @confirm="search" />
        <text class="reset-btn" @click="resetSearch">重置</text>
      </view>

      <scroll-view class="chip-scroll" scroll-x>
        <view class="chip-row">
          <text
            v-for="item in statusFilters"
            :key="item.label"
            class="filter-chip"
            :class="{ active: activeStatus === item.value }"
            @click="switchStatus(item.value)"
          >
            {{ item.label }}
          </text>
        </view>
      </scroll-view>

      <view class="date-row">
        <text
          v-for="item in dateFilters"
          :key="item.value"
          class="date-chip"
          :class="{ active: activeDate === item.value }"
          @click="switchDate(item.value)"
        >
          {{ item.label }}
        </text>
      </view>
    </view>

    <view v-if="!orders.length && !loading" class="empty-card">
      <text class="empty-title">没有找到订单</text>
      <text class="empty-desc">换个状态、日期或关键词试试</text>
    </view>

    <view v-for="order in orders" :key="order.orderId" class="order-card" @click="openDetail(order)">
      <view class="order-head">
        <view>
          <text class="pickup">{{ order.pickupDisplay || '未生成' }}</text>
          <text class="order-no">#{{ order.orderNo }}</text>
        </view>
        <text class="status-pill" :class="statusClass(order.orderStatus)">{{ displayOrderStatusDesc(order) }}</text>
      </view>

      <view class="order-meta">
        <text>{{ order.itemCount }} 杯 · {{ formatMoney(order.totalAmount) }}</text>
        <text>{{ order.userPhoneMasked || '手机号--' }}</text>
      </view>
      <view class="time-line">
        <text>下单 {{ formatTime(order.createTime) }}</text>
        <text v-if="order.scheduledPickupTime">预约 {{ formatTime(order.scheduledPickupTime) }}</text>
      </view>
      <text v-if="order.remark" class="remark">备注：{{ order.remark }}</text>

      <view class="card-actions">
        <text v-if="order.orderStatus === 1" class="action danger" @click.stop="handleCancel(order)">取消退款</text>
        <text v-if="order.orderStatus === 1" class="action primary" @click.stop="handleStart(order)">开始制作</text>
        <text v-if="order.orderStatus === 2" class="action primary" @click.stop="handleNotify(order)">通知取餐</text>
        <text v-if="order.orderStatus === 3" class="action primary" @click.stop="handleComplete(order)">完成订单</text>
        <text class="action ghost">查看详情</text>
      </view>
    </view>

    <view v-if="loading || loadingMore" class="loading-tip">加载中...</view>
    <view v-else-if="orders.length && !hasMore" class="loading-tip">已经到底了</view>
  </view>
</template>

<style scoped lang="scss">
.order-list-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 22rpx) 24rpx calc(env(safe-area-inset-bottom) + 42rpx);
  background:
    radial-gradient(circle at 88% 4%, rgba(231, 182, 117, .28), transparent 26%),
    linear-gradient(180deg, #fbf7ee 0%, #f5f0e7 48%, #efe8dd 100%);
}

.hero {
  padding: 26rpx 28rpx 30rpx;
  border-radius: 34rpx;
  background: linear-gradient(135deg, #181711 0%, #2b251d 62%, #4a3420 100%);
  box-shadow: 0 20rpx 54rpx rgba(44, 34, 22, .14);
  color: #fff;
}

.hero-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back {
  color: rgba(255, 255, 255, .78);
  font-size: 24rpx;
}

.eyebrow {
  color: #e8ba7c;
  font-size: 18rpx;
  font-weight: 850;
  letter-spacing: 4rpx;
}

.title {
  display: block;
  margin-top: 30rpx;
  font-size: 46rpx;
  font-weight: 880;
}

.subtitle {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, .68);
  font-size: 23rpx;
}

.filter-card,
.order-card,
.empty-card {
  margin-top: 22rpx;
  border: 1rpx solid rgba(255, 255, 255, .76);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .88);
  box-shadow: 0 18rpx 46rpx rgba(70, 56, 38, .09);
}

.filter-card {
  padding: 22rpx;
}

.search-row {
  display: flex;
  align-items: center;
  gap: 14rpx;
}

.phone-row {
  margin-top: 14rpx;
}

.search-input {
  height: 76rpx;
  flex: 1;
  padding: 0 22rpx;
  border-radius: 24rpx;
  background: #fffaf3;
  color: #292620;
  font-size: 25rpx;
}

.search-btn,
.reset-btn {
  width: 120rpx;
  height: 76rpx;
  border-radius: 24rpx;
  background: #181711;
  color: #fff7ea;
  font-size: 24rpx;
  font-weight: 780;
  line-height: 76rpx;
  text-align: center;
}

.reset-btn {
  background: #efe4d4;
  color: #9a6a36;
}

.chip-scroll {
  width: 100%;
  margin-top: 20rpx;
  overflow: hidden;
  white-space: nowrap;
}

.chip-row {
  display: inline-block;
  min-width: 100%;
  padding-right: 12rpx;
  box-sizing: border-box;
  font-size: 0;
  white-space: nowrap;
}

.filter-chip,
.date-chip {
  display: inline-block;
  height: 58rpx;
  margin-right: 12rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  background: #f2eadf;
  box-sizing: border-box;
  color: #8f8275;
  font-size: 23rpx;
  font-weight: 760;
  line-height: 58rpx;
  vertical-align: top;
  white-space: nowrap;
}

.chip-row .filter-chip:last-child {
  margin-right: 0;
}

.filter-chip.active,
.date-chip.active {
  background: #181711;
  color: #fff7ea;
}

.date-row {
  display: flex;
  gap: 12rpx;
  margin-top: 18rpx;
}

.date-row .date-chip {
  margin-right: 0;
}

.order-card {
  padding: 24rpx;
}

.order-head,
.order-meta,
.time-line,
.card-actions {
  display: flex;
  align-items: center;
}

.order-head {
  justify-content: space-between;
  gap: 20rpx;
}

.order-head > view {
  min-width: 0;
  flex: 1;
}

.pickup {
  display: block;
  color: #181711;
  font-size: 44rpx;
  font-weight: 880;
  line-height: 1.1;
}

.order-no {
  display: block;
  max-width: 100%;
  overflow: hidden;
  margin-top: 8rpx;
  color: #968c7e;
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status-pill {
  flex-shrink: 0;
  padding: 10rpx 16rpx;
  border-radius: 999rpx;
  background: #efe4d4;
  color: #9a6a36;
  font-size: 21rpx;
  font-weight: 800;
}

.status-1 { background: #fff1d7; color: #a0641d; }
.status-2 { background: #e8f0ff; color: #315a9b; }
.status-3 { background: #e7f5ee; color: #39745a; }
.status-4 { background: #ede9e3; color: #70665a; }
.status-5 { background: #f8e8e5; color: #bd5144; }

.order-meta,
.time-line {
  justify-content: space-between;
  margin-top: 18rpx;
  color: #776f65;
  font-size: 23rpx;
}

.time-line {
  color: #a49a8d;
  font-size: 21rpx;
}

.remark {
  display: block;
  margin-top: 18rpx;
  padding: 16rpx 18rpx;
  border-radius: 20rpx;
  background: #fbf3e9;
  color: #8b6b48;
  font-size: 22rpx;
  line-height: 1.45;
}

.card-actions {
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 22rpx;
}

.action {
  height: 58rpx;
  padding: 0 20rpx;
  border-radius: 999rpx;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.action.primary {
  background: #181711;
  color: #fff7ea;
}

.action.danger {
  background: #f7e5df;
  color: #bd5144;
}

.action.ghost {
  background: #f1e8db;
  color: #8f6b44;
}

.empty-card {
  padding: 56rpx 24rpx;
  text-align: center;
}

.empty-title,
.empty-desc {
  display: block;
}

.empty-title {
  color: #292620;
  font-size: 30rpx;
  font-weight: 850;
}

.empty-desc,
.loading-tip {
  color: #9f9588;
  font-size: 22rpx;
}

.empty-desc {
  margin-top: 12rpx;
}

.loading-tip {
  padding: 28rpx 0 10rpx;
  text-align: center;
}
</style>
