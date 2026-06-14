<template>
  <view class="safe-page home">
    <view class="header">
      <view class="brand-row">
        <view>
          <text class="brand">GOGORDER</text>
          <text class="welcome">今天想喝点什么？</text>
        </view>
        <view class="avatar" @click="openProfile">{{ avatarText }}</view>
      </view>

      <view class="location" @click="selectShop">
        <view class="location-mark"><view class="location-dot" /></view>
        <view class="location-copy">
          <view class="location-title-row">
            <text class="location-title">{{ currentShop.name }}</text>
            <text class="location-arrow">›</text>
          </view>
          <text class="location-address">{{ currentShop.address }}</text>
        </view>
        <view class="distance">
          <text class="distance-value">{{ currentShop.distanceText }}</text>
          <text class="distance-label">距你</text>
        </view>
      </view>
    </view>

    <scroll-view class="home-scroll" scroll-y :show-scrollbar="false">
      <view class="home-content">
    <view class="banner">
      <view class="banner-copy">
        <text class="banner-tag">夏日清爽计划</text>
        <text class="banner-title">一口鲜果，<br />刚好解暑</text>
        <view class="banner-action" @click="startOrder('立即点单')">
          <text>去看看</text>
          <text class="banner-arrow">→</text>
        </view>
      </view>
      <view class="drink-art">
        <view class="fruit fruit-one" />
        <view class="fruit fruit-two" />
        <view class="cup">
          <view class="cup-lid" />
          <view class="cup-body">
            <text>GO</text>
          </view>
        </view>
      </view>
      <text class="banner-number">01</text>
    </view>

    <view class="order-grid">
      <view class="order-entry pickup-entry" @click="startOrder('立即点单')">
        <view class="entry-icon pickup-icon">
          <view class="bag-handle" />
          <view class="bag-body">GO</view>
        </view>
        <view class="entry-copy">
          <text class="entry-title">立即点单</text>
          <text class="entry-subtitle">到店自取 · 无需排队</text>
        </view>
        <text class="entry-arrow">›</text>
      </view>

      <view class="order-entry delivery-entry" @click="startDelivery">
        <view class="entry-icon delivery-icon">送</view>
        <view class="entry-copy">
          <text class="entry-title">外卖配送</text>
          <text class="entry-subtitle">送到身边 · 轻松享用</text>
        </view>
        <text class="entry-arrow">›</text>
      </view>
    </view>

    <view class="shop-card" @click="selectShop">
      <view class="shop-card-top">
        <view>
          <view class="shop-name-row">
            <text class="shop-name">{{ currentShop.name }}</text>
            <text
              class="shop-status"
              :class="{ pending: !currentShop.id, unavailable: currentShop.id && !currentShop.instantAvailable }"
            >
              {{ currentShop.statusName }}
            </text>
          </view>
          <text class="shop-time">营业时间 {{ currentShop.businessHours }}</text>
        </view>
        <view class="switch-shop">切换门店</view>
      </view>
      <view class="shop-notice">
        <text class="notice-label">店内公告</text>
        <text class="notice-copy">{{ currentShop.notice }}</text>
      </view>
    </view>

    <view class="section">
      <view class="section-heading">
        <view>
          <text class="section-title">本周人气</text>
          <text class="section-subtitle">大家最近都在喝</text>
        </view>
        <text class="section-more" @click="startOrder('全部商品')">全部 ›</text>
      </view>

      <scroll-view class="product-scroll" scroll-x :show-scrollbar="false">
        <view class="product-list">
          <view v-for="product in popularProducts" :key="product.name" class="product-card" @click="startOrder(product.name)">
            <view class="product-cover" :style="{ background: product.background }">
              <view class="mini-cup">
                <view class="mini-lid" />
                <view class="mini-body" :style="{ background: product.color }"><text>GO</text></view>
              </view>
              <text class="product-badge">{{ product.badge }}</text>
            </view>
            <text class="product-name">{{ product.name }}</text>
            <text class="product-desc">{{ product.description }}</text>
            <view class="product-bottom">
              <text class="product-price"><text>¥</text>{{ product.price }}</text>
              <view class="add-button">+</view>
            </view>
          </view>
        </view>
      </scroll-view>
    </view>

    <view class="benefit-card" @click="showDeveloping('会员权益')">
      <view>
        <text class="benefit-kicker">GOGORDER MEMBER</text>
        <text class="benefit-title">每一杯，都有小惊喜</text>
        <text class="benefit-copy">登录会员，查看你的专属权益</text>
      </view>
      <view class="benefit-action">查看权益 ›</view>
    </view>
      </view>
    </scroll-view>
  </view>
</template>

<script setup lang="ts">
import { computed, ref } from 'vue'
import { onShow } from '@dcloudio/uni-app'
import { getUserInfo } from '../../api/auth'
import { getProducts } from '../../api/product'
import { getShopDetail } from '../../api/shop'
import { getToken, getUser, saveUser } from '../../utils/auth'
import { businessHours, formatDistance, getCurrentShop, saveCurrentShop } from '../../utils/shop'
import type { UserInfo } from '../../types/auth'
import type { PopularProductCard } from '../../types/product'
import type { HomeShopView, Shop } from '../../types/shop'

const user = ref<UserInfo | null>(getUser())
const avatarText = computed(() => (user.value?.nickname || 'GO').slice(0, 1))

const currentShop = ref<HomeShopView>(normalizeShop(getCurrentShop()))

const defaultPopularProducts: PopularProductCard[] = [
  { name: '鲜橙茉莉', description: '清甜鲜果茶', price: '16', badge: 'TOP 1', color: '#ef9f49', background: 'linear-gradient(145deg, #fff1d9, #f7d9a9)' },
  { name: '生椰拿铁', description: '经典人气款', price: '18', badge: '热卖', color: '#b98757', background: 'linear-gradient(145deg, #efe6db, #dbc8b1)' },
  { name: '青提冰茶', description: '清爽低负担', price: '17', badge: '新品', color: '#a8bf72', background: 'linear-gradient(145deg, #eef4dc, #d9e3b8)' }
]
const popularProducts = ref<PopularProductCard[]>(defaultPopularProducts)

onShow(async () => {
  if (!getToken()) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  try {
    user.value = await getUserInfo()
    saveUser(user.value)
  } catch (error) {
    user.value = getUser() || user.value
  }
  await refreshCurrentShop()
})

function selectShop() {
  uni.navigateTo({ url: '/pages/shop/select' })
}

function startOrder(label: string) {
  if (!currentShop.value.id) {
    selectShop()
    return
  }
  const orderType = !currentShop.value.instantAvailable ? 'PREORDER' : 'NORMAL'
  uni.navigateTo({ url: `/pages/menu/index?orderType=${orderType}` })
}

function startDelivery() {
  if (!currentShop.value.id) {
    selectShop()
    return
  }
  uni.showToast({ title: '外卖功能即将开放', icon: 'none' })
}

function showDeveloping(name: string) {
  uni.showToast({ title: `${name}功能即将开放`, icon: 'none' })
}

function openProfile() {
  uni.switchTab({ url: '/pages/profile/index' })
}

async function refreshCurrentShop() {
  const selected = getCurrentShop()
  if (!selected?.id) {
    currentShop.value = normalizeShop(null)
    popularProducts.value = defaultPopularProducts
    return
  }
  currentShop.value = normalizeShop(selected)
  try {
    const detail = await getShopDetail(selected.id)
    const refreshed = { ...selected, ...detail }
    saveCurrentShop(refreshed)
    currentShop.value = normalizeShop(refreshed)
    await refreshPopularProducts(refreshed.id)
  } catch (error) {
    currentShop.value = normalizeShop(selected)
  }
}

async function refreshPopularProducts(shopId: number) {
  try {
    const products = await getProducts({ shopId })
    const styles = defaultPopularProducts
    popularProducts.value = products.slice(0, 3).map((product, index) => ({
      ...product,
      description: product.description || '现点现做，新鲜好喝',
      price: money(product.displayPrice),
      badge: index === 0 ? 'TOP 1' : (product.tags?.[0] || '推荐'),
      color: styles[index]?.color || '#c7833f',
      background: styles[index]?.background || 'linear-gradient(145deg, #efe6db, #dbc8b1)'
    }))
  } catch (error) {
    popularProducts.value = defaultPopularProducts
  }
}

function money(cents: number | null | undefined): string {
  const value = (Number(cents || 0) / 100).toFixed(2)
  return value.endsWith('.00') ? value.slice(0, -3) : value
}

function normalizeShop(shop: Shop | null): HomeShopView {
  if (!shop?.id) {
    return {
      id: null,
      name: '请选择附近门店',
      address: '定位后为你推荐最近门店',
      distanceText: '--',
      businessHours: '--',
      statusName: '待选择',
      instantAvailable: false,
      notice: '选择门店后查看店内公告'
    }
  }
  return {
    ...shop,
    distanceText: formatDistance(shop.distance),
    businessHours: businessHours(shop),
    notice: shop.notice || '门店饮品均为现点现做，请及时取餐'
  }
}
</script>

<style lang="scss" scoped>
.home {
  display: flex;
  height: 100vh;
  overflow: hidden;
  flex-direction: column;
  background: #f5f3ee;
  box-sizing: border-box;
}

.header {
  position: relative;
  z-index: 5;
  flex-shrink: 0;
  padding: calc(var(--status-bar-height) + 24rpx) 28rpx 22rpx;
  background: rgba(245, 243, 238, 0.98);
  box-shadow: 0 10rpx 32rpx rgba(44, 38, 29, 0.06);
}

.home-scroll {
  min-height: 0;
  flex: 1;
}

.home-content {
  padding: 24rpx 28rpx calc(72rpx + env(safe-area-inset-bottom));
}

.brand-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.brand {
  display: block;
  color: #b17d42;
  font-size: 20rpx;
  font-weight: 800;
  letter-spacing: 5rpx;
}

.welcome {
  display: block;
  margin-top: 7rpx;
  color: #181711;
  font-size: 37rpx;
  font-weight: 800;
}

.avatar {
  display: flex;
  width: 76rpx;
  height: 76rpx;
  align-items: center;
  justify-content: center;
  border: 5rpx solid #fff;
  border-radius: 50%;
  background: #191813;
  box-shadow: 0 8rpx 24rpx rgba(35, 31, 24, 0.12);
  color: #e8ba7c;
  font-size: 24rpx;
  font-weight: 800;
}

.location {
  display: flex;
  align-items: center;
  margin-top: 27rpx;
  padding: 21rpx 22rpx;
  border-radius: 26rpx;
  background: #fff;
  box-shadow: 0 12rpx 38rpx rgba(44, 38, 29, 0.06);
}

.location-mark {
  display: flex;
  width: 54rpx;
  height: 54rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  background: #f2e6d5;
}

.location-dot {
  width: 14rpx;
  height: 14rpx;
  border: 7rpx solid #a66d35;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
}

.location-copy {
  min-width: 0;
  flex: 1;
  margin-left: 18rpx;
}

.location-title-row,
.shop-name-row {
  display: flex;
  align-items: center;
}

.location-title {
  overflow: hidden;
  max-width: 390rpx;
  color: #25231e;
  font-size: 27rpx;
  font-weight: 750;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.location-arrow {
  margin-left: 8rpx;
  color: #9d9589;
  font-size: 34rpx;
  line-height: 1;
}

.location-address {
  display: block;
  overflow: hidden;
  margin-top: 5rpx;
  color: #9a9388;
  font-size: 21rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.distance {
  padding-left: 18rpx;
  border-left: 1rpx solid #eee9e0;
  text-align: center;
}

.distance-value,
.distance-label {
  display: block;
}

.distance-value {
  color: #39352e;
  font-size: 22rpx;
  font-weight: 700;
}

.distance-label {
  margin-top: 4rpx;
  color: #a49d93;
  font-size: 18rpx;
}

.banner {
  position: relative;
  overflow: hidden;
  height: 330rpx;
  padding: 38rpx 36rpx;
  border-radius: 38rpx;
  background: linear-gradient(145deg, #ce692d, #e4974a);
  box-shadow: 0 20rpx 52rpx rgba(167, 93, 39, 0.2);
  box-sizing: border-box;
  color: #fff;
}

.banner-copy {
  position: relative;
  z-index: 3;
}

.banner-tag {
  display: inline-block;
  padding: 7rpx 15rpx;
  border: 1rpx solid rgba(255, 255, 255, 0.48);
  border-radius: 24rpx;
  font-size: 18rpx;
  letter-spacing: 2rpx;
}

.banner-title {
  display: block;
  margin-top: 18rpx;
  font-size: 46rpx;
  font-weight: 850;
  line-height: 1.2;
}

.banner-action {
  display: flex;
  width: 120rpx;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding: 10rpx 17rpx;
  border-radius: 26rpx;
  background: #fff;
  color: #9d4d24;
  font-size: 20rpx;
  font-weight: 700;
}

.banner-arrow {
  font-size: 24rpx;
}

.drink-art {
  position: absolute;
  right: 34rpx;
  bottom: -26rpx;
  width: 250rpx;
  height: 300rpx;
}

.cup {
  position: absolute;
  right: 20rpx;
  bottom: 0;
  z-index: 2;
  width: 158rpx;
  height: 238rpx;
  transform: rotate(7deg);
}

.cup-lid {
  position: absolute;
  top: 0;
  left: 3rpx;
  z-index: 2;
  width: 152rpx;
  height: 26rpx;
  border-radius: 15rpx;
  background: #2a241e;
}

.cup-body {
  display: flex;
  position: absolute;
  top: 18rpx;
  left: 15rpx;
  width: 128rpx;
  height: 210rpx;
  align-items: center;
  justify-content: center;
  border-radius: 12rpx 12rpx 42rpx 42rpx;
  background: rgba(255, 246, 222, 0.9);
  box-shadow: inset 0 0 0 5rpx rgba(255, 255, 255, 0.28);
  color: #b65629;
  font-size: 34rpx;
  font-weight: 900;
}

.fruit {
  position: absolute;
  border: 10rpx solid rgba(255, 221, 125, 0.9);
  border-radius: 50%;
  background: #f7b746;
  box-shadow: inset 0 0 0 8rpx rgba(255, 255, 255, 0.23);
}

.fruit-one {
  top: 62rpx;
  left: 20rpx;
  width: 76rpx;
  height: 76rpx;
}

.fruit-two {
  right: -32rpx;
  bottom: 70rpx;
  width: 96rpx;
  height: 96rpx;
}

.banner-number {
  position: absolute;
  right: 18rpx;
  top: -35rpx;
  color: rgba(255, 255, 255, 0.08);
  font-size: 210rpx;
  font-weight: 900;
}

.order-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 18rpx;
  margin-top: 22rpx;
}

.order-entry {
  position: relative;
  overflow: hidden;
  min-height: 220rpx;
  padding: 27rpx 25rpx;
  border-radius: 32rpx;
  box-sizing: border-box;
}

.pickup-entry {
  background: #1c1b17;
  color: #fff;
}

.delivery-entry {
  background: #e9d8bf;
  color: #30291f;
}

.entry-icon {
  position: relative;
  width: 65rpx;
  height: 65rpx;
}

.pickup-icon {
  margin-top: 3rpx;
}

.bag-body {
  display: flex;
  position: absolute;
  bottom: 0;
  left: 5rpx;
  width: 55rpx;
  height: 48rpx;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx 8rpx 14rpx 14rpx;
  background: #e7b675;
  color: #1c1b17;
  font-size: 16rpx;
  font-weight: 900;
}

.bag-handle {
  position: absolute;
  top: 0;
  left: 19rpx;
  width: 28rpx;
  height: 26rpx;
  border: 5rpx solid #e7b675;
  border-bottom: none;
  border-radius: 18rpx 18rpx 0 0;
}

.delivery-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 21rpx;
  background: #704f2d;
  color: #f3dfc2;
  font-size: 26rpx;
  font-weight: 900;
}

.entry-copy {
  position: absolute;
  left: 25rpx;
  bottom: 25rpx;
}

.entry-title,
.entry-subtitle {
  display: block;
}

.entry-title {
  font-size: 30rpx;
  font-weight: 800;
}

.entry-subtitle {
  margin-top: 7rpx;
  color: #a9a49a;
  font-size: 19rpx;
}

.delivery-entry .entry-subtitle {
  color: #89745c;
}

.entry-arrow {
  position: absolute;
  top: 24rpx;
  right: 23rpx;
  font-size: 36rpx;
  opacity: 0.65;
}

.shop-card {
  margin-top: 22rpx;
  padding: 27rpx 26rpx 22rpx;
  border-radius: 31rpx;
  background: #fff;
  box-shadow: 0 12rpx 38rpx rgba(44, 38, 29, 0.05);
}

.shop-card-top {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
}

.shop-name {
  color: #292620;
  font-size: 27rpx;
  font-weight: 750;
}

.shop-status {
  margin-left: 12rpx;
  padding: 5rpx 11rpx;
  border-radius: 18rpx;
  background: #e5f1e8;
  color: #398054;
  font-size: 18rpx;
}

.shop-status.pending {
  background: #f1ede6;
  color: #8d8173;
}

.shop-status.unavailable {
  background: #f6ead7;
  color: #a87336;
}

.shop-time {
  display: block;
  margin-top: 10rpx;
  color: #989187;
  font-size: 21rpx;
}

.switch-shop {
  padding: 10rpx 17rpx;
  border-radius: 24rpx;
  background: #f1ede6;
  color: #6d6255;
  font-size: 20rpx;
}

.shop-notice {
  display: flex;
  align-items: center;
  margin-top: 23rpx;
  padding-top: 20rpx;
  border-top: 1rpx solid #eeeae3;
}

.notice-label {
  flex-shrink: 0;
  padding: 5rpx 10rpx;
  border-radius: 8rpx;
  background: #f4e6d5;
  color: #9c6534;
  font-size: 17rpx;
}

.notice-copy {
  overflow: hidden;
  margin-left: 13rpx;
  color: #8e877d;
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.section {
  margin-top: 43rpx;
}

.section-heading {
  display: flex;
  align-items: flex-end;
  justify-content: space-between;
  padding: 0 5rpx;
}

.section-title,
.section-subtitle {
  display: block;
}

.section-title {
  color: #22201c;
  font-size: 32rpx;
  font-weight: 800;
}

.section-subtitle {
  margin-top: 7rpx;
  color: #9a9388;
  font-size: 20rpx;
}

.section-more {
  padding: 10rpx 0;
  color: #8c8275;
  font-size: 22rpx;
}

.product-scroll {
  width: calc(100% + 28rpx);
  margin-top: 23rpx;
}

.product-list {
  display: flex;
  gap: 18rpx;
  padding-right: 28rpx;
}

.product-card {
  width: 245rpx;
  flex-shrink: 0;
  padding: 12rpx 12rpx 18rpx;
  border-radius: 28rpx;
  background: #fff;
  box-sizing: border-box;
}

.product-cover {
  position: relative;
  height: 214rpx;
  overflow: hidden;
  border-radius: 22rpx;
}

.product-badge {
  position: absolute;
  top: 12rpx;
  left: 12rpx;
  padding: 5rpx 9rpx;
  border-radius: 12rpx;
  background: rgba(255, 255, 255, 0.82);
  color: #6e5943;
  font-size: 15rpx;
  font-weight: 700;
}

.mini-cup {
  position: absolute;
  right: 42rpx;
  bottom: 15rpx;
  width: 112rpx;
  height: 158rpx;
  transform: rotate(5deg);
}

.mini-lid {
  position: absolute;
  top: 0;
  z-index: 2;
  width: 112rpx;
  height: 18rpx;
  border-radius: 11rpx;
  background: #302c26;
}

.mini-body {
  display: flex;
  position: absolute;
  top: 12rpx;
  left: 9rpx;
  width: 94rpx;
  height: 140rpx;
  align-items: center;
  justify-content: center;
  border-radius: 8rpx 8rpx 30rpx 30rpx;
  box-shadow: inset 0 0 0 5rpx rgba(255, 255, 255, 0.24);
  color: rgba(255, 255, 255, 0.88);
  font-size: 22rpx;
  font-weight: 900;
}

.product-name {
  display: block;
  margin-top: 17rpx;
  color: #2c2924;
  font-size: 25rpx;
  font-weight: 750;
}

.product-desc {
  display: block;
  margin-top: 5rpx;
  color: #a09a91;
  font-size: 18rpx;
}

.product-bottom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16rpx;
}

.product-price {
  color: #2d2923;
  font-size: 29rpx;
  font-weight: 800;
}

.product-price text {
  margin-right: 2rpx;
  font-size: 17rpx;
}

.add-button {
  width: 40rpx;
  height: 40rpx;
  border-radius: 50%;
  background: #1c1b17;
  color: #fff;
  font-size: 27rpx;
  line-height: 37rpx;
  text-align: center;
}

.benefit-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 43rpx;
  padding: 31rpx 27rpx;
  border-radius: 30rpx;
  background: linear-gradient(135deg, #e7d2b3, #f0e5d4);
}

.benefit-kicker,
.benefit-title,
.benefit-copy {
  display: block;
}

.benefit-kicker {
  color: #9a6c37;
  font-size: 16rpx;
  font-weight: 800;
  letter-spacing: 2rpx;
}

.benefit-title {
  margin-top: 9rpx;
  color: #332b22;
  font-size: 27rpx;
  font-weight: 800;
}

.benefit-copy {
  margin-top: 7rpx;
  color: #897763;
  font-size: 19rpx;
}

.benefit-action {
  flex-shrink: 0;
  margin-left: 15rpx;
  padding: 11rpx 16rpx;
  border-radius: 23rpx;
  background: #352d23;
  color: #f1dfc3;
  font-size: 19rpx;
}
</style>
