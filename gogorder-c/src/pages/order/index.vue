<template>
  <view class="safe-page order-page">
    <view class="order-header">
      <view>
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
.order-page { min-height: 100vh; padding-bottom: calc(130rpx + env(safe-area-inset-bottom)); background: #f5f5f5; }
.order-header { padding: calc(var(--status-bar-height) + 28rpx) 30rpx 24rpx; background: #fff; display: flex; align-items: center; justify-content: space-between; }
.title { display: block; color: #151515; font-size: 43rpx; font-weight: 900; }.description { display: block; margin-top: 7rpx; color: #aaa; font-size: 20rpx; }
.refresh-button { height: 58rpx; padding: 0 18rpx; border-radius: 29rpx; background: #f4f5f7; color: #777; display: flex; align-items: center; gap: 6rpx; font-size: 19rpx; }.refresh-icon { color: #075fea; font-size: 29rpx; }

.date-tabs { position: sticky; z-index: 10; top: 0; padding: 14rpx 24rpx 18rpx; border-bottom: 1rpx solid #eee; background: #fff; display: flex; gap: 16rpx; }
.date-tab { flex: 1; padding: 18rpx 22rpx; border: 1rpx solid #ececec; border-radius: 20rpx; background: #fafafa; display: flex; flex-direction: column; }
.date-tab.active { border-color: #075fea; background: #075fea; box-shadow: 0 10rpx 24rpx rgba(7,95,234,.18); }.tab-title { color: #333; font-size: 24rpx; font-weight: 800; }.tab-copy { margin-top: 4rpx; color: #aaa; font-size: 17rpx; }.date-tab.active .tab-title { color: #fff; }.date-tab.active .tab-copy { color: rgba(255,255,255,.66); }

.order-content { padding: 22rpx; }.loading-state { padding: 180rpx 0; color: #aaa; display: flex; flex-direction: column; align-items: center; gap: 20rpx; font-size: 21rpx; }.loading-ring { width: 48rpx; height: 48rpx; border: 5rpx solid #e4e4e4; border-top-color: #075fea; border-radius: 50%; animation: spin .8s linear infinite; }@keyframes spin { to { transform: rotate(360deg); } }
.empty-card { margin-top: 25rpx; padding: 90rpx 35rpx 55rpx; border-radius: 28rpx; background: #fff; text-align: center; }.empty-cup { width: 100rpx; height: 105rpx; margin: 0 auto; border-radius: 10rpx 10rpx 28rpx 28rpx; background: #eaf1fc; color: #075fea; font-size: 28rpx; font-weight: 900; line-height: 105rpx; }.empty-title { display: block; margin-top: 30rpx; font-size: 30rpx; font-weight: 800; }.empty-description { display: block; margin-top: 11rpx; color: #aaa; font-size: 21rpx; }.order-now { width: 230rpx; height: 74rpx; margin-top: 34rpx; border-radius: 37rpx; background: #075fea; color: #fff; font-size: 23rpx; line-height: 74rpx; }

.order-list { display: flex; flex-direction: column; gap: 18rpx; }.order-card { padding: 24rpx; border-radius: 25rpx; background: #fff; box-shadow: 0 5rpx 18rpx rgba(0,0,0,.025); }
.card-top, .shop-info, .product-summary, .card-bottom { display: flex; align-items: center; }.card-top { justify-content: space-between; padding-bottom: 20rpx; border-bottom: 1rpx solid #f0f0f0; }
.shop-logo { width: 58rpx; height: 58rpx; flex: none; border-radius: 17rpx; background: #f5f5f5; }.shop-copy { min-width: 0; margin-left: 14rpx; display: flex; flex-direction: column; }.shop-name { max-width: 350rpx; overflow: hidden; font-size: 24rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }.order-number { max-width: 380rpx; margin-top: 5rpx; overflow: hidden; color: #aaa; font-size: 16rpx; text-overflow: ellipsis; white-space: nowrap; }
.status { flex: none; margin-left: 14rpx; font-size: 21rpx; font-weight: 800; }.status.pending { color: #d78622; }.status.active-order { color: #075fea; }.status.completed { color: #777; }.status.cancelled { color: #aaa; }
.pickup-strip { margin-top: 18rpx; padding: 16rpx 20rpx; border-radius: 16rpx; background: #edf4ff; color: #075fea; display: flex; align-items: baseline; }.pickup-strip > text:first-child { font-size: 19rpx; }.pickup-number { margin-left: 12rpx; font-size: 34rpx; font-weight: 900; letter-spacing: 2rpx; }.pickup-hint { margin-left: auto; color: #6d9ce4; font-size: 17rpx; }
.product-summary { padding: 22rpx 0 18rpx; }.product-images { flex: none; display: flex; }.product-cover, .more-cover { width: 80rpx; height: 80rpx; margin-right: -15rpx; border: 4rpx solid #fff; border-radius: 17rpx; background: #f5f5f5; overflow: hidden; color: #075fea; display: flex; align-items: center; justify-content: center; font-size: 18rpx; font-weight: 900; }.product-cover image { width: 100%; height: 100%; }.more-cover { background: #eef3fa; color: #7594c1; }
.product-copy { min-width: 0; flex: 1; margin-left: 28rpx; display: flex; flex-direction: column; }.product-names { max-width: 260rpx; overflow: hidden; font-size: 22rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.product-count { margin-top: 7rpx; color: #aaa; font-size: 17rpx; }.amount-copy { margin-left: 12rpx; display: flex; flex-direction: column; align-items: flex-end; }.amount { font-size: 28rpx; font-weight: 900; }.detail-link { margin-top: 7rpx; color: #075fea; font-size: 17rpx; }
.card-bottom { min-height: 42rpx; padding-top: 15rpx; border-top: 1rpx solid #f0f0f0; color: #aaa; font-size: 17rpx; }.preorder-time { margin-left: auto; color: #bf843b; }
.list-footer { padding: 24rpx 0 12rpx; color: #aaa; font-size: 19rpx; text-align: center; }
</style>
