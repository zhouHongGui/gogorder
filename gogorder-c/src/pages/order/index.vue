<template>
  <view class="safe-page order-page">
    <view class="order-header">
      <view>
        <text class="eyebrow">MY ORDERS</text>
        <text class="title">订单</text>
        <text class="description">查看今天和过往的点单记录</text>
      </view>
      <view class="refresh-button" @click="loadOrders(true)">
        <text class="refresh-icon">↻</text>
        <text>刷新</text>
      </view>
    </view>

    <view class="date-tabs">
      <view
        v-for="tab in tabs"
        :key="tab.scope"
        class="date-tab"
        :class="{ active: activeScope === tab.scope }"
        @click="selectTab(tab.scope)"
      >
        <text class="tab-title">{{ tab.label }}</text>
        <text class="tab-copy">{{ tab.description }}</text>
      </view>
    </view>

    <view class="order-content">
      <view v-if="loading" class="loading-state">
        <view class="loading-ring" />
        <text>正在加载订单...</text>
      </view>

      <view v-else-if="orders.length === 0" class="empty-card">
        <view class="empty-cup">GO</view>
        <text class="empty-title">{{ activeScope === 'TODAY' ? '今天还没有订单' : '暂无历史订单' }}</text>
        <text class="empty-description">点一杯喜欢的饮品，开启今天的好心情</text>
        <button class="order-now" @click="goHome">去点餐</button>
      </view>

      <view v-else class="order-list">
        <view v-for="order in orders" :key="order.orderId" class="order-card" @click="openDetail(order.orderId)">
          <view class="card-top">
            <view class="shop-info">
              <image class="shop-logo" src="/static/brand/store-logo.png" mode="aspectFill" />
              <view class="shop-copy">
                <text class="shop-name">{{ order.shopName || 'GOGORDER 门店' }}</text>
                <text class="order-number">订单号 {{ order.orderNo }}</text>
              </view>
            </view>
            <view class="status" :class="statusClass(order.orderStatus)">
              {{ order.orderStatusDesc }}
            </view>
          </view>

          <view v-if="order.pickupDisplay && order.payStatus === 1" class="pickup-strip">
            <text>取餐号</text>
            <text class="pickup-number">{{ order.pickupDisplay }}</text>
            <text class="pickup-hint">{{ statusHint(order.orderStatus) }}</text>
          </view>

          <view class="product-summary">
            <view class="product-images">
              <view
                v-for="(item, index) in order.items.slice(0, 3)"
                :key="`${item.productId}-${index}`"
                class="product-cover"
              >
                <image v-if="item.productImage" :src="item.productImage" mode="aspectFill" />
                <text v-else>GO</text>
              </view>
              <view v-if="order.items.length > 3" class="more-cover">+{{ order.items.length - 3 }}</view>
            </view>
            <view class="product-copy">
              <text class="product-names">{{ productNames(order) }}</text>
              <text class="product-count">共 {{ order.itemCount }} 件 · {{ order.orderTypeDesc }}</text>
            </view>
            <view class="amount-copy">
              <text class="amount">¥{{ money(order.totalAmount) }}</text>
              <text class="detail-link">详情 ›</text>
            </view>
          </view>

          <view class="card-bottom">
            <text>{{ formatTime(order.createTime) }}</text>
            <text v-if="order.scheduledPickupTime" class="preorder-time">
              预约 {{ formatTime(order.scheduledPickupTime) }}
            </text>
          </view>
        </view>

        <view class="list-footer">
          <text v-if="loadingMore">正在加载更多...</text>
          <text v-else-if="hasMore" @click.stop="loadMore">继续加载</text>
          <text v-else>没有更多订单了</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { onPullDownRefresh, onReachBottom, onShow } from '@dcloudio/uni-app'
import { getOrders } from '../../api/order'
import type { OrderListItem } from '../../types/order'
import { getToken } from '../../utils/auth'

type DateScope = 'TODAY' | 'HISTORY'

const tabs: Array<{ label: string; description: string; scope: DateScope }> = [
  { label: '当日订单', description: '今天下单', scope: 'TODAY' },
  { label: '历史订单', description: '往日记录', scope: 'HISTORY' }
]

const PAGE_SIZE = 10
const activeScope = ref<DateScope>('TODAY')
const orders = ref<OrderListItem[]>([])
const pageNum = ref(1)
const hasMore = ref(false)
const loading = ref(true)
const loadingMore = ref(false)
let requestSerial = 0

onShow(() => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  loadOrders(true)
})

onReachBottom(loadMore)

onPullDownRefresh(async () => {
  await loadOrders(true)
  uni.stopPullDownRefresh()
})

function selectTab(scope: DateScope) {
  if (activeScope.value === scope) return
  activeScope.value = scope
  loadOrders(true)
}

/**
 * 加载订单列表。reset=true 重置到第一页；否则加载下一页（翻页）。
 * 用 requestSerial 竞态保护：切 tab/下拉并发时只采用最新请求结果。
 */
async function loadOrders(reset = false) {
  if (reset) {
    pageNum.value = 1
    loading.value = true
  } else if (loading.value || loadingMore.value || !hasMore.value) {
    return
  } else {
    loadingMore.value = true
  }

  const serial = ++requestSerial
  try {
    const page = await getOrders({
      dateScope: activeScope.value,
      pageNum: pageNum.value,
      pageSize: PAGE_SIZE
    })
    if (serial !== requestSerial) return
    orders.value = reset ? page.rows : [...orders.value, ...page.rows]
    hasMore.value = page.hasMore
  } catch {
    if (serial === requestSerial && reset) orders.value = []
  } finally {
    if (serial === requestSerial) {
      loading.value = false
      loadingMore.value = false
    }
  }
}

/** 触底加载更多：有下一页且非加载中时翻页。 */
function loadMore() {
  if (!hasMore.value || loading.value || loadingMore.value) return
  pageNum.value += 1
  loadOrders()
}

function openDetail(orderId: number) {
  uni.navigateTo({ url: `/pages/order/detail?orderId=${orderId}` })
}

function productNames(order: OrderListItem): string {
  const names = order.items.slice(0, 2).map((item) => item.productName).join('、')
  return order.items.length > 2 ? `${names} 等` : names
}

function statusClass(status: number): string {
  if (status === 0) return 'pending'
  if (status === 1 || status === 2 || status === 3) return 'active-order'
  if (status === 4) return 'completed'
  return 'cancelled'
}

function statusHint(status: number): string {
  if (status === 1) return '门店已接单'
  if (status === 2) return '正在制作'
  if (status === 3) return '请及时取餐'
  return ''
}

function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

function money(value: number): string {
  return (Number(value || 0) / 100).toFixed(2)
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : ''
}
</script>

<style lang="scss" scoped>
.order-page {
  min-height: 100vh;
  padding-bottom: calc(130rpx + env(safe-area-inset-bottom));
  background: #f7f4ee;
}

.order-header {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: calc(var(--status-bar-height) + 25rpx) 28rpx 21rpx;
}

.eyebrow,
.title,
.description {
  display: block;
}

.eyebrow {
  color: #ad7b43;
  font-size: 17rpx;
  font-weight: 800;
  letter-spacing: 4rpx;
}

.title {
  margin-top: 7rpx;
  color: #1d1c18;
  font-size: 46rpx;
  font-weight: 850;
}

.description {
  margin-top: 7rpx;
  color: #928b80;
  font-size: 20rpx;
}

.refresh-button {
  display: flex;
  height: 60rpx;
  align-items: center;
  gap: 6rpx;
  padding: 0 18rpx;
  border-radius: 30rpx;
  background: #fff;
  box-shadow: 0 8rpx 24rpx rgba(44, 38, 29, 0.05);
  color: #71685d;
  font-size: 19rpx;
}

.refresh-icon {
  color: #a8753e;
  font-size: 28rpx;
}

.date-tabs {
  display: flex;
  position: sticky;
  z-index: 10;
  top: 0;
  gap: 6rpx;
  margin: 0 28rpx;
  padding: 6rpx;
  border-radius: 26rpx;
  background: #ebe5db;
  box-shadow: 0 10rpx 28rpx rgba(44, 38, 29, 0.04);
}

.date-tab {
  display: flex;
  flex: 1;
  align-items: center;
  justify-content: center;
  padding: 16rpx 14rpx;
  border-radius: 21rpx;
  flex-direction: column;
}

.date-tab.active {
  background: #fff;
  box-shadow: 0 7rpx 20rpx rgba(64, 52, 37, 0.09);
}

.tab-title {
  color: #7d7469;
  font-size: 24rpx;
  font-weight: 750;
}

.tab-copy {
  margin-top: 3rpx;
  color: #aaa196;
  font-size: 16rpx;
}

.date-tab.active .tab-title {
  color: #211f1a;
}

.date-tab.active .tab-copy {
  color: #a8753e;
}

.order-content {
  padding: 24rpx 28rpx;
}

.loading-state {
  display: flex;
  align-items: center;
  gap: 20rpx;
  padding: 180rpx 0;
  flex-direction: column;
  color: #9b9388;
  font-size: 21rpx;
}

.loading-ring {
  width: 48rpx;
  height: 48rpx;
  border: 5rpx solid #e8e0d5;
  border-top-color: #a8753e;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.empty-card {
  margin-top: 16rpx;
  padding: 88rpx 35rpx 55rpx;
  border-radius: 32rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(44, 38, 29, 0.05);
  text-align: center;
}

.empty-cup {
  width: 100rpx;
  height: 105rpx;
  margin: 0 auto;
  border-radius: 10rpx 10rpx 30rpx 30rpx;
  background: linear-gradient(145deg, #e8c99f, #c99558);
  color: #fff;
  font-size: 27rpx;
  font-weight: 900;
  line-height: 105rpx;
}

.empty-title {
  display: block;
  margin-top: 30rpx;
  color: #292620;
  font-size: 30rpx;
  font-weight: 800;
}

.empty-description {
  display: block;
  margin-top: 11rpx;
  color: #9d958a;
  font-size: 21rpx;
}

.order-now {
  width: 230rpx;
  height: 74rpx;
  margin-top: 34rpx;
  border-radius: 37rpx;
  background: #1c1b17;
  color: #fff;
  font-size: 23rpx;
  line-height: 74rpx;
}

.order-list {
  display: flex;
  gap: 20rpx;
  flex-direction: column;
}

.order-card {
  padding: 24rpx;
  border: 1rpx solid rgba(190, 174, 151, 0.14);
  border-radius: 31rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(44, 38, 29, 0.05);
}

.card-top,
.shop-info,
.product-summary,
.card-bottom {
  display: flex;
  align-items: center;
}

.card-top {
  justify-content: space-between;
  padding-bottom: 18rpx;
  border-bottom: 1rpx solid #f0ebe3;
}

.shop-logo {
  width: 62rpx;
  height: 62rpx;
  flex: none;
  border-radius: 18rpx;
  background: #f4efe7;
}

.shop-copy {
  display: flex;
  min-width: 0;
  margin-left: 14rpx;
  flex-direction: column;
}

.shop-name {
  overflow: hidden;
  max-width: 350rpx;
  color: #292620;
  font-size: 24rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.order-number {
  overflow: hidden;
  max-width: 380rpx;
  margin-top: 5rpx;
  color: #a39b90;
  font-size: 16rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex: none;
  margin-left: 14rpx;
  padding: 7rpx 13rpx;
  border-radius: 18rpx;
  font-size: 18rpx;
  font-weight: 750;
}

.status.pending { background: #f7ead8; color: #a36a29; }
.status.active-order { background: #e5f1e8; color: #398054; }
.status.completed { background: #efede8; color: #716b63; }
.status.cancelled { background: #f2eeee; color: #9b8783; }

.pickup-strip {
  display: flex;
  align-items: baseline;
  margin-top: 18rpx;
  padding: 16rpx 19rpx;
  border-radius: 19rpx;
  background: #1c1b17;
  color: #d7c7ae;
}

.pickup-strip > text:first-child {
  font-size: 18rpx;
}

.pickup-number {
  margin-left: 12rpx;
  color: #f0c68d;
  font-size: 35rpx;
  font-weight: 900;
  letter-spacing: 2rpx;
}

.pickup-hint {
  margin-left: auto;
  color: #b9ab98;
  font-size: 17rpx;
}

.product-summary {
  padding: 22rpx 0 18rpx;
}

.product-images {
  display: flex;
  flex: none;
}

.product-cover,
.more-cover {
  display: flex;
  width: 82rpx;
  height: 82rpx;
  overflow: hidden;
  align-items: center;
  justify-content: center;
  margin-right: -14rpx;
  border: 4rpx solid #fff;
  border-radius: 19rpx;
  background: #f4efe7;
  color: #a8753e;
  font-size: 18rpx;
  font-weight: 900;
}

.product-cover image {
  width: 100%;
  height: 100%;
}

.more-cover {
  background: #eee6da;
  color: #8b7962;
}

.product-copy {
  display: flex;
  min-width: 0;
  flex: 1;
  margin-left: 28rpx;
  flex-direction: column;
}

.product-names {
  overflow: hidden;
  max-width: 260rpx;
  color: #302c26;
  font-size: 22rpx;
  font-weight: 700;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.product-count {
  margin-top: 7rpx;
  color: #9e968b;
  font-size: 17rpx;
}

.amount-copy {
  display: flex;
  align-items: flex-end;
  margin-left: 12rpx;
  flex-direction: column;
}

.amount {
  color: #211f1a;
  font-size: 28rpx;
  font-weight: 900;
}

.detail-link {
  margin-top: 7rpx;
  color: #9a7044;
  font-size: 17rpx;
}

.card-bottom {
  min-height: 42rpx;
  padding-top: 15rpx;
  border-top: 1rpx solid #f0ebe3;
  color: #a39b90;
  font-size: 17rpx;
}

.preorder-time {
  margin-left: auto;
  color: #a8753e;
}

.list-footer {
  padding: 25rpx 0 12rpx;
  color: #a39b90;
  font-size: 19rpx;
  text-align: center;
}
</style>
