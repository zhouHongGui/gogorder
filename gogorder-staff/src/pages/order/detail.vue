<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onPullDownRefresh } from '@dcloudio/uni-app'
import { cancelRefund, completeOrder, getOrderDetail, notifyPickup, startMake } from '../../api/order'
import type { StaffOrderDetail } from '../../types/order'

const orderId = ref(0)
const loading = ref(false)
const order = ref<StaffOrderDetail | null>(null)

const canCancel = computed(() => order.value?.orderStatus === 1)
const canStart = computed(() => order.value?.orderStatus === 1)
const canNotify = computed(() => order.value?.orderStatus === 2)
const canComplete = computed(() => order.value?.orderStatus === 3)

onLoad((options) => {
  orderId.value = Number(options?.id || 0)
  void loadDetail()
})

onPullDownRefresh(async () => {
  try {
    await loadDetail()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadDetail() {
  if (!orderId.value || loading.value) return
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId.value)
  } catch {
    // request 已统一提示。
  } finally {
    loading.value = false
  }
}

async function handleStart() {
  if (!order.value) return
  await startMake(order.value.orderId)
  uni.showToast({ title: '已开始制作', icon: 'success' })
  await loadDetail()
}

async function handleNotify() {
  if (!order.value) return
  await notifyPickup(order.value.orderId)
  uni.showToast({ title: '已通知取餐', icon: 'success' })
  await loadDetail()
}

async function handleComplete() {
  if (!order.value) return
  await completeOrder(order.value.orderId)
  uni.showToast({ title: '订单已完成', icon: 'success' })
  await loadDetail()
}

function handleCancel() {
  if (!order.value) return
  const current = order.value
  const reasons = ['商品售空', '设备故障', '用户沟通取消', '其他原因']
  uni.showActionSheet({
    itemList: reasons,
    success: ({ tapIndex }) => {
      const reason = reasons[tapIndex]
      uni.showModal({
        title: '取消并退款',
        content: `确认取消 ${current.pickupDisplay || current.orderNo} 并全额退款？原因：${reason}`,
        success: ({ confirm }) => {
          if (!confirm) return
          void cancelRefund(current.orderId, reason).then(async () => {
            uni.showToast({ title: '已取消退款', icon: 'success' })
            await loadDetail()
          })
        }
      })
    }
  })
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.redirectTo({ url: '/pages/order/list' })
  })
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

function formatTime(value?: string) {
  if (!value) return '--'
  return String(value).replace('T', ' ').slice(0, 16)
}

function displayOrderStatusDesc(detail: StaffOrderDetail) {
  return detail.orderStatus === 1 ? '待制作' : detail.orderStatusDesc
}

function timeRows(detail: StaffOrderDetail) {
  return [
    { label: '下单时间', value: detail.createTime },
    { label: '支付时间', value: detail.payTime },
    { label: '接单时间', value: detail.acceptTime },
    { label: '开始制作', value: detail.makeStartTime },
    { label: '通知取餐', value: detail.completeMakeTime },
    { label: '完成时间', value: detail.verifyTime },
    { label: '取消时间', value: detail.cancelTime }
  ].filter(item => item.value)
}
</script>

<template>
  <view class="safe-page detail-page">
    <view class="hero">
      <view class="hero-top">
        <text class="back" @click="goBack">‹ 返回</text>
        <text class="eyebrow">ORDER DETAIL</text>
      </view>
      <view v-if="order" class="hero-main">
        <view>
          <text class="pickup">{{ order.pickupDisplay || '未生成' }}</text>
          <text class="order-no">#{{ order.orderNo }}</text>
        </view>
        <view class="status-box">
          <text class="status">{{ displayOrderStatusDesc(order) }}</text>
          <text class="amount">{{ formatMoney(order.totalAmount) }}</text>
        </view>
      </view>
      <text v-else class="hero-loading">{{ loading ? '加载中...' : '订单不存在' }}</text>
    </view>

    <template v-if="order">
      <view class="section-card">
        <view class="section-head">
          <text class="section-title">订单信息</text>
          <text class="section-tag">{{ order.orderTypeDesc }}</text>
        </view>
        <view class="info-grid">
          <view class="info-item">
            <text class="info-label">顾客手机号</text>
            <text class="info-value">{{ order.reservedPhone || '--' }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">支付状态</text>
            <text class="info-value">{{ order.payStatusDesc || order.payStatus }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">退款状态</text>
            <text class="info-value">{{ order.refundStatusDesc || order.refundStatus }}</text>
          </view>
          <view class="info-item">
            <text class="info-label">预约时间</text>
            <text class="info-value">{{ formatTime(order.scheduledPickupTime) }}</text>
          </view>
        </view>
        <view v-if="order.remark" class="remark">备注：{{ order.remark }}</view>
        <view v-if="order.cancelReason" class="remark danger">取消原因：{{ order.cancelReason }}</view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">商品明细</text>
          <text class="section-tag">{{ order.itemCount }} 杯</text>
        </view>
        <view v-for="item in order.items" :key="item.productId + item.specText" class="item-row">
          <view class="item-main">
            <text class="item-name">{{ item.productName }} × {{ item.quantity }}</text>
            <text class="item-spec">{{ item.specText }}</text>
          </view>
          <text class="item-price">{{ formatMoney(item.subtotal) }}</text>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">金额</text>
        </view>
        <view class="money-row">
          <text>商品金额</text>
          <text>{{ formatMoney(order.productAmount) }}</text>
        </view>
        <view class="money-row">
          <text>包装费</text>
          <text>{{ formatMoney(order.packFee) }}</text>
        </view>
        <view class="money-row total">
          <text>实付合计</text>
          <text>{{ formatMoney(order.totalAmount) }}</text>
        </view>
      </view>

      <view class="section-card">
        <view class="section-head">
          <text class="section-title">时间线</text>
        </view>
        <view v-for="item in timeRows(order)" :key="item.label" class="time-row">
          <text class="time-label">{{ item.label }}</text>
          <text class="time-value">{{ formatTime(item.value) }}</text>
        </view>
      </view>

      <view v-if="canCancel || canStart || canNotify || canComplete" class="bottom-actions">
        <text v-if="canCancel" class="bottom-action danger" @click="handleCancel">取消退款</text>
        <text v-if="canStart" class="bottom-action primary" @click="handleStart">开始制作</text>
        <text v-if="canNotify" class="bottom-action primary" @click="handleNotify">通知取餐</text>
        <text v-if="canComplete" class="bottom-action primary" @click="handleComplete">完成订单</text>
      </view>
    </template>
  </view>
</template>

<style scoped lang="scss">
.detail-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 22rpx) 24rpx calc(env(safe-area-inset-bottom) + 132rpx);
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

.hero-top,
.hero-main,
.section-head,
.item-row,
.money-row,
.time-row,
.bottom-actions {
  display: flex;
  align-items: center;
}

.hero-top,
.hero-main,
.section-head,
.money-row,
.time-row {
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

.hero-main {
  margin-top: 34rpx;
}

.pickup {
  display: block;
  font-size: 58rpx;
  font-weight: 900;
  line-height: 1;
}

.order-no,
.hero-loading {
  display: block;
  margin-top: 12rpx;
  color: rgba(255, 255, 255, .64);
  font-size: 23rpx;
}

.status-box {
  text-align: right;
}

.status {
  display: block;
  color: #f0c98d;
  font-size: 25rpx;
  font-weight: 850;
}

.amount {
  display: block;
  margin-top: 10rpx;
  font-size: 34rpx;
  font-weight: 880;
}

.section-card {
  margin-top: 22rpx;
  padding: 26rpx;
  border: 1rpx solid rgba(255, 255, 255, .76);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .9);
  box-shadow: 0 18rpx 46rpx rgba(70, 56, 38, .09);
}

.section-title {
  color: #181711;
  font-size: 29rpx;
  font-weight: 860;
}

.section-tag {
  padding: 8rpx 16rpx;
  border-radius: 999rpx;
  background: #f1e7d9;
  color: #9a6a36;
  font-size: 21rpx;
  font-weight: 800;
}

.info-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 18rpx;
  margin-top: 24rpx;
}

.info-item {
  width: calc(50% - 9rpx);
  min-width: 0;
}

.info-label,
.info-value,
.item-name,
.item-spec,
.remark {
  display: block;
}

.info-label {
  color: #a09689;
  font-size: 21rpx;
}

.info-value {
  overflow: hidden;
  margin-top: 8rpx;
  color: #2b261f;
  font-size: 24rpx;
  font-weight: 780;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.remark {
  margin-top: 22rpx;
  padding: 18rpx 20rpx;
  border-radius: 22rpx;
  background: #fbf3e9;
  color: #8b6b48;
  font-size: 23rpx;
  line-height: 1.45;
}

.remark.danger {
  background: #f8e8e5;
  color: #bd5144;
}

.item-row {
  justify-content: space-between;
  gap: 22rpx;
  padding: 22rpx 0;
  border-bottom: 1rpx solid #f0e7db;
}

.item-row:last-child {
  border-bottom: 0;
  padding-bottom: 0;
}

.item-main {
  min-width: 0;
  flex: 1;
}

.item-name {
  color: #2b261f;
  font-size: 26rpx;
  font-weight: 820;
}

.item-spec {
  margin-top: 8rpx;
  color: #9b9184;
  font-size: 21rpx;
}

.item-price {
  color: #bd651c;
  font-size: 25rpx;
  font-weight: 850;
}

.money-row,
.time-row {
  padding-top: 20rpx;
  color: #766d62;
  font-size: 24rpx;
}

.money-row.total {
  margin-top: 8rpx;
  padding-top: 24rpx;
  border-top: 1rpx solid #f0e7db;
  color: #181711;
  font-size: 30rpx;
  font-weight: 880;
}

.time-label {
  color: #9b9184;
}

.time-value {
  color: #2b261f;
  font-weight: 760;
}

.bottom-actions {
  position: fixed;
  right: 24rpx;
  bottom: calc(env(safe-area-inset-bottom) + 24rpx);
  left: 24rpx;
  gap: 16rpx;
  padding: 18rpx;
  border-radius: 32rpx;
  background: rgba(255, 255, 255, .96);
  box-shadow: 0 18rpx 48rpx rgba(56, 43, 28, .16);
}

.bottom-action {
  flex: 1;
  height: 78rpx;
  border-radius: 999rpx;
  font-size: 26rpx;
  font-weight: 850;
  line-height: 78rpx;
  text-align: center;
}

.bottom-action.primary {
  background: #181711;
  color: #fff7ea;
}

.bottom-action.danger {
  background: #f7e5df;
  color: #bd5144;
}
</style>
