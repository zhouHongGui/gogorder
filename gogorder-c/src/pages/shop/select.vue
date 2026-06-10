<template>
  <view class="safe-page shop-page">
    <view class="shop-header">
      <view class="header-copy">
        <text class="eyebrow">SELECT A STORE</text>
        <text class="title">选择门店</text>
        <text class="subtitle">选择取餐门店后，即可浏览对应菜单</text>
      </view>
      <view class="locate-button" :class="{ loading: locating }" @click="locate">
        <view class="locate-dot" />
        <text>{{ locating ? '定位中' : '重新定位' }}</text>
      </view>
    </view>

    <view class="search-box">
      <view class="search-icon" />
      <input
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索门店名称或地址"
        @confirm="loadShops"
      />
      <text v-if="keyword" class="clear" @click="clearKeyword">×</text>
    </view>

    <view class="list-heading">
      <view>
        <text class="list-title">{{ location ? '附近门店' : '全部门店' }}</text>
        <text class="list-caption">{{ listCaption }}</text>
      </view>
      <text class="shop-count">{{ shops.length }} 家</text>
    </view>

    <view v-if="loading" class="state-card">
      <view class="loading-ring" />
      <text>正在查找门店...</text>
    </view>

    <view v-else-if="!shops.length" class="state-card">
      <view class="empty-mark">GO</view>
      <text class="state-title">没有找到门店</text>
      <text class="state-copy">换个关键词试试，或重新获取位置。</text>
    </view>

    <view v-else class="shop-list">
      <view
        v-for="shop in shops"
        :key="shop.id"
        class="shop-item"
        :class="{ selected: currentShopId === shop.id }"
        @click="chooseShop(shop)"
      >
        <view class="shop-cover" :style="shop.image ? { backgroundImage: `url(${shop.image})` } : {}">
          <text v-if="!shop.image">GO</text>
        </view>

        <view class="shop-main">
          <view class="shop-title-row">
            <text class="shop-name">{{ shop.name }}</text>
            <text class="status" :class="statusClass(shop)">{{ shop.statusName }}</text>
          </view>
          <text class="shop-address">{{ shop.address || '暂未填写门店地址' }}</text>
          <view class="shop-meta">
            <text>营业时间 {{ businessHours(shop) }}</text>
            <text v-if="shop.distance !== null && shop.distance !== undefined">{{ formatDistance(shop.distance) }}</text>
          </view>
          <view v-if="!shop.instantAvailable" class="preorder-tip">
            当前仅支持预约取餐
          </view>
        </view>

        <view class="choose-mark">
          <text v-if="currentShopId === shop.id">✓</text>
          <text v-else>›</text>
        </view>
      </view>
    </view>
  </view>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { getNearbyShops } from '../../api/shop'
import { businessHours, formatDistance, getCurrentShop, getLastLocation, saveCurrentShop, saveLastLocation } from '../../utils/shop'

const shops = ref([])
const keyword = ref('')
const location = ref(getLastLocation())
const loading = ref(false)
const locating = ref(false)
const currentShopId = ref(getCurrentShop()?.id)
let searchTimer

const listCaption = computed(() => {
  if (keyword.value) return `“${keyword.value}”的搜索结果`
  return location.value ? '已按距离由近到远排序' : '未获取位置，按门店默认顺序展示'
})

watch(keyword, () => {
  clearTimeout(searchTimer)
  searchTimer = setTimeout(loadShops, 350)
})

onLoad(() => {
  if (location.value) loadShops()
  else locate()
})

function locate() {
  if (locating.value) return
  locating.value = true
  uni.getLocation({
    type: 'gcj02',
    success(result) {
      location.value = { longitude: result.longitude, latitude: result.latitude }
      saveLastLocation(location.value)
      loadShops()
    },
    fail() {
      uni.showToast({ title: '未获取位置，将展示全部门店', icon: 'none' })
      loadShops()
    },
    complete() {
      locating.value = false
    }
  })
}

async function loadShops() {
  loading.value = true
  try {
    shops.value = await getNearbyShops({
      ...(location.value || {}),
      keyword: keyword.value.trim()
    })
  } catch (error) {
    shops.value = []
  } finally {
    loading.value = false
  }
}

function clearKeyword() {
  keyword.value = ''
}

function chooseShop(shop) {
  saveCurrentShop(shop)
  currentShopId.value = shop.id
  uni.showToast({ title: '门店已切换', icon: 'success' })
  setTimeout(() => {
    const pages = getCurrentPages()
    if (pages.length > 1) uni.navigateBack()
    else uni.switchTab({ url: '/pages/index/index' })
  }, 350)
}

function statusClass(shop) {
  if (shop.instantAvailable) return 'open'
  if (shop.status === 2) return 'paused'
  return 'resting'
}
</script>

<style lang="scss" scoped>
.shop-page {
  padding: calc(var(--status-bar-height) + 24rpx) 28rpx 70rpx;
  background: #f5f3ee;
}

.shop-header,
.list-heading,
.shop-title-row,
.shop-meta {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.shop-header {
  align-items: flex-end;
}

.eyebrow,
.title,
.subtitle {
  display: block;
}

.eyebrow {
  color: #ad7b43;
  font-size: 18rpx;
  font-weight: 800;
  letter-spacing: 4rpx;
}

.title {
  margin-top: 8rpx;
  color: #1d1c18;
  font-size: 46rpx;
  font-weight: 850;
}

.subtitle {
  margin-top: 8rpx;
  color: #928b80;
  font-size: 21rpx;
}

.locate-button {
  display: flex;
  align-items: center;
  padding: 13rpx 17rpx;
  border-radius: 25rpx;
  background: #fff;
  color: #655c50;
  font-size: 20rpx;
}

.locate-button.loading {
  opacity: 0.6;
}

.locate-dot {
  width: 13rpx;
  height: 13rpx;
  margin-right: 9rpx;
  border: 5rpx solid #a66d35;
  border-radius: 50% 50% 50% 0;
  transform: rotate(-45deg);
}

.search-box {
  display: flex;
  align-items: center;
  margin-top: 34rpx;
  padding: 0 23rpx;
  border-radius: 27rpx;
  background: #fff;
  box-shadow: 0 12rpx 38rpx rgba(44, 38, 29, 0.05);
}

.search-icon {
  width: 20rpx;
  height: 20rpx;
  margin-right: 15rpx;
  border: 4rpx solid #938b80;
  border-radius: 50%;
}

.search-icon::after {
  display: block;
  width: 10rpx;
  height: 4rpx;
  margin: 17rpx 0 0 16rpx;
  background: #938b80;
  content: "";
  transform: rotate(45deg);
}

.search-box input {
  height: 86rpx;
  flex: 1;
  font-size: 25rpx;
}

.clear {
  padding: 10rpx;
  color: #a59e94;
  font-size: 33rpx;
}

.list-heading {
  margin: 40rpx 4rpx 20rpx;
  align-items: flex-end;
}

.list-title,
.list-caption {
  display: block;
}

.list-title {
  color: #27241f;
  font-size: 31rpx;
  font-weight: 800;
}

.list-caption {
  margin-top: 7rpx;
  color: #9d968c;
  font-size: 19rpx;
}

.shop-count {
  color: #8b8277;
  font-size: 20rpx;
}

.shop-list {
  display: flex;
  flex-direction: column;
  gap: 18rpx;
}

.shop-item {
  display: flex;
  position: relative;
  align-items: flex-start;
  padding: 22rpx;
  border: 2rpx solid transparent;
  border-radius: 30rpx;
  background: #fff;
  box-shadow: 0 12rpx 36rpx rgba(44, 38, 29, 0.05);
}

.shop-item.selected {
  border-color: #c99c64;
}

.shop-cover {
  display: flex;
  width: 128rpx;
  height: 128rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 22rpx;
  background: linear-gradient(145deg, #e8c99f, #c99558);
  background-position: center;
  background-size: cover;
  color: #fff;
  font-size: 27rpx;
  font-weight: 900;
}

.shop-main {
  min-width: 0;
  flex: 1;
  margin-left: 20rpx;
  padding-right: 29rpx;
}

.shop-title-row {
  justify-content: flex-start;
}

.shop-name {
  overflow: hidden;
  max-width: 270rpx;
  color: #28251f;
  font-size: 27rpx;
  font-weight: 800;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.status {
  flex-shrink: 0;
  margin-left: 10rpx;
  padding: 5rpx 10rpx;
  border-radius: 17rpx;
  font-size: 16rpx;
}

.status.open {
  background: #e5f1e8;
  color: #398054;
}

.status.paused {
  background: #f6ead7;
  color: #aa6f2b;
}

.status.resting {
  background: #f0ece7;
  color: #8e8479;
}

.shop-address {
  display: block;
  overflow: hidden;
  margin-top: 12rpx;
  color: #8f887e;
  font-size: 20rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-meta {
  margin-top: 13rpx;
  color: #6e665c;
  font-size: 19rpx;
}

.preorder-tip {
  display: inline-block;
  margin-top: 13rpx;
  padding: 6rpx 10rpx;
  border-radius: 8rpx;
  background: #f5eee3;
  color: #9b7041;
  font-size: 17rpx;
}

.choose-mark {
  position: absolute;
  right: 18rpx;
  top: 20rpx;
  color: #ad8355;
  font-size: 28rpx;
}

.state-card {
  display: flex;
  min-height: 360rpx;
  align-items: center;
  justify-content: center;
  flex-direction: column;
  border-radius: 30rpx;
  background: #fff;
  color: #938c82;
  font-size: 22rpx;
}

.loading-ring {
  width: 48rpx;
  height: 48rpx;
  margin-bottom: 22rpx;
  border: 5rpx solid #eee5d8;
  border-top-color: #a8753e;
  border-radius: 50%;
  animation: rotate 0.8s linear infinite;
}

.empty-mark {
  width: 88rpx;
  height: 88rpx;
  border-radius: 29rpx;
  background: #1c1b17;
  color: #e8ba7c;
  font-size: 23rpx;
  font-weight: 850;
  line-height: 88rpx;
  text-align: center;
}

.state-title {
  margin-top: 25rpx;
  color: #2e2a24;
  font-size: 27rpx;
  font-weight: 750;
}

.state-copy {
  margin-top: 10rpx;
  font-size: 20rpx;
}

@keyframes rotate {
  to { transform: rotate(360deg); }
}
</style>
