<template>
  <view class="safe-page result-page">
    <view v-if="loading" class="loading">正在获取订单结果...</view>
    <view v-else-if="order" class="result-card">
      <view class="status-mark" :class="{ failed: !success }">{{ success ? '✓' : '!' }}</view>
      <text class="result-title">{{ success ? '支付成功' : '订单已创建，支付未完成' }}</text>
      <text class="result-copy">{{ success ? '门店已自动接单，请留意取餐号' : errorMessage }}</text>

      <view v-if="success && order.pickupDisplay" class="pickup-card">
        <text class="pickup-label">取餐号</text>
        <text class="pickup-number">{{ order.pickupDisplay }}</text>
        <text class="pickup-shop">{{ order.shopName }}</text>
        <view v-if="showQueueInfo" class="queue-panel">
          <text class="queue-title">前方制作</text>
          <view class="queue-metrics">
            <view class="queue-metric">
              <text class="queue-value">{{ queueAheadOrders }}</text>
              <text class="queue-unit">单</text>
            </view>
            <view class="queue-divider" />
            <view class="queue-metric">
              <text class="queue-value">{{ queueAheadCups }}</text>
              <text class="queue-unit">杯</text>
            </view>
          </view>
        </view>
      </view>

      <view class="detail-list">
        <view class="detail-row"><text>订单编号</text><text>{{ order.orderNo }}</text></view>
        <view class="detail-row"><text>订单金额</text><text>¥{{ money(order.totalAmount) }}</text></view>
        <view v-if="order.scheduledPickupTime" class="detail-row"><text>预约取餐</text><text>{{ formatTime(order.scheduledPickupTime) }}</text></view>
        <view v-if="success && balanceAfter !== null" class="detail-row"><text>支付后余额</text><text>¥{{ money(balanceAfter) }}</text></view>
      </view>

      <button v-if="!success && order.payStatus === 0" class="primary-button retry-button" :disabled="paying" @click="retryPay">
        {{ paying ? '支付中...' : '重新支付' }}
      </button>
      <button class="home-button" @click="goHome">返回首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { clearCart } from '../../api/cart'
import { getOrderDetail, payOrder } from '../../api/order'
import type { OrderDetail } from '../../types/order'

const loading = ref(true)
const paying = ref(false)
const success = ref(false)
const errorMessage = ref('支付失败，请重试')
const balanceAfter = ref<number | null>(null)
const order = ref<OrderDetail | null>(null)
const showQueueInfo = computed(() => {
  const current = order.value
  return Boolean(success.value && current && current.payStatus === 1 && (current.orderStatus === 1 || current.orderStatus === 2))
})
const queueAheadOrders = computed(() => Math.max(0, Number(order.value?.queueAheadOrders || 0)))
const queueAheadCups = computed(() => Math.max(0, Number(order.value?.queueAheadCups || 0)))

/**
 * 页面加载：从路由参数读 orderId/success/error/balanceAfter。
 * 以订单实际支付态(payStatus=1)为准修正 success；无 orderId 直接结束加载。
 */
onLoad(async (options?: Record<string, unknown>) => {
  const orderId = Number(options?.orderId)
  success.value = options?.success === 'true'
  errorMessage.value = typeof options?.error === 'string' ? decodeURIComponent(options.error) : errorMessage.value
  const balance = Number(options?.balanceAfter)
  balanceAfter.value = Number.isFinite(balance) ? balance : null
  if (!orderId) {
    loading.value = false
    return
  }
  try {
    order.value = await getOrderDetail(orderId)
    // 以服务端实际支付态为准（覆盖路由参数的 success，避免误判）。
    success.value = order.value.payStatus === 1
  } finally {
    loading.value = false
  }
})

/** 重新支付（支付失败/超时后再次尝试）。成功则刷新订单详情与余额，切到成功态。 */
async function retryPay() {
  if (!order.value || paying.value) return
  paying.value = true
  try {
    const paid = await payOrder(order.value.orderId)
    await clearCart(order.value.shopId)
    balanceAfter.value = paid.balanceAfter
    order.value = await getOrderDetail(order.value.orderId)
    success.value = true
  } catch (error) {
    errorMessage.value = error instanceof Error ? error.message : '支付失败，请重试'
    uni.showToast({ title: errorMessage.value, icon: 'none' })
  } finally {
    paying.value = false
  }
}

/** 返回首页（tabBar 页用 switchTab）。 */
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}

/** 分转元展示。 */
function money(value: number): string {
  return (Number(value || 0) / 100).toFixed(2)
}

/** 格式化时间（去掉 T 与秒，显示到分钟）。 */
function formatTime(value: string): string {
  return value.replace('T', ' ').slice(0, 16)
}
</script>

<style lang="scss" scoped>
.result-page { padding: 58rpx 28rpx; }
.loading { padding-top: 180rpx; color: #918a7e; text-align: center; font-size: 25rpx; }
.result-card { padding: 52rpx 30rpx 34rpx; border-radius: 38rpx; background: #fff; box-shadow: 0 24rpx 70rpx rgba(62,50,34,.08); text-align: center; }
.status-mark { width: 106rpx; height: 106rpx; margin: 0 auto; border-radius: 50%; background: #39745a; color: #fff; font-size: 58rpx; font-weight: 800; line-height: 106rpx; }
.status-mark.failed { background: #c99554; }
.result-title { display: block; margin-top: 26rpx; font-size: 36rpx; font-weight: 800; }
.result-copy { display: block; margin-top: 12rpx; color: #91897d; font-size: 23rpx; line-height: 1.6; }
.pickup-card { margin-top: 38rpx; padding: 32rpx; border-radius: 28rpx; background: linear-gradient(135deg, #181711, #3b342a); color: #fff; }
.pickup-label { display: block; color: #d8b07a; font-size: 20rpx; letter-spacing: 5rpx; }
.pickup-number { display: block; margin-top: 8rpx; font-size: 80rpx; font-weight: 900; letter-spacing: 8rpx; }
.pickup-shop { display: block; margin-top: 8rpx; color: rgba(255,255,255,.65); font-size: 22rpx; }
.queue-panel { margin-top: 28rpx; padding: 22rpx 24rpx; border-radius: 24rpx; background: rgba(255,255,255,.1); }
.queue-title { display: block; color: rgba(255,255,255,.64); font-size: 20rpx; letter-spacing: 3rpx; }
.queue-metrics { display: flex; align-items: center; justify-content: center; margin-top: 12rpx; }
.queue-metric { display: flex; align-items: baseline; gap: 6rpx; }
.queue-value { color: #f3c987; font-size: 46rpx; font-weight: 900; }
.queue-unit { color: rgba(255,255,255,.72); font-size: 22rpx; }
.queue-divider { width: 1rpx; height: 38rpx; margin: 0 34rpx; background: rgba(255,255,255,.18); }
.detail-list { margin-top: 30rpx; padding: 10rpx 0; border-top: 1rpx solid #eee9df; border-bottom: 1rpx solid #eee9df; }
.detail-row { display: flex; justify-content: space-between; gap: 30rpx; padding: 16rpx 0; color: #847d72; font-size: 23rpx; text-align: right; }
.detail-row text:last-child { color: #27231e; }
.retry-button { margin-top: 34rpx; }
.home-button { margin-top: 18rpx; background: transparent; color: #6f685e; font-size: 25rpx; }
</style>
