<template>
  <view class="safe-page detail-page">
    <view class="nav-bar">
      <view class="back-button" @click="goBack">‹</view>
      <text>订单详情</text>
      <view class="nav-space" />
    </view>

    <view v-if="loading" class="loading-state">正在加载订单详情...</view>

    <template v-else-if="order">
      <view class="status-hero" :class="statusClass(order.orderStatus)">
        <view class="status-main">
          <text v-if="order.pickupDisplay && order.payStatus === 1" class="pickup-number">{{ order.pickupDisplay }}</text>
          <text class="status-title">{{ order.orderStatusDesc }}</text>
        </view>
        <text class="status-copy">{{ statusMessage(order.orderStatus) }}</text>
        <text class="status-tip">{{ statusTip(order.orderStatus) }}</text>
      </view>

      <view class="content">
        <view class="main-card">
          <view class="shop-row">
            <view class="order-type">{{ order.orderType === 'PREORDER' ? '预约' : '自取' }}</view>
            <text class="shop-name">{{ order.shopName || 'GOGORDER 门店' }}</text>
            <view v-if="order.shopPhone" class="phone-button" @click="callShop">
              <view class="phone-icon">⌕</view>
            </view>
          </view>

          <text class="section-title">已选商品</text>
          <view class="product-list">
            <view v-for="(item, index) in order.items" :key="`${item.productId}-${index}`" class="product-row">
              <image v-if="item.productImage" class="product-image" :src="item.productImage" mode="aspectFill" />
              <view v-else class="product-placeholder">GO</view>
              <view class="product-copy">
                <text class="product-name">{{ item.productName }}</text>
                <text class="product-spec">{{ item.specText || '默认规格' }}</text>
              </view>
              <view class="product-price">
                <text>¥{{ money(item.unitPrice) }}</text>
                <text>× {{ item.quantity }}</text>
              </view>
            </view>
          </view>

          <view class="remark-row">
            <text class="row-label">备注</text>
            <text class="row-value">{{ order.remark || '无' }}</text>
          </view>

          <view class="amount-list">
            <view class="amount-row">
              <text>商品总计</text>
              <text>¥{{ money(order.productAmount) }}</text>
            </view>
            <view v-if="order.packFee" class="amount-row">
              <text>打包费</text>
              <text>¥{{ money(order.packFee) }}</text>
            </view>
            <view class="amount-row">
              <text>支付方式</text>
              <text class="muted">余额支付</text>
            </view>
            <view class="amount-row total-row">
              <text>实付</text>
              <text class="total-amount">¥{{ money(order.totalAmount) }}</text>
            </view>
          </view>
        </view>

        <view class="info-card">
          <view class="info-row">
            <text>订单号</text>
            <text>{{ order.orderNo }}</text>
          </view>
          <view class="info-row">
            <text>预留电话</text>
            <text>{{ order.reservedPhone || '--' }}</text>
          </view>
          <view class="info-row">
            <text>下单时间</text>
            <text>{{ formatTime(order.createTime) }}</text>
          </view>
          <view v-if="order.scheduledPickupTime" class="info-row">
            <text>预约取餐</text>
            <text>{{ formatTime(order.scheduledPickupTime) }}</text>
          </view>
          <view v-if="order.payTime" class="info-row">
            <text>支付时间</text>
            <text>{{ formatTime(order.payTime) }}</text>
          </view>
          <view v-if="order.makeStartTime" class="info-row">
            <text>开始制作</text>
            <text>{{ formatTime(order.makeStartTime) }}</text>
          </view>
          <view v-if="order.completeMakeTime" class="info-row">
            <text>制作完成</text>
            <text>{{ formatTime(order.completeMakeTime) }}</text>
          </view>
          <view v-if="order.verifyTime" class="info-row">
            <text>取餐时间</text>
            <text>{{ formatTime(order.verifyTime) }}</text>
          </view>
          <view v-if="order.cancelTime" class="info-row">
            <text>取消时间</text>
            <text>{{ formatTime(order.cancelTime) }}</text>
          </view>
          <view v-if="order.cancelReason" class="info-row">
            <text>取消原因</text>
            <text>{{ order.cancelReason }}</text>
          </view>
        </view>
      </view>

      <view v-if="showActions" class="bottom-actions">
        <button
          v-if="order.orderStatus === 0"
          class="action-button secondary-button"
          :disabled="processing"
          @click="cancel"
        >
          取消订单
        </button>
        <button
          v-if="order.orderStatus === 0"
          class="action-button primary-button"
          :disabled="processing"
          @click="pay"
        >
          {{ processing ? '处理中...' : '去支付' }}
        </button>
        <button
          v-if="order.orderStatus === 4 || order.orderStatus === 5"
          class="action-button primary-button full-button"
          @click="orderAgain"
        >
          再来一单
        </button>
      </view>
    </template>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad, onShow } from '@dcloudio/uni-app'
import { clearCart } from '../../api/cart'
import { cancelOrder, getOrderDetail, payOrder } from '../../api/order'
import type { OrderDetail } from '../../types/order'
import { ApiRequestError } from '../../utils/request'

const orderId = ref(0)
const order = ref<OrderDetail | null>(null)
const loading = ref(true)
const processing = ref(false)

const showActions = computed(() => {
  const status = order.value?.orderStatus
  return status === 0 || status === 4 || status === 5
})

onLoad((options?: Record<string, unknown>) => {
  orderId.value = Number(options?.orderId)
})

onShow(loadDetail)

async function loadDetail() {
  if (!orderId.value) return
  loading.value = true
  try {
    order.value = await getOrderDetail(orderId.value)
  } finally {
    loading.value = false
  }
}

/** 支付订单：成功后清购物车、刷新详情并弹窗展示取餐号；余额不足时弹窗展示余额与需付金额。 */
async function pay() {
  if (!order.value || processing.value) return
  processing.value = true
  try {
    const paid = await payOrder(order.value.orderId)
    await clearCart(order.value.shopId)
    await loadDetail()
    await uni.showModal({
      title: '支付成功',
      content: paid.pickupDisplay ? `门店已接单，取餐号为 ${paid.pickupDisplay}` : '门店已接单，请留意订单进度',
      showCancel: false
    })
  } catch (error) {
    const balanceError = getBalanceError(error)
    if (balanceError) {
      await uni.showModal({
        title: '余额不足',
        content: `当前余额 ¥${money(balanceError.balance)}，本单需支付 ¥${money(balanceError.required)}`,
        showCancel: false
      })
    } else {
      uni.showToast({ title: error instanceof Error ? error.message : '支付失败', icon: 'none' })
    }
  } finally {
    processing.value = false
  }
}

/** 取消未支付订单：二次确认后调用接口，成功刷新详情。 */
async function cancel() {
  if (!order.value || processing.value) return
  const result = await uni.showModal({
    title: '取消订单',
    content: '确认取消这笔待支付订单吗？',
    confirmText: '确认取消',
    confirmColor: '#9c5650'
  })
  if (!result.confirm) return
  processing.value = true
  try {
    await cancelOrder(order.value.orderId)
    await loadDetail()
    uni.showToast({ title: '订单已取消', icon: 'success' })
  } finally {
    processing.value = false
  }
}

function orderAgain() {
  if (!order.value) return
  uni.navigateTo({ url: `/pages/shop/detail?id=${order.value.shopId}` })
}

function callShop() {
  if (!order.value?.shopPhone) return
  uni.makePhoneCall({ phoneNumber: order.value.shopPhone })
}

function goBack() {
  uni.navigateBack()
}

/** 从支付异常中提取 {balance, required}（余额不足时后端附带），用于弹窗展示明细。 */
function getBalanceError(error: unknown): { balance: number; required: number } | null {
  if (!(error instanceof ApiRequestError) || !error.data || typeof error.data !== 'object') return null
  const data = error.data as { balance?: number; required?: number }
  if (!Number.isFinite(data.balance) || !Number.isFinite(data.required)) return null
  return { balance: Number(data.balance), required: Number(data.required) }
}

function statusClass(status: number): string {
  if (status === 0) return 'pending'
  if (status === 1 || status === 2 || status === 3) return 'active-order'
  if (status === 4) return 'completed'
  return 'cancelled'
}

function statusMessage(status: number): string {
  if (status === 0) return '订单已创建，请尽快完成支付'
  if (status === 1) return '门店已接单，正在等待制作'
  if (status === 2) return '正在制作，马上就好'
  if (status === 3) return '饮品制作完成，请及时取餐'
  if (status === 4) return '本次订单已完成'
  return '本次订单已取消'
}

function statusTip(status: number): string {
  if (status === 0) return '超时未支付，订单将自动取消'
  if (status === 1 || status === 2) return '预计时长仅供参考，请耐心等待'
  if (status === 3) return '请向门店工作人员出示取餐号'
  if (status === 4) return '感谢你的光临，期待再次见面'
  return order.value?.cancelReason || '可重新选择商品下单'
}

function money(value: number): string {
  return (Number(value || 0) / 100).toFixed(2)
}

function formatTime(value: string): string {
  return value ? value.replace('T', ' ').slice(0, 16) : '--'
}
</script>

<style lang="scss" scoped>
.detail-page { min-height: 100vh; padding-bottom: calc(40rpx + env(safe-area-inset-bottom)); background: #f5f5f5; }
.nav-bar { height: 88rpx; padding: var(--status-bar-height) 24rpx 0; background: #fff; color: #222; display: flex; align-items: center; justify-content: space-between; font-size: 28rpx; font-weight: 700; }.back-button, .nav-space { width: 64rpx; }.back-button { height: 64rpx; border-radius: 50%; background: #f5f5f5; font-size: 47rpx; font-weight: 400; line-height: 57rpx; text-align: center; }
.loading-state { padding-top: 260rpx; color: #aaa; font-size: 23rpx; text-align: center; }

.status-hero { padding: 40rpx 48rpx 42rpx; background: #fff; }.status-main { display: flex; align-items: baseline; gap: 15rpx; }.pickup-number { color: #075fea; font-size: 54rpx; font-weight: 900; letter-spacing: 2rpx; }.status-title { color: #202020; font-size: 31rpx; font-weight: 900; }.status-copy { display: block; margin-top: 13rpx; color: #075fea; font-size: 22rpx; }.status-tip { display: block; margin-top: 16rpx; color: #aaa; font-size: 18rpx; }.status-hero.pending .status-copy { color: #c57d24; }.status-hero.completed .status-copy, .status-hero.cancelled .status-copy { color: #888; }

.content { padding: 26rpx 22rpx 20rpx; }.main-card, .info-card { border-radius: 29rpx; background: #fff; }.main-card { padding: 28rpx 30rpx 12rpx; }.shop-row { height: 58rpx; display: flex; align-items: center; }.order-type { padding: 8rpx 18rpx; border-radius: 25rpx; background: #075fea; color: #fff; font-size: 20rpx; font-weight: 800; }.shop-name { min-width: 0; flex: 1; margin-left: 13rpx; overflow: hidden; color: #333; font-size: 25rpx; font-weight: 800; text-overflow: ellipsis; white-space: nowrap; }.phone-button { width: 56rpx; height: 56rpx; border-radius: 50%; color: #075fea; display: flex; align-items: center; justify-content: center; }.phone-icon { font-size: 35rpx; transform: rotate(-35deg); }
.section-title { display: block; margin-top: 20rpx; color: #333; font-size: 24rpx; font-weight: 800; }.product-list { margin-top: 8rpx; }.product-row { padding: 19rpx 0; display: flex; align-items: center; }.product-image, .product-placeholder { width: 105rpx; height: 105rpx; flex: none; border-radius: 18rpx; background: #f5f5f5; }.product-placeholder { color: #075fea; font-size: 24rpx; font-weight: 900; line-height: 105rpx; text-align: center; }.product-copy { min-width: 0; flex: 1; margin-left: 18rpx; display: flex; flex-direction: column; }.product-name { max-width: 350rpx; overflow: hidden; font-size: 23rpx; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }.product-spec { max-width: 370rpx; margin-top: 8rpx; overflow: hidden; color: #aaa; font-size: 17rpx; text-overflow: ellipsis; white-space: nowrap; }.product-price { margin-left: 12rpx; display: flex; flex-direction: column; align-items: flex-end; gap: 8rpx; font-size: 19rpx; }.product-price text:first-child { color: #075fea; font-size: 25rpx; font-weight: 800; }.product-price text:last-child { color: #777; }
.remark-row { min-height: 84rpx; border-top: 1rpx solid #eee; border-bottom: 1rpx solid #eee; display: flex; align-items: center; }.row-label { color: #333; font-size: 23rpx; font-weight: 700; }.row-value { min-width: 0; flex: 1; margin-left: 25rpx; overflow: hidden; color: #aaa; font-size: 20rpx; text-align: right; text-overflow: ellipsis; white-space: nowrap; }
.amount-list { padding: 10rpx 0; }.amount-row { min-height: 64rpx; color: #555; display: flex; align-items: center; justify-content: space-between; font-size: 21rpx; }.amount-row + .amount-row { border-top: 1rpx solid #f0f0f0; }.muted { color: #aaa; }.total-row { color: #333; }.total-amount { color: #075fea; font-size: 31rpx; font-weight: 900; }
.info-card { margin-top: 22rpx; padding: 20rpx 30rpx; }.info-row { min-height: 55rpx; color: #777; display: flex; align-items: center; justify-content: space-between; gap: 30rpx; font-size: 20rpx; }.info-row text:last-child { color: #aaa; text-align: right; }

.bottom-actions { position: sticky; bottom: 0; margin-top: 8rpx; padding: 20rpx 24rpx calc(20rpx + env(safe-area-inset-bottom)); border-top: 1rpx solid #eee; background: rgba(255,255,255,.96); display: flex; gap: 16rpx; }.action-button { height: 78rpx; flex: 1; margin: 0; border-radius: 39rpx; font-size: 24rpx; line-height: 78rpx; }.secondary-button { border: 1rpx solid #ddd; background: #fff; color: #666; }.primary-button { background: #075fea; color: #fff; font-weight: 800; }.full-button { width: 100%; flex: auto; }.action-button[disabled] { opacity: .55; }
</style>
