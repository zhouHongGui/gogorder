<template>
  <view class="safe-page detail-page">
    <view class="top-bar">
      <view class="back-button" @click="goBack">‹</view>
      <text class="top-title">门店详情</text>
      <view class="top-placeholder" />
    </view>

    <scroll-view class="detail-scroll" scroll-y :show-scrollbar="false">
      <view v-if="loading" class="loading-state">
        <view class="loading-ring" />
        <text>正在加载门店信息...</text>
      </view>

      <view v-else-if="shop" class="detail-content">
        <view
          class="hero"
          :style="shop.image ? { backgroundImage: `url(${shop.image})` } : {}"
        >
          <view v-if="!shop.image" class="hero-fallback">
            <view class="hero-logo-card">
              <image class="hero-logo" src="/static/brand/store-logo.png" mode="aspectFit" />
            </view>
          </view>
          <view class="hero-shade" />
          <view class="hero-copy">
            <view class="hero-status" :class="statusClass">
              <view class="status-dot" />
              <text>{{ shop.statusName }}</text>
            </view>
            <text class="shop-name">{{ shop.name }}</text>
            <text class="shop-address">{{ shop.address || '暂未填写门店地址' }}</text>
          </view>
        </view>

        <view class="summary-card">
          <view class="summary-item">
            <text class="summary-value">{{ distanceText }}</text>
            <text class="summary-label">距你</text>
          </view>
          <view class="summary-divider" />
          <view class="summary-item">
            <text class="summary-value">自取 / 外卖</text>
            <text class="summary-label">当前服务</text>
          </view>
          <view class="summary-divider" />
          <view class="summary-item">
            <text class="summary-value">{{ shop.packFee ? `¥${money(shop.packFee)}` : '免费' }}</text>
            <text class="summary-label">打包费</text>
          </view>
        </view>

        <view v-if="!shop.instantAvailable" class="service-tip">
          <view class="tip-icon">!</view>
          <view>
            <text class="tip-title">当前仅支持预约取餐</text>
            <text class="tip-copy">你仍可浏览菜单并选择合适的预约时间。</text>
          </view>
        </view>

        <view class="info-card">
          <view class="section-heading">
            <view>
              <text class="section-kicker">STORE INFO</text>
              <text class="section-title">门店信息</text>
            </view>
            <view class="info-action" @click="openLocation">导航</view>
          </view>

          <view class="info-row">
            <view class="info-icon clock-icon">
              <view class="clock-hand hour" />
              <view class="clock-hand minute" />
            </view>
            <view class="info-copy">
              <text class="info-label">营业时间</text>
              <text class="info-value">{{ businessHours(shop) }}</text>
            </view>
          </view>

          <view class="info-row" @click="openLocation">
            <view class="info-icon location-icon"><view class="location-dot" /></view>
            <view class="info-copy">
              <text class="info-label">门店地址</text>
              <text class="info-value">{{ shop.address || '暂未填写门店地址' }}</text>
            </view>
            <text class="row-arrow">›</text>
          </view>

          <view v-if="shop.phone" class="info-row" @click="callShop">
            <view class="info-icon phone-icon">☎</view>
            <view class="info-copy">
              <text class="info-label">联系电话</text>
              <text class="info-value">{{ shop.phone }}</text>
            </view>
            <text class="row-arrow">›</text>
          </view>
        </view>

        <view class="notice-card">
          <view class="notice-heading">
            <text class="notice-label">店内公告</text>
            <text class="notice-mark">GO</text>
          </view>
          <text class="notice-copy">{{ shop.notice || '门店饮品均为现点现做，请及时取餐。' }}</text>
        </view>

        <view class="map-section">
          <view class="section-heading map-heading">
            <view>
              <text class="section-kicker">LOCATION</text>
              <text class="section-title">门店位置</text>
            </view>
            <text class="distance-chip">{{ distanceText }}</text>
          </view>
          <view class="map-card">
            <shop-map :shops="[shop]" :center="mapCenter" :focused-shop-id="shop.id" />
          </view>
        </view>

        <view class="bottom-space" />
      </view>

      <view v-else class="empty-state">
        <text class="empty-mark">GO</text>
        <text class="empty-title">门店信息加载失败</text>
        <text class="empty-copy">请返回门店列表后重新选择。</text>
      </view>
    </scroll-view>

    <view v-if="shop" class="bottom-actions">
      <view class="select-action" :class="{ selected: isCurrentShop }" @click="selectShop">
        <text>{{ isCurrentShop ? '当前门店' : '选择此店' }}</text>
      </view>
      <button class="preorder-action" @click="startDelivery">外卖</button>
      <button
        class="order-action"
        :class="{ disabled: !shop.instantAvailable }"
        @click="startOrder('NORMAL')"
      >
        {{ shop.instantAvailable ? '自取点单' : '预约自取' }}
      </button>
    </view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import ShopMap from '../../components/ShopMap/index.vue'
import { getShopDetail } from '../../api/shop'
import { businessHours, formatDistance, getCurrentShop, saveCurrentShop } from '../../utils/shop'
import type { Coordinates, OrderType, Shop } from '../../types/shop'

const fallbackCenter: Coordinates = { longitude: 116.397428, latitude: 39.90923 }
const shop = ref<Shop | null>(null)
const loading = ref(true)
const routeDistance = ref<number | null>(null)
const currentShopId = ref<number | undefined>(getCurrentShop()?.id)

const isCurrentShop = computed(() => currentShopId.value === shop.value?.id)
const distanceText = computed(() => formatDistance(shop.value?.distance))
const statusClass = computed(() => {
  if (shop.value?.instantAvailable) return 'open'
  if (shop.value?.status === 2) return 'paused'
  return 'resting'
})
const mapCenter = computed<Coordinates>(() => {
  const longitude = Number(shop.value?.longitude)
  const latitude = Number(shop.value?.latitude)
  if (Number.isFinite(longitude) && Number.isFinite(latitude) && !(longitude === 0 && latitude === 0)) {
    return { longitude, latitude }
  }
  return fallbackCenter
})

onLoad(async (options?: Record<string, unknown>) => {
  const shopId = Number(options?.id)
  const distance = Number(options?.distance)
  routeDistance.value = Number.isFinite(distance) ? distance : null
  if (!shopId) {
    loading.value = false
    return
  }

  const selected = getCurrentShop()
  if (selected?.id === shopId) shop.value = selected
  try {
    const detail = await getShopDetail(shopId)
    shop.value = {
      ...(shop.value || detail),
      ...detail,
      distance: routeDistance.value ?? shop.value?.distance ?? detail.distance
    }
  } finally {
    loading.value = false
  }
})

/** 选定当前门店：保存为常用门店并提示。 */
function selectShop() {
  if (!shop.value) return
  saveCurrentShop(shop.value)
  currentShopId.value = shop.value.id
  uni.showToast({ title: '已选择此门店', icon: 'success' })
}

/** 开始点单：保存门店后跳菜单页；门店不支持即时单时强制改预订单。 */
function startOrder(orderType: OrderType) {
  if (!shop.value) return
  if (orderType === 'NORMAL' && !shop.value.instantAvailable) {
    orderType = 'PREORDER'
  }
  saveCurrentShop(shop.value)
  currentShopId.value = shop.value.id
  uni.navigateTo({ url: `/pages/menu/index?orderType=${orderType}` })
}

function startDelivery() {
  if (!shop.value) return
  saveCurrentShop(shop.value)
  currentShopId.value = shop.value.id
  uni.showToast({ title: '外卖功能即将开放', icon: 'none' })
}

function openLocation() {
  if (!shop.value) return
  const longitude = Number(shop.value.longitude)
  const latitude = Number(shop.value.latitude)
  if (!Number.isFinite(longitude) || !Number.isFinite(latitude)) {
    uni.showToast({ title: '门店暂未配置经纬度', icon: 'none' })
    return
  }
  uni.openLocation({
    longitude,
    latitude,
    name: shop.value.name,
    address: shop.value.address
  })
}

function callShop() {
  if (!shop.value?.phone) return
  uni.makePhoneCall({ phoneNumber: shop.value.phone })
}

function goBack() {
  uni.switchTab({ url: '/pages/index/index' })
}

function money(cents: number): string {
  const value = (cents / 100).toFixed(2)
  return value.endsWith('.00') ? value.slice(0, -3) : value
}
</script>

<style lang="scss" scoped>
.detail-page {
  display: flex;
  height: 100vh;
  overflow: hidden;
  flex-direction: column;
  background: #f4f1eb;
}

.top-bar {
  display: flex;
  position: relative;
  z-index: 6;
  height: calc(var(--status-bar-height) + 92rpx);
  flex-shrink: 0;
  align-items: flex-end;
  justify-content: space-between;
  padding: var(--status-bar-height) 26rpx 17rpx;
  background: rgba(244, 241, 235, 0.97);
  box-sizing: border-box;
}

.back-button,
.top-placeholder {
  width: 58rpx;
  height: 58rpx;
}

.back-button {
  border-radius: 20rpx;
  background: #fff;
  color: #312d27;
  font-size: 48rpx;
  line-height: 50rpx;
  text-align: center;
}

.top-title {
  color: #28251f;
  font-size: 28rpx;
  font-weight: 800;
  line-height: 58rpx;
}

.detail-scroll {
  min-height: 0;
  flex: 1;
}

.detail-content {
  padding: 0 24rpx;
}

.hero {
  position: relative;
  height: 430rpx;
  overflow: hidden;
  border-radius: 38rpx;
  background: linear-gradient(145deg, #d6b17e, #9e6b37);
  background-position: center;
  background-size: cover;
  box-shadow: 0 20rpx 55rpx rgba(64, 48, 29, 0.17);
}

.hero-fallback {
  display: flex;
  position: absolute;
  inset: 0;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  background:
    radial-gradient(circle at 75% 24%, rgba(255, 229, 181, 0.72), transparent 22%),
    linear-gradient(145deg, #d6b17e, #9e6b37);
}

.hero-logo-card {
  display: flex;
  width: 235rpx;
  height: 260rpx;
  align-items: center;
  justify-content: center;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, 0.94);
  box-shadow: 0 18rpx 45rpx rgba(94, 55, 19, 0.18);
}

.hero-logo {
  width: 205rpx;
  height: 225rpx;
}

.hero-shade {
  position: absolute;
  inset: 0;
  background: linear-gradient(180deg, rgba(20, 18, 15, 0.04), rgba(20, 18, 15, 0.8));
}

.hero-copy {
  position: absolute;
  right: 28rpx;
  bottom: 28rpx;
  left: 28rpx;
  z-index: 2;
}

.hero-status {
  display: inline-flex;
  align-items: center;
  padding: 8rpx 14rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, 0.88);
  color: #665e54;
  font-size: 18rpx;
  font-weight: 700;
}

.hero-status.open {
  color: #34774d;
}

.hero-status.paused {
  color: #9b692e;
}

.status-dot {
  width: 10rpx;
  height: 10rpx;
  margin-right: 8rpx;
  border-radius: 50%;
  background: currentColor;
}

.shop-name {
  display: block;
  margin-top: 15rpx;
  color: #fff;
  font-size: 42rpx;
  font-weight: 850;
}

.shop-address {
  display: block;
  overflow: hidden;
  margin-top: 9rpx;
  color: rgba(255, 255, 255, 0.74);
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-card {
  display: grid;
  position: relative;
  z-index: 3;
  grid-template-columns: 1fr 1rpx 1fr 1rpx 1fr;
  align-items: center;
  margin: -22rpx 20rpx 0;
  padding: 26rpx 18rpx;
  border-radius: 28rpx;
  background: #fff;
  box-shadow: 0 14rpx 42rpx rgba(44, 38, 29, 0.09);
}

.summary-item {
  min-width: 0;
  text-align: center;
}

.summary-value,
.summary-label {
  display: block;
}

.summary-value {
  overflow: hidden;
  color: #302c25;
  font-size: 23rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.summary-label {
  margin-top: 7rpx;
  color: #a29a90;
  font-size: 17rpx;
}

.summary-divider {
  width: 1rpx;
  height: 42rpx;
  background: #ece7df;
}

.service-tip {
  display: flex;
  align-items: center;
  margin-top: 24rpx;
  padding: 22rpx 24rpx;
  border-radius: 26rpx;
  background: #f1e4d1;
}

.tip-icon {
  display: flex;
  width: 46rpx;
  height: 46rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin-right: 17rpx;
  border-radius: 50%;
  background: #9d6b36;
  color: #fff;
  font-size: 24rpx;
  font-weight: 800;
}

.tip-title,
.tip-copy {
  display: block;
}

.tip-title {
  color: #694a2c;
  font-size: 22rpx;
  font-weight: 800;
}

.tip-copy {
  margin-top: 5rpx;
  color: #977b5d;
  font-size: 18rpx;
}

.info-card,
.notice-card {
  margin-top: 24rpx;
  padding: 28rpx;
  border-radius: 32rpx;
  background: #fff;
}

.section-heading,
.notice-heading,
.info-row {
  display: flex;
  align-items: center;
}

.section-heading,
.notice-heading {
  justify-content: space-between;
}

.section-kicker,
.section-title {
  display: block;
}

.section-kicker {
  color: #ae7a42;
  font-size: 15rpx;
  font-weight: 800;
  letter-spacing: 3rpx;
}

.section-title {
  margin-top: 7rpx;
  color: #2c2923;
  font-size: 29rpx;
  font-weight: 850;
}

.info-action,
.distance-chip {
  padding: 9rpx 15rpx;
  border-radius: 19rpx;
  background: #f2ece3;
  color: #8b643b;
  font-size: 18rpx;
  font-weight: 700;
}

.info-row {
  position: relative;
  margin-top: 25rpx;
  padding-top: 25rpx;
  border-top: 1rpx solid #eee9e2;
}

.info-icon {
  display: flex;
  position: relative;
  width: 56rpx;
  height: 56rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 19rpx;
  background: #f4eee6;
  color: #90653a;
}

.clock-icon {
  border: 4rpx solid #90653a;
  border-radius: 50%;
  background: transparent;
  box-sizing: border-box;
}

.clock-hand {
  position: absolute;
  left: 24rpx;
  top: 24rpx;
  width: 3rpx;
  border-radius: 3rpx;
  background: #90653a;
  transform-origin: 2rpx 2rpx;
}

.clock-hand.hour {
  height: 15rpx;
  transform: rotate(180deg);
}

.clock-hand.minute {
  height: 19rpx;
  transform: rotate(120deg);
}

.location-dot {
  width: 14rpx;
  height: 14rpx;
  border: 6rpx solid #90653a;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
}

.phone-icon {
  font-size: 25rpx;
}

.info-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}

.info-label,
.info-value {
  display: block;
}

.info-label {
  color: #a19a90;
  font-size: 17rpx;
}

.info-value {
  margin-top: 6rpx;
  color: #423d35;
  font-size: 21rpx;
  line-height: 1.45;
}

.row-arrow {
  margin-left: 14rpx;
  color: #aaa196;
  font-size: 32rpx;
}

.notice-card {
  position: relative;
  overflow: hidden;
  background: linear-gradient(135deg, #20201c, #3a332a);
}

.notice-label {
  color: #e8b877;
  font-size: 22rpx;
  font-weight: 800;
}

.notice-mark {
  color: rgba(255, 255, 255, 0.12);
  font-size: 45rpx;
  font-weight: 900;
}

.notice-copy {
  display: block;
  margin-top: 18rpx;
  color: rgba(255, 255, 255, 0.78);
  font-size: 21rpx;
  line-height: 1.75;
}

.map-section {
  margin-top: 34rpx;
}

.map-heading {
  padding: 0 5rpx;
}

.map-card {
  height: 330rpx;
  overflow: hidden;
  margin-top: 20rpx;
  border: 5rpx solid #fff;
  border-radius: 32rpx;
  background: #e9e3d9;
  box-shadow: 0 14rpx 40rpx rgba(44, 38, 29, 0.08);
  box-sizing: border-box;
}

.bottom-space {
  height: 165rpx;
}

.bottom-actions {
  display: grid;
  position: relative;
  z-index: 8;
  grid-template-columns: auto 1fr 1.15fr;
  gap: 12rpx;
  flex-shrink: 0;
  padding: 18rpx 24rpx calc(18rpx + env(safe-area-inset-bottom));
  border-top: 1rpx solid #ece6dd;
  background: rgba(255, 255, 255, 0.98);
}

.select-action,
.preorder-action,
.order-action {
  height: 82rpx;
  border-radius: 41rpx;
  font-size: 22rpx;
  font-weight: 800;
  line-height: 82rpx;
  text-align: center;
}

.select-action {
  padding: 0 20rpx;
  background: #f1ede7;
  color: #776c5f;
}

.select-action.selected {
  color: #a06d35;
}

.preorder-action {
  margin: 0;
  background: #ead9c1;
  color: #58432e;
}

.order-action {
  margin: 0;
  background: #1c1b17;
  color: #fff;
}

.order-action.disabled {
  background: #9f978c;
}

.loading-state,
.empty-state {
  display: flex;
  min-height: 70vh;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  color: #968e83;
  font-size: 21rpx;
}

.loading-ring {
  width: 52rpx;
  height: 52rpx;
  margin-bottom: 22rpx;
  border: 5rpx solid #e6ded3;
  border-top-color: #9f6e3c;
  border-radius: 50%;
  animation: rotate 0.8s linear infinite;
}

.empty-mark {
  width: 92rpx;
  height: 92rpx;
  border-radius: 30rpx;
  background: #1c1b17;
  color: #e8ba7c;
  font-size: 24rpx;
  font-weight: 900;
  line-height: 92rpx;
  text-align: center;
}

.empty-title {
  margin-top: 24rpx;
  color: #352f28;
  font-size: 27rpx;
  font-weight: 800;
}

.empty-copy {
  margin-top: 9rpx;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}
</style>
