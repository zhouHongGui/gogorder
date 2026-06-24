<script setup lang="ts">
import { computed, ref } from 'vue'
import { onHide, onPullDownRefresh, onShow, onUnload } from '@dcloudio/uni-app'
import { batchStartMake, cancelRefund, completeMake, getOrderBoard, startMake } from '../../api/order'
import type { StaffOrderBoard, StaffOrderCard } from '../../types/order'
import { getCurrentShop } from '../../utils/session'

type BoardTab = 'pending' | 'making' | 'waiting' | 'preorders'

const loading = ref(false)
const board = ref<StaffOrderBoard>({
  preorders: [],
  pending: [],
  making: [],
  waiting: [],
  preordersCount: 0,
  pendingCount: 0,
  makingCount: 0,
  waitingCount: 0,
  totalActiveCount: 0
})
const activeTab = ref<BoardTab>('pending')
const currentShop = ref(getCurrentShop())
let pollTimer: ReturnType<typeof setInterval> | undefined

const tabs = computed(() => [
  { key: 'pending' as BoardTab, label: '待制作', count: board.value.pendingCount },
  { key: 'making' as BoardTab, label: '制作中', count: board.value.makingCount },
  { key: 'waiting' as BoardTab, label: '待取餐', count: board.value.waitingCount },
  { key: 'preorders' as BoardTab, label: '预约单', count: board.value.preordersCount }
])

const currentOrders = computed(() => board.value[activeTab.value])
const canBatchStart = computed(() => activeTab.value === 'pending' && board.value.pending.length > 0)

onShow(() => {
  currentShop.value = getCurrentShop()
  void loadBoard(true)
  startPolling()
})

onHide(stopPolling)
onUnload(stopPolling)

onPullDownRefresh(async () => {
  try {
    await loadBoard(false)
  } finally {
    uni.stopPullDownRefresh()
  }
})

function startPolling() {
  stopPolling()
  pollTimer = setInterval(() => void loadBoard(false, true), 5000)
}

function stopPolling() {
  if (pollTimer) {
    clearInterval(pollTimer)
    pollTimer = undefined
  }
}

async function loadBoard(showLoading = false, silent = false) {
  if (loading.value) return
  loading.value = showLoading
  try {
    board.value = await getOrderBoard()
  } catch {
    if (!silent) {
      // request 已统一提示。
    }
  } finally {
    loading.value = false
  }
}

async function handleStart(order: StaffOrderCard) {
  await startMake(order.orderId)
  uni.showToast({ title: '已开始制作', icon: 'success' })
  await loadBoard()
}

function handleBatchStart() {
  const orderIds = board.value.pending.map((item) => item.orderId)
  if (!orderIds.length) return
  uni.showModal({
    title: '批量开始制作',
    content: `确认开始制作 ${orderIds.length} 笔订单？`,
    success: ({ confirm }) => {
      if (!confirm) return
      void batchStartMake(orderIds).then(async (result) => {
        uni.showToast({ title: `已处理${result.count}单`, icon: 'success' })
        await loadBoard()
      })
    }
  })
}

async function handleComplete(order: StaffOrderCard) {
  await completeMake(order.orderId)
  uni.showToast({ title: '已进入待取餐', icon: 'success' })
  await loadBoard()
}

function handleCancel(order: StaffOrderCard) {
  const reasons = ['门店售罄', '设备故障', '用户沟通取消', '其他原因']
  uni.showActionSheet({
    itemList: reasons,
    success: ({ tapIndex }) => {
      const reason = reasons[tapIndex]
      uni.showModal({
        title: '取消并退款',
        content: `确认取消订单 ${order.pickupDisplay || order.orderNo} 并全额退款？原因：${reason}`,
        success: ({ confirm }) => {
          if (!confirm) return
          void cancelRefund(order.orderId, reason).then(async () => {
            uni.showToast({ title: '已取消退款', icon: 'success' })
            await loadBoard()
          })
        }
      })
    }
  })
}

function openVerify() {
  uni.navigateTo({ url: '/pages/order/verify' })
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/workbench/index' })
  })
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(5, 16)
}

function firstItems(order: StaffOrderCard) {
  return order.items.slice(0, 3)
}
</script>

<template>
  <view class="safe-page board-page">
    <view class="hero-card">
      <view class="hero-copy">
        <view class="hero-topline">
          <text class="back-chip" @click="goBack">‹ 返回</text>
          <text class="eyebrow">制作看板</text>
        </view>
        <text class="hero-title">{{ currentShop?.shopName || '当前门店' }}</text>
      </view>
      <view class="hero-actions">
        <text class="hero-count">{{ board.totalActiveCount }}</text>
        <text class="hero-label">进行中</text>
      </view>
    </view>

    <view class="summary-grid">
      <view v-for="tab in tabs" :key="tab.key" class="summary-pill" :class="{ active: activeTab === tab.key }" @click="activeTab = tab.key">
        <text class="summary-number">{{ tab.count }}</text>
        <text class="summary-name">{{ tab.label }}</text>
      </view>
    </view>

    <view class="toolbar">
      <text class="toolbar-text">{{ loading ? '刷新中...' : '每 5 秒自动刷新' }}</text>
      <view class="toolbar-buttons">
        <text v-if="canBatchStart" class="toolbar-button" @click="handleBatchStart">批量开始</text>
        <text class="toolbar-button dark" @click="openVerify">扫码核销</text>
      </view>
    </view>

    <view v-if="!currentOrders.length" class="empty-card">
      <text class="empty-title">暂无订单</text>
      <text class="empty-desc">新订单会自动出现在这里</text>
    </view>

    <view v-for="order in currentOrders" :key="order.orderId" class="order-card" :class="{ timeout: order.makeTimeout }">
      <view class="order-head">
        <view class="order-main">
          <text class="pickup">{{ order.pickupDisplay || '未生成' }}</text>
          <text class="order-no">#{{ order.orderNo }}</text>
        </view>
        <view class="order-tags">
          <text v-if="order.orderType === 'PREORDER'" class="tag preorder">预约</text>
          <text v-if="order.makeTimeout" class="tag danger">超时</text>
          <text class="tag">{{ order.orderStatusDesc }}</text>
        </view>
      </view>

      <view class="order-meta">
        <text>下单 {{ formatTime(order.createTime) }}</text>
        <text v-if="order.scheduledPickupTime">预约 {{ formatTime(order.scheduledPickupTime) }}</text>
        <text>{{ order.itemCount }} 杯 · {{ formatMoney(order.totalAmount) }}</text>
      </view>

      <view class="items">
        <view v-for="item in firstItems(order)" :key="item.productId + item.specText" class="item-line">
          <view class="item-copy">
            <text class="item-name">{{ item.productName }} × {{ item.quantity }}</text>
            <text class="item-spec">{{ item.specText }}</text>
          </view>
        </view>
        <text v-if="order.items.length > 3" class="more-items">还有 {{ order.items.length - 3 }} 项</text>
      </view>

      <view v-if="order.remark" class="remark">备注：{{ order.remark }}</view>

      <view class="card-actions">
        <text v-if="order.orderStatus === 1" class="action-chip danger-chip" @click="handleCancel(order)">取消退款</text>
        <text v-if="order.orderStatus === 1" class="action-chip primary-chip" @click="handleStart(order)">开始制作</text>
        <text v-if="order.orderStatus === 2" class="action-chip primary-chip" @click="handleComplete(order)">制作完成</text>
        <text v-if="order.orderStatus === 3" class="action-chip primary-chip" @click="openVerify">核销取餐</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.board-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 22rpx) 24rpx calc(env(safe-area-inset-bottom) + 40rpx);
  background:
    radial-gradient(circle at 92% 4%, rgba(231, 182, 117, .24), transparent 26%),
    linear-gradient(180deg, #fbf7ee 0%, #f5f0e7 48%, #efe8dd 100%);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 22rpx;
  padding: 26rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, .56);
  border-radius: 30rpx;
  background:
    radial-gradient(circle at 90% 0%, rgba(226, 178, 111, .28), transparent 32%),
    linear-gradient(135deg, #181711 0%, #2b251d 60%, #3b2b1f 100%);
  color: #fff;
  box-shadow: 0 18rpx 44rpx rgba(69, 50, 30, .16);
}

.hero-copy {
  min-width: 0;
  flex: 1;
}

.hero-topline {
  display: flex;
  align-items: center;
  gap: 12rpx;
}

.back-chip {
  height: 46rpx;
  padding: 0 16rpx;
  border-radius: 999rpx;
  background: rgba(255, 255, 255, .12);
  color: rgba(255, 255, 255, .82);
  font-size: 21rpx;
  font-weight: 760;
  line-height: 46rpx;
}

.eyebrow {
  display: block;
  color: #d7ad75;
  font-size: 19rpx;
  font-weight: 800;
  letter-spacing: 5rpx;
}

.hero-title {
  display: block;
  max-width: 470rpx;
  margin-top: 12rpx;
  overflow: hidden;
  font-size: 34rpx;
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
  line-height: 1.25;
}

.hero-actions {
  display: flex;
  flex-direction: column;
  align-items: center;
  min-width: 112rpx;
  padding: 14rpx 18rpx;
  border: 1rpx solid rgba(255, 255, 255, .1);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .09);
}

.hero-count {
  font-size: 38rpx;
  font-weight: 820;
  line-height: 1;
}

.hero-label {
  margin-top: 6rpx;
  color: rgba(255, 255, 255, .68);
  font-size: 19rpx;
}

.summary-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 10rpx;
  margin-top: 18rpx;
  padding: 8rpx;
  border: 1rpx solid rgba(255, 255, 255, .78);
  border-radius: 26rpx;
  background: rgba(255, 255, 255, .62);
  box-shadow: 0 10rpx 28rpx rgba(67, 57, 42, .05);
}

.summary-pill {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  min-height: 92rpx;
  padding: 12rpx 6rpx;
  border-radius: 22rpx;
  color: #8f8374;
}

.summary-pill.active {
  background: #181711;
  color: #fff;
  box-shadow: 0 10rpx 24rpx rgba(24, 23, 17, .12);
}

.summary-number {
  font-size: 31rpx;
  font-weight: 820;
  line-height: 1;
}

.summary-name {
  margin-top: 7rpx;
  font-size: 20rpx;
}

.toolbar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
  margin: 20rpx 0 14rpx;
  padding: 14rpx 16rpx;
  border: 1rpx solid rgba(255, 255, 255, .72);
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .72);
}

.toolbar-text {
  color: #9a9388;
  font-size: 21rpx;
}

.toolbar-buttons {
  display: flex;
  flex: none;
  gap: 10rpx;
}

.toolbar-button {
  height: 50rpx;
  padding: 0 18rpx;
  border-radius: 999rpx;
  background: #fff;
  color: #9c6534;
  font-size: 21rpx;
  font-weight: 700;
  line-height: 50rpx;
}

.toolbar-button.dark {
  background: #2a251d;
  color: #f1dfc3;
}

.empty-card,
.order-card {
  margin-top: 14rpx;
  border: 1rpx solid rgba(220, 205, 184, .28);
  border-radius: 28rpx;
  background: rgba(255, 255, 255, .94);
  box-shadow: 0 12rpx 32rpx rgba(67, 57, 42, .055);
}

.empty-card {
  padding: 54rpx 30rpx;
}

.order-card {
  padding: 24rpx;
}

.order-card.timeout {
  border-color: #d95c4c;
  box-shadow: 0 14rpx 36rpx rgba(217, 92, 76, .14);
}

.empty-title {
  display: block;
  color: #292620;
  font-size: 29rpx;
  font-weight: 800;
  text-align: center;
}

.empty-desc {
  display: block;
  margin-top: 10rpx;
  color: #9a9388;
  font-size: 22rpx;
  text-align: center;
}

.order-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 20rpx;
}

.order-main {
  min-width: 0;
  flex: 1;
}

.pickup {
  display: block;
  color: #181711;
  font-size: 42rpx;
  font-weight: 880;
  line-height: 1.05;
}

.order-no {
  display: block;
  margin-top: 8rpx;
  color: #9a9388;
  font-size: 20rpx;
}

.order-tags {
  display: flex;
  flex-wrap: wrap;
  justify-content: flex-end;
  gap: 8rpx;
}

.tag {
  height: 32rpx;
  padding: 0 12rpx;
  border-radius: 999rpx;
  background: #f1eadf;
  color: #8f6940;
  font-size: 19rpx;
  line-height: 32rpx;
}

.tag.preorder {
  background: #e8f0ea;
  color: #39745a;
}

.tag.danger {
  background: #fde5df;
  color: #b94b3d;
}

.order-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 10rpx 16rpx;
  margin-top: 18rpx;
  padding: 14rpx 16rpx;
  border-radius: 20rpx;
  background: #fbf7ef;
  color: #7e756b;
  font-size: 21rpx;
}

.items {
  margin-top: 16rpx;
  padding: 6rpx 18rpx;
  border: 1rpx solid #f0e7da;
  border-radius: 22rpx;
  background: #fffaf3;
}

.item-line {
  display: flex;
  padding: 14rpx 0;
}

.item-line + .item-line {
  border-top: 1rpx solid #f0e7da;
}

.item-copy {
  min-width: 0;
  flex: 1;
}

.item-name {
  display: block;
  color: #292620;
  font-size: 24rpx;
  font-weight: 720;
  line-height: 1.35;
}

.item-spec,
.more-items {
  color: #9a9388;
  font-size: 20rpx;
}

.item-spec {
  display: block;
  margin-top: 6rpx;
  line-height: 1.35;
}

.more-items {
  display: block;
  padding: 6rpx 0 12rpx;
}

.remark {
  margin-top: 14rpx;
  padding: 13rpx 16rpx;
  border-radius: 18rpx;
  background: #fff5df;
  color: #91642f;
  font-size: 21rpx;
  line-height: 1.45;
}

.card-actions {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  flex-wrap: wrap;
  gap: 12rpx;
  margin-top: 18rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f0ebe3;
}

.action-chip {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  box-sizing: border-box;
  min-width: 124rpx;
  height: 58rpx;
  padding: 0 22rpx;
  border-radius: 999rpx;
  font-size: 22rpx;
  font-weight: 760;
  line-height: 58rpx;
  text-align: center;
}

.danger-chip {
  border: 1rpx solid #efccc4;
  background: #fff4f1;
  color: #a94d42;
}

.primary-chip {
  background: #181711;
  color: #f1dfc3;
  box-shadow: 0 8rpx 18rpx rgba(24, 23, 17, .12);
}
</style>
