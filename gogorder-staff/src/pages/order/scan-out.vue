<script setup lang="ts">
import { computed, ref } from 'vue'
import { scanOutOrder } from '../../api/order'
import type { StaffOrderDetail } from '../../types/order'
import { getCurrentShop } from '../../utils/session'

// 出餐兜底页：扫码可读 pickup_token；手动输入使用订单号/取餐号。
const orderCode = ref('')
const submitting = ref(false)
const result = ref<StaffOrderDetail | null>(null)
const currentShop = ref(getCurrentShop())
const isH5Runtime = ref(false)

// #ifdef H5
isH5Runtime.value = true
// #endif

const resultTitle = computed(() => {
  if (!result.value) return ''
  return `${result.value.pickupDisplay || '取餐'} · 已通知`
})

async function submitScanOut() {
  const code = orderCode.value.trim()
  if (!code) {
    uni.showToast({ title: '请输入订单号或取餐号', icon: 'none' })
    return
  }
  submitting.value = true
  try {
    result.value = await scanOutOrder(code)
    orderCode.value = ''
    uni.showToast({ title: '已通知取餐', icon: 'success' })
  } finally {
    submitting.value = false
  }
}

function scanCode() {
  // #ifdef H5
  uni.showToast({ title: 'H5 暂不支持扫码，请输入订单号', icon: 'none' })
  return
  // #endif

  // #ifndef H5
  if (typeof uni.scanCode !== 'function') {
    uni.showToast({ title: '当前环境不支持扫码，请手动输入', icon: 'none' })
    return
  }
  uni.scanCode({
    scanType: ['qrCode', 'barCode'],
    success: (res) => {
      orderCode.value = String(res.result || '').trim()
      void submitScanOut()
    },
    fail: () => {
      uni.showToast({ title: '扫码失败，请重试或手动输入', icon: 'none' })
    }
  })
  // #endif
}

function resetResult() {
  result.value = null
  orderCode.value = ''
}

function backToBoard() {
  uni.navigateBack()
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}
</script>

<template>
  <view class="safe-page verify-page">
    <view class="hero-card">
      <view>
        <text class="eyebrow">出餐兜底</text>
        <text class="hero-title">{{ currentShop?.shopName || '当前门店' }}</text>
      </view>
      <text class="back-link" @click="backToBoard">返回</text>
    </view>

    <view class="verify-card">
      <text class="card-title">扫码或输入订单号</text>
      <text v-if="isH5Runtime" class="h5-tip">当前是 H5 调试环境，浏览器扫码能力受限。请手动输入订单号或取餐号；微信小程序端仍可直接扫码。</text>
      <input v-model="orderCode" class="token-input" maxlength="64" placeholder="请输入订单号 / 取餐号" />
      <view class="button-row">
        <button class="scan-button" :class="{ disabled: isH5Runtime }" @click="scanCode">{{ isH5Runtime ? 'H5不可扫码' : '扫码' }}</button>
        <button class="submit-button" :loading="submitting" @click="submitScanOut">通知取餐</button>
      </view>
    </view>

    <view v-if="result" class="result-card">
      <view class="result-head">
        <text class="result-title">{{ resultTitle }}</text>
        <text class="result-amount">{{ formatMoney(result.totalAmount) }}</text>
      </view>
      <text class="order-no">#{{ result.orderNo }}</text>
      <view class="items">
        <view v-for="item in result.items" :key="item.productId + item.specText" class="item-line">
          <view>
            <text class="item-name">{{ item.productName }} × {{ item.quantity }}</text>
            <text class="item-spec">{{ item.specText }}</text>
          </view>
          <text class="item-price">{{ formatMoney(item.subtotal) }}</text>
        </view>
      </view>
      <button class="next-button" @click="resetResult">继续出餐</button>
    </view>
  </view>
</template>

<style scoped lang="scss">
.verify-page {
  min-height: 100vh;
  padding: calc(env(safe-area-inset-top) + 28rpx) 28rpx calc(env(safe-area-inset-bottom) + 42rpx);
  background:
    radial-gradient(circle at 90% 10%, rgba(231, 182, 117, .26), transparent 26%),
    linear-gradient(180deg, #fbf7ee 0%, #f7f4ee 45%, #f3eee4 100%);
}

.hero-card,
.verify-card,
.result-card {
  border: 1rpx solid #fff;
  border-radius: 34rpx;
  box-shadow: 0 18rpx 48rpx rgba(67, 57, 42, .08);
}

.hero-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 34rpx;
  background: linear-gradient(135deg, #181711 0%, #30291f 58%, #4a3422 100%);
  color: #fff;
}

.eyebrow {
  display: block;
  color: #d7ad75;
  font-size: 21rpx;
  font-weight: 800;
  letter-spacing: 5rpx;
}

.hero-title {
  display: block;
  max-width: 470rpx;
  margin-top: 12rpx;
  overflow: hidden;
  font-size: 38rpx;
  font-weight: 820;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.back-link {
  padding: 12rpx 18rpx;
  border: 1rpx solid rgba(215, 173, 117, .55);
  border-radius: 24rpx;
  color: #f1dfc3;
  font-size: 23rpx;
}

.verify-card,
.result-card {
  margin-top: 24rpx;
  padding: 30rpx;
  background: rgba(255, 255, 255, .92);
}

.card-title {
  display: block;
  color: #292620;
  font-size: 30rpx;
  font-weight: 800;
}

.h5-tip {
  display: block;
  margin-top: 16rpx;
  padding: 18rpx 20rpx;
  border-radius: 22rpx;
  background: #fff4de;
  color: #9a622d;
  font-size: 23rpx;
  line-height: 1.5;
}

.token-input {
  height: 92rpx;
  margin-top: 22rpx;
  padding: 0 24rpx;
  border: 1rpx solid #eadfce;
  border-radius: 26rpx;
  background: #fbf7ee;
  color: #292620;
  font-size: 28rpx;
}

.button-row {
  display: flex;
  gap: 18rpx;
  margin-top: 22rpx;
}

.scan-button,
.submit-button,
.next-button {
  height: 82rpx;
  border-radius: 26rpx;
  font-size: 27rpx;
  font-weight: 760;
  line-height: 82rpx;
}

.scan-button {
  flex: 1;
  background: #f1eadf;
  color: #8f6940;
}

.scan-button.disabled {
  opacity: .72;
}

.submit-button {
  flex: 2;
  background: #181711;
  color: #f1dfc3;
}

.result-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20rpx;
}

.result-title {
  color: #181711;
  font-size: 40rpx;
  font-weight: 860;
}

.result-amount {
  color: #9c6534;
  font-size: 28rpx;
  font-weight: 780;
}

.order-no {
  display: block;
  margin-top: 8rpx;
  color: #9a9388;
  font-size: 22rpx;
}

.items {
  margin-top: 24rpx;
  padding: 18rpx 20rpx;
  border-radius: 22rpx;
  background: #f8f3eb;
}

.item-line {
  display: flex;
  justify-content: space-between;
  gap: 18rpx;
  padding: 12rpx 0;
}

.item-name,
.item-spec {
  display: block;
}

.item-name {
  color: #292620;
  font-size: 25rpx;
  font-weight: 740;
}

.item-spec {
  margin-top: 6rpx;
  color: #9a9388;
  font-size: 22rpx;
}

.item-price {
  color: #7e756b;
  font-size: 24rpx;
}

.next-button {
  margin-top: 24rpx;
  background: #2a251d;
  color: #f1dfc3;
}
</style>
