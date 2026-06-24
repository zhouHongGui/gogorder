<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getMyShops, switchShop } from '../../api/shop'
import type { ShopContext } from '../../types/auth'
import { getCurrentShop, getShops, saveShopContext } from '../../utils/session'

const loading = ref(false)
const switchingId = ref<number | null>(null)
const shops = ref<ShopContext[]>(getShops())
const currentShop = ref<ShopContext | null>(getCurrentShop())

const sortedShops = computed(() => {
  const currentId = currentShop.value?.shopId
  return [...shops.value].sort((a, b) => {
    if (a.shopId === currentId) return -1
    if (b.shopId === currentId) return 1
    if (a.isDefault !== b.isDefault) return Number(b.isDefault || 0) - Number(a.isDefault || 0)
    return a.shopId - b.shopId
  })
})

onShow(() => {
  void loadShops()
})

onPullDownRefresh(async () => {
  try {
    await loadShops()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadShops() {
  if (loading.value) return
  loading.value = true
  try {
    const result = await getMyShops()
    shops.value = result.shops
    currentShop.value = result.currentShop
    saveShopContext(result.shops, result.currentShop)
  } catch {
    // request 已统一处理错误。
  } finally {
    loading.value = false
  }
}

async function selectShop(shop: ShopContext) {
  if (shop.shopId === currentShop.value?.shopId) {
    uni.navigateBack()
    return
  }
  if (switchingId.value) return
  switchingId.value = shop.shopId
  try {
    const selected = await switchShop(shop.shopId)
    currentShop.value = selected
    saveShopContext(shops.value, selected)
    uni.showToast({ title: '门店已切换', icon: 'success' })
    setTimeout(() => {
      uni.reLaunch({ url: '/pages/workbench/index' })
    }, 350)
  } catch {
    // request 已统一展示错误。
  } finally {
    switchingId.value = null
  }
}

function goBack() {
  uni.navigateBack()
}

function statusText(status?: number) {
  if (status === 1) return '营业中'
  if (status === 2) return '暂停即时单'
  return '休息中'
}

function statusClass(status?: number) {
  return `status-${status ?? 0}`
}

function shopInitial(name?: string) {
  return (name || 'GO').slice(0, 1)
}
</script>

<template>
  <view class="safe-page shop-select-page">
    <view class="hero">
      <view class="nav-line">
        <text class="back-link" @click="goBack">返回</text>
        <text class="shop-count">{{ shops.length }} 家授权门店</text>
      </view>
      <text class="eyebrow">AUTHORIZED SHOPS</text>
      <text class="hero-title">选择操作门店</text>
      <text class="hero-desc">选择后，订单看板、核销和门店操作都会切换到对应门店。</text>
    </view>

    <view class="list-wrap">
      <view v-if="loading && !shops.length" class="empty-card">
        <text class="empty-title">正在加载门店</text>
        <text class="empty-desc">稍等一下，正在读取你的授权门店。</text>
      </view>

      <view v-else-if="!shops.length" class="empty-card">
        <text class="empty-title">暂无授权门店</text>
        <text class="empty-desc">请联系管理员在后台给该员工授权门店。</text>
      </view>

      <view
        v-for="shop in sortedShops"
        :key="shop.shopId"
        class="shop-card"
        :class="{ current: shop.shopId === currentShop?.shopId }"
        @click="selectShop(shop)"
      >
        <view class="shop-mark">{{ shopInitial(shop.shopName) }}</view>
        <view class="shop-copy">
          <view class="shop-title-row">
            <text class="shop-title">{{ shop.shopName }}</text>
            <text v-if="shop.shopId === currentShop?.shopId" class="current-chip">当前</text>
            <text v-else-if="shop.isDefault === 1" class="default-chip">默认</text>
          </view>
          <text class="shop-code">{{ shop.shopCode || '未配置门店编码' }}</text>
          <text class="shop-address">{{ shop.address || '未配置地址' }}</text>
          <view class="shop-footer">
            <view class="status-row">
              <text class="status-dot" :class="statusClass(shop.status)" />
              <text class="status-text">{{ statusText(shop.status) }}</text>
            </view>
            <text class="select-text">
              {{ switchingId === shop.shopId ? '切换中...' : shop.shopId === currentShop?.shopId ? '正在使用' : '进入门店 ›' }}
            </text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.shop-select-page {
  min-height: 100vh;
  padding-bottom: calc(env(safe-area-inset-bottom) + 44rpx);
  background:
    radial-gradient(circle at 86% 8%, rgba(231, 182, 117, .32), transparent 28%),
    linear-gradient(180deg, #fbf7ee 0%, #f5f1e9 46%, #eee7dc 100%);
}

.hero {
  padding: calc(env(safe-area-inset-top) + 28rpx) 28rpx 38rpx;
}

.nav-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 36rpx;
}

.back-link,
.shop-count {
  padding: 12rpx 18rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .78);
  color: #8f6233;
  font-size: 23rpx;
  font-weight: 740;
}

.shop-count {
  color: #8f887c;
}

.eyebrow {
  display: block;
  color: #b17d42;
  font-size: 20rpx;
  font-weight: 850;
  letter-spacing: 5rpx;
}

.hero-title {
  display: block;
  margin-top: 10rpx;
  color: #181711;
  font-size: 48rpx;
  font-weight: 900;
  line-height: 1.16;
}

.hero-desc {
  display: block;
  max-width: 640rpx;
  margin-top: 16rpx;
  color: #82786c;
  font-size: 25rpx;
  line-height: 1.55;
}

.list-wrap {
  padding: 0 28rpx;
}

.empty-card,
.shop-card {
  border: 1rpx solid rgba(255, 255, 255, .86);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 16rpx 42rpx rgba(67, 57, 42, .07);
}

.empty-card {
  padding: 46rpx 30rpx;
  text-align: center;
}

.empty-title {
  display: block;
  color: #292620;
  font-size: 31rpx;
  font-weight: 860;
}

.empty-desc {
  display: block;
  margin-top: 12rpx;
  color: #9a9388;
  font-size: 24rpx;
  line-height: 1.5;
}

.shop-card {
  display: flex;
  gap: 22rpx;
  margin-bottom: 20rpx;
  padding: 26rpx;
}

.shop-card.current {
  border-color: rgba(231, 182, 117, .86);
  background: #fffaf0;
  box-shadow: 0 18rpx 46rpx rgba(174, 111, 48, .12);
}

.shop-mark {
  display: flex;
  width: 92rpx;
  height: 92rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 31rpx;
  background: linear-gradient(145deg, #181711, #4a3422);
  color: #e8ba7c;
  font-size: 34rpx;
  font-weight: 900;
}

.shop-card.current .shop-mark {
  background: linear-gradient(145deg, #ce692d, #e7b675);
  color: #fff;
}

.shop-copy {
  min-width: 0;
  flex: 1;
}

.shop-title-row,
.shop-footer,
.status-row {
  display: flex;
  align-items: center;
}

.shop-title-row,
.shop-footer {
  justify-content: space-between;
  gap: 14rpx;
}

.shop-title {
  min-width: 0;
  overflow: hidden;
  color: #181711;
  font-size: 31rpx;
  font-weight: 860;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.current-chip,
.default-chip {
  flex-shrink: 0;
  padding: 8rpx 14rpx;
  border-radius: 19rpx;
  font-size: 20rpx;
  font-weight: 780;
}

.current-chip {
  background: #181711;
  color: #f1dfc3;
}

.default-chip {
  background: #f1e4d2;
  color: #9c6534;
}

.shop-code,
.shop-address {
  display: block;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-code {
  margin-top: 9rpx;
  color: #b17d42;
  font-size: 21rpx;
  font-weight: 720;
}

.shop-address {
  margin-top: 8rpx;
  color: #92897d;
  font-size: 22rpx;
}

.shop-footer {
  margin-top: 20rpx;
  padding-top: 18rpx;
  border-top: 1rpx solid #f0e6d9;
}

.status-dot {
  width: 13rpx;
  height: 13rpx;
  border-radius: 50%;
}

.status-dot.status-0 {
  background: #9d9589;
}

.status-dot.status-1 {
  background: #3f9a61;
  box-shadow: 0 0 0 8rpx rgba(63, 154, 97, .12);
}

.status-dot.status-2 {
  background: #d18b32;
  box-shadow: 0 0 0 8rpx rgba(209, 139, 50, .12);
}

.status-text {
  margin-left: 13rpx;
  color: #7e756b;
  font-size: 22rpx;
}

.select-text {
  flex-shrink: 0;
  color: #9c6534;
  font-size: 23rpx;
  font-weight: 780;
}
</style>
