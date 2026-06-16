<template>
  <view class="safe-page confirm-page">
    <view class="store-card">
      <image class="store-logo" src="/static/brand/store-logo.png" mode="aspectFit" />
      <view class="store-copy">
        <text class="eyebrow">PICKUP STORE</text>
        <text class="store-name">{{ shop?.name || cart.shopName }}</text>
        <text class="store-address">{{ shop?.address }}</text>
      </view>
    </view>

    <view class="card">
      <view class="section-title-row">
        <text class="section-title">订单商品</text>
        <text class="section-count">共 {{ cart.totalCount }} 件</text>
      </view>
      <view v-for="item in cart.items" :key="item.cartItemId" class="order-item">
        <view class="item-image" :style="item.image ? { backgroundImage: `url(${item.image})` } : {}">
          <text v-if="!item.image">GO</text>
        </view>
        <view class="item-copy">
          <text class="item-name">{{ item.productName }}</text>
          <text class="item-spec">{{ item.specText || '默认规格' }}</text>
          <text class="item-price">¥{{ money(item.unitPrice) }}</text>
        </view>
        <text class="item-quantity">×{{ item.quantity }}</text>
      </view>
    </view>

    <view class="card">
      <view class="section-title-row">
        <text class="section-title">取餐方式</text>
        <text class="type-chip">{{ orderType === 'NORMAL' ? '即时自取' : '预约自取' }}</text>
      </view>
      <view class="type-switch">
        <view
          class="type-option"
          :class="{ active: orderType === 'NORMAL', disabled: !shop?.instantAvailable }"
          @click="selectNormal"
        >
          <text class="type-name">即时单</text>
          <text class="type-copy">支付后立即制作</text>
        </view>
        <view class="type-option" :class="{ active: orderType === 'PREORDER' }" @click="selectPreorder">
          <text class="type-name">预约单</text>
          <text class="type-copy">选择时间再制作</text>
        </view>
      </view>
      <picker
        v-if="orderType === 'PREORDER'"
        mode="selector"
        :range="slotLabels"
        :value="slotIndex"
        @change="onSlotChange"
      >
        <view class="field-row">
          <view>
            <text class="field-label">预约取餐时间</text>
            <text class="field-value">{{ selectedSlotLabel || '请选择可预约时间' }}</text>
          </view>
          <text class="field-arrow">›</text>
        </view>
      </picker>
      <view class="remark-field">
        <text class="field-label">订单备注</text>
        <textarea v-model="remark" maxlength="200" placeholder="口味偏好或其他备注（选填）" />
      </view>
    </view>

    <view class="card amount-card">
      <view class="amount-row"><text>商品金额</text><text>¥{{ money(cart.totalAmount) }}</text></view>
      <view class="amount-row"><text>包装费</text><text>¥{{ money(packFee) }}</text></view>
      <view class="amount-row total-row"><text>应付合计</text><text>¥{{ money(totalAmount) }}</text></view>
    </view>

    <view class="bottom-space" />
    <view class="submit-bar">
      <view>
        <text class="submit-label">合计</text>
        <text class="submit-total">¥{{ money(totalAmount) }}</text>
      </view>
      <button class="submit-button" :disabled="submitting || !cart.totalCount" @click="submitAndPay">
        {{ submitting ? '处理中...' : '确认并支付' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { clearCart, getCart } from '../../api/cart'
import { payOrder, submitOrder } from '../../api/order'
import { getPreorderSlots, getShopDetail } from '../../api/shop'
import type { Cart } from '../../types/cart'
import type { OrderType, PreorderSlot, Shop } from '../../types/shop'
import { ApiRequestError } from '../../utils/request'

/** 生成空购物车对象（初始化用）。 */
const emptyCart = (): Cart => ({ shopId: null, shopName: '', items: [], totalAmount: 0, totalCount: 0 })
const shop = ref<Shop | null>(null)
const cart = ref<Cart>(emptyCart())
const orderType = ref<OrderType>('NORMAL')
const slots = ref<PreorderSlot[]>([])
const slotIndex = ref(0)
const remark = ref('')
const submitting = ref(false)
// 下单幂等令牌：页面级唯一，submit 失败重试复用同一 token（后端按 token 幂等返回同一订单）。
const submitToken = createSubmitToken()

/** 包装费（分）= 总杯数 × 每杯包装费。 */
const packFee = computed(() => cart.value.totalCount * Number(shop.value?.packFee || 0))
/** 应付合计（分）= 商品金额 + 包装费。 */
const totalAmount = computed(() => cart.value.totalAmount + packFee.value)
/** 预约时段展示文案列表（如「今天 09:30」）。 */
const slotLabels = computed(() => slots.value.map(slot => `${slot.dateLabel} ${slot.timeLabel}`))
/** 当前选中的预约时段。 */
const selectedSlot = computed(() => slots.value[slotIndex.value])
/** 当前选中时段的展示文案。 */
const selectedSlotLabel = computed(() => selectedSlot.value ? slotLabels.value[slotIndex.value] : '')

/**
 * 页面加载：从路由参数取 shopId/orderType，并行拉取门店详情与购物车。
 * 门店不支持即时单时强制切预订单；购物车为空则提示并返回。
 */
onLoad(async (options?: Record<string, unknown>) => {
  const shopId = Number(options?.shopId)
  orderType.value = options?.orderType === 'PREORDER' ? 'PREORDER' : 'NORMAL'
  if (!shopId) {
    uni.navigateBack()
    return
  }
  try {
    // 并行拉门店详情与购物车，减少等待。
    const [shopData, cartData] = await Promise.all([getShopDetail(shopId), getCart(shopId)])
    shop.value = shopData
    cart.value = cartData
    if (!shopData.instantAvailable) orderType.value = 'PREORDER'
    if (orderType.value === 'PREORDER') await loadSlots()
    if (!cartData.totalCount) {
      uni.showToast({ title: '购物车是空的', icon: 'none' })
      setTimeout(() => uni.navigateBack(), 500)
    }
  } catch {
    uni.showToast({ title: '确认订单加载失败', icon: 'none' })
  }
})

/** 加载预订单可选取餐时段，重置选中索引为第一个。 */
async function loadSlots() {
  if (!shop.value) return
  slots.value = await getPreorderSlots(shop.value.id)
  slotIndex.value = 0
}

/** 选择即时单：门店不支持即时单时拦截提示。 */
function selectNormal() {
  if (!shop.value?.instantAvailable) {
    uni.showToast({ title: '当前门店暂不支持即时单', icon: 'none' })
    return
  }
  orderType.value = 'NORMAL'
}

/** 选择预订单：首次切到预订单时加载时段。 */
async function selectPreorder() {
  orderType.value = 'PREORDER'
  if (!slots.value.length) await loadSlots()
}

/** 预约时段选择器变更回调，更新选中索引。 */
function onSlotChange(event: { detail: { value: string | number } }) {
  slotIndex.value = Number(event.detail.value)
}

/**
 * 确认下单并支付（下单 → 支付 → 清购物车 → 跳结果页）。
 * 下单成功但支付失败时，带 orderId 跳结果页（结果页可重新支付）；
 * 下单前失败则仅 toast 提示。submitToken 保证幂等，网络重试不会重复下单。
 */
async function submitAndPay() {
  if (!shop.value || !cart.value.totalCount || submitting.value) return
  // 预订单必须选时段。
  if (orderType.value === 'PREORDER' && !selectedSlot.value) {
    uni.showToast({ title: '请选择预约取餐时间', icon: 'none' })
    return
  }
  submitting.value = true
  let orderId = 0
  try {
    const submitted = await submitOrder({
      shopId: shop.value.id,
      submitToken,
      orderType: orderType.value,
      scheduledPickupTime: orderType.value === 'PREORDER' ? selectedSlot.value.value : undefined,
      remark: remark.value.trim(),
      items: cart.value.items.map(item => ({
        productId: item.productId,
        specs: item.specs,
        quantity: item.quantity
      }))
    })
    orderId = submitted.orderId
    // 下单成功后立即支付。
    const paid = await payOrder(orderId)
    // 支付成功清空购物车并跳结果页。
    await clearCart(shop.value.id)
    uni.redirectTo({
      url: `/pages/order/result?orderId=${orderId}&success=true&balanceAfter=${paid.balanceAfter}`
    })
  } catch (error) {
    // 格式化错误信息（余额不足时展示余额与需付金额）。
    const message = formatOrderError(error)
    if (orderId) {
      // 已下单但支付失败：带 orderId 跳结果页（支持重新支付）。
      uni.redirectTo({
        url: `/pages/order/result?orderId=${orderId}&success=false&error=${encodeURIComponent(message)}`
      })
    } else {
      // 下单前失败：仅 toast。
      uni.showToast({ title: message, icon: 'none' })
    }
  } finally {
    submitting.value = false
  }
}

/**
 * 格式化订单错误信息。余额不足时从 error.data 取 {balance, required} 展示明细，
 * 否则返回通用错误消息。
 */
function formatOrderError(error: unknown): string {
  if (error instanceof ApiRequestError && error.data && typeof error.data === 'object') {
    const data = error.data as { balance?: number; required?: number }
    if (Number.isFinite(data.balance) && Number.isFinite(data.required)) {
      return `余额不足：当前余额 ¥${money(Number(data.balance))}，订单需支付 ¥${money(Number(data.required))}`
    }
  }
  return error instanceof Error ? error.message : '订单处理失败'
}

/** 生成下单幂等令牌（时间戳+两段随机），页面级唯一。 */
function createSubmitToken(): string {
  return `${Date.now()}-${Math.random().toString(16).slice(2)}-${Math.random().toString(16).slice(2)}`
}

/** 分转元展示（除以 100 保留两位）。 */
function money(value: number): string {
  return (Number(value || 0) / 100).toFixed(2)
}
</script>

<style lang="scss" scoped>
.confirm-page { padding: 26rpx 24rpx 180rpx; }
.store-card, .card { margin-bottom: 22rpx; border-radius: 30rpx; background: #fff; box-shadow: 0 14rpx 45rpx rgba(67, 57, 42, .06); }
.store-card { display: flex; align-items: center; padding: 28rpx; background: linear-gradient(135deg, #181711, #383229); color: #fff; }
.store-logo { width: 92rpx; height: 92rpx; margin-right: 22rpx; border-radius: 26rpx; background: #fff; }
.store-copy { min-width: 0; display: flex; flex-direction: column; }
.eyebrow { color: #d7ad75; font-size: 18rpx; letter-spacing: 4rpx; }
.store-name { margin-top: 8rpx; font-size: 31rpx; font-weight: 800; }
.store-address { margin-top: 8rpx; color: rgba(255,255,255,.65); font-size: 22rpx; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.card { padding: 30rpx; }
.section-title-row, .amount-row, .field-row { display: flex; align-items: center; justify-content: space-between; }
.section-title { font-size: 30rpx; font-weight: 800; }
.section-count { color: #999184; font-size: 22rpx; }
.order-item { display: flex; align-items: center; padding: 28rpx 0; border-bottom: 1rpx solid #f0ebe3; }
.order-item:last-child { border-bottom: 0; padding-bottom: 0; }
.item-image { width: 104rpx; height: 104rpx; flex: none; border-radius: 24rpx; background: #f3eee5 center/cover; display: flex; align-items: center; justify-content: center; color: #b18a55; font-weight: 800; }
.item-copy { min-width: 0; flex: 1; margin-left: 22rpx; display: flex; flex-direction: column; }
.item-name { font-size: 27rpx; font-weight: 700; }
.item-spec { margin-top: 8rpx; color: #9a9286; font-size: 21rpx; }
.item-price { margin-top: 12rpx; color: #bc7741; font-size: 25rpx; font-weight: 700; }
.item-quantity { color: #777064; font-size: 24rpx; }
.type-chip { padding: 8rpx 15rpx; border-radius: 20rpx; background: #f2e7d8; color: #a46d35; font-size: 20rpx; }
.type-switch { display: flex; gap: 18rpx; margin-top: 26rpx; }
.type-option { flex: 1; padding: 24rpx; border: 2rpx solid #eee8dd; border-radius: 24rpx; display: flex; flex-direction: column; }
.type-option.active { border-color: #181711; background: #f5f1e9; }
.type-option.disabled { opacity: .45; }
.type-name { font-size: 26rpx; font-weight: 700; }
.type-copy { margin-top: 8rpx; color: #9b9387; font-size: 20rpx; }
.field-row { margin-top: 24rpx; padding: 25rpx 0; border-top: 1rpx solid #f0ebe3; border-bottom: 1rpx solid #f0ebe3; }
.field-label { display: block; color: #928a7e; font-size: 21rpx; }
.field-value { display: block; margin-top: 8rpx; font-size: 26rpx; font-weight: 600; }
.field-arrow { color: #ad9271; font-size: 40rpx; }
.remark-field { margin-top: 24rpx; }
.remark-field textarea { width: 100%; height: 120rpx; box-sizing: border-box; margin-top: 15rpx; padding: 20rpx; border-radius: 20rpx; background: #f8f5ef; font-size: 24rpx; }
.amount-card { padding-top: 20rpx; padding-bottom: 20rpx; }
.amount-row { padding: 13rpx 0; color: #7e776c; font-size: 24rpx; }
.total-row { margin-top: 8rpx; padding-top: 22rpx; border-top: 1rpx solid #eee8de; color: #181711; font-size: 28rpx; font-weight: 800; }
.submit-bar { position: fixed; z-index: 10; left: 0; right: 0; bottom: 0; display: flex; align-items: center; justify-content: space-between; padding: 20rpx 28rpx calc(20rpx + env(safe-area-inset-bottom)); background: rgba(255,255,255,.96); box-shadow: 0 -12rpx 35rpx rgba(40,34,25,.08); }
.submit-label { color: #948c80; font-size: 21rpx; }
.submit-total { display: block; margin-top: 3rpx; font-size: 36rpx; font-weight: 800; }
.submit-button { width: 300rpx; height: 84rpx; margin: 0; border-radius: 42rpx; background: #181711; color: #fff; font-size: 28rpx; font-weight: 700; line-height: 84rpx; }
.submit-button[disabled] { opacity: .55; }
</style>
