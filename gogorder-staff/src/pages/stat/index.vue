<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { getStatOverview } from '../../api/stat'
import type { StaffStatOverview, StaffStatRange, StaffStatStatusCount } from '../../types/stat'
import { getCurrentShop, getUser } from '../../utils/session'

const ranges: Array<{ label: string; value: StaffStatRange }> = [
  { label: '今日', value: 'TODAY' },
  { label: '昨日', value: 'YESTERDAY' },
  { label: '近7天', value: 'LAST_7_DAYS' }
]

const loading = ref(false)
const activeRange = ref<StaffStatRange>('TODAY')
const overview = ref<StaffStatOverview | null>(null)
const currentShop = ref(getCurrentShop())
const user = ref(getUser())

const statusCards = computed(() => {
  const rows = overview.value?.statusCounts || []
  const wanted = [1, 2, 3, 4]
  return wanted.map(status => {
    const item = rows.find(row => row.orderStatus === status)
    return {
      orderStatus: status,
      orderStatusDesc: item?.orderStatusDesc || statusName(status),
      count: item?.count || 0
    }
  })
})

const dateText = computed(() => {
  if (!overview.value) return ''
  if (overview.value.startDate === overview.value.endDate) return overview.value.startDate
  return `${overview.value.startDate} 至 ${overview.value.endDate}`
})

const rankMax = computed(() => {
  const ranks = overview.value?.productRanks || []
  return ranks.reduce((max, item) => Math.max(max, item.quantity || 0), 0) || 1
})

onShow(() => {
  if (!user.value) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  currentShop.value = getCurrentShop()
  void loadOverview()
})

onPullDownRefresh(async () => {
  try {
    await loadOverview()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadOverview() {
  if (loading.value) return
  loading.value = true
  try {
    overview.value = await getStatOverview(activeRange.value)
  } finally {
    loading.value = false
  }
}

function switchRange(range: StaffStatRange) {
  if (activeRange.value === range) return
  activeRange.value = range
  void loadOverview()
}

function goBack() {
  uni.navigateBack({
    fail: () => uni.reLaunch({ url: '/pages/workbench/index' })
  })
}

function formatMoney(value?: number) {
  return `¥${((value || 0) / 100).toFixed(2)}`
}

function rankPercent(quantity: number) {
  return `${Math.max(8, Math.round((quantity || 0) / rankMax.value * 100))}%`
}

function statusName(status: number) {
  const names: Record<number, string> = {
    1: '待制作',
    2: '制作中',
    3: '待取餐',
    4: '已完成'
  }
  return names[status] || '其他'
}

function statusClass(item: StaffStatStatusCount) {
  return `status-${item.orderStatus}`
}
</script>

<template>
  <view class="safe-page stat-page">
    <view class="top-shell">
      <view class="nav-row">
        <text class="back-btn" @click="goBack">‹</text>
        <view class="nav-title">
          <text class="eyebrow">SHOP DATA</text>
          <text class="title">营业数据</text>
        </view>
        <view class="refresh-btn" @click="loadOverview">{{ loading ? '...' : '刷' }}</view>
      </view>

      <view class="shop-line">
        <text class="shop-name">{{ currentShop?.shopName || '当前门店' }}</text>
        <text class="date-text">{{ dateText || '加载中' }}</text>
      </view>

      <view class="range-tabs">
        <view
          v-for="item in ranges"
          :key="item.value"
          class="range-tab"
          :class="{ active: activeRange === item.value }"
          @click="switchRange(item.value)"
        >
          {{ item.label }}
        </view>
      </view>

      <view class="turnover-card">
        <text class="card-label">{{ overview?.rangeDesc || '今日' }}营业额</text>
        <text class="turnover-value">{{ formatMoney(overview?.turnoverAmount) }}</text>
        <view class="metric-row">
          <view class="metric-item">
            <text class="metric-value">{{ overview?.orderCount || 0 }}</text>
            <text class="metric-label">订单</text>
          </view>
          <view class="metric-item">
            <text class="metric-value">{{ overview?.cupCount || 0 }}</text>
            <text class="metric-label">杯数</text>
          </view>
          <view class="metric-item">
            <text class="metric-value">{{ formatMoney(overview?.avgOrderAmount) }}</text>
            <text class="metric-label">客单价</text>
          </view>
        </view>
      </view>
    </view>

    <view class="content-wrap">
      <view class="section-heading">
        <view>
          <text class="section-kicker">STATUS</text>
          <text class="section-title">订单状态分布</text>
        </view>
      </view>

      <view class="status-grid">
        <view v-for="item in statusCards" :key="item.orderStatus" class="status-card" :class="statusClass(item)">
          <text class="status-count">{{ item.count }}</text>
          <text class="status-label">{{ item.orderStatusDesc }}</text>
        </view>
      </view>

      <view class="section-heading rank-heading">
        <view>
          <text class="section-kicker">RANKING</text>
          <text class="section-title">商品销量排行</text>
        </view>
        <text class="rank-tip">Top 10</text>
      </view>

      <view class="rank-card">
        <view v-if="!overview?.productRanks?.length" class="empty-rank">
          <text class="empty-title">暂无销量数据</text>
          <text class="empty-desc">当前时间范围内还没有已支付订单</text>
        </view>
        <view v-for="(item, index) in overview?.productRanks || []" :key="item.productId" class="rank-item">
          <view class="rank-index">{{ index + 1 }}</view>
          <view class="rank-main">
            <view class="rank-row">
              <text class="rank-name">{{ item.productName }}</text>
              <text class="rank-count">{{ item.quantity }}杯</text>
            </view>
            <view class="rank-bar-track">
              <view class="rank-bar" :style="{ width: rankPercent(item.quantity) }" />
            </view>
            <text class="rank-amount">{{ formatMoney(item.salesAmount) }}</text>
          </view>
        </view>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.stat-page {
  min-height: 100vh;
  padding-bottom: calc(env(safe-area-inset-bottom) + 44rpx);
  background:
    radial-gradient(circle at 88% 8%, rgba(231, 182, 117, .36), transparent 28%),
    linear-gradient(180deg, #fbf7ee 0%, #f4efe6 52%, #eee7dc 100%);
}

.top-shell {
  padding: calc(env(safe-area-inset-top) + 24rpx) 28rpx 32rpx;
}

.nav-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.back-btn,
.refresh-btn {
  display: flex;
  width: 70rpx;
  height: 70rpx;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .78);
  color: #292620;
  font-size: 40rpx;
  font-weight: 800;
  box-shadow: 0 10rpx 28rpx rgba(67, 57, 42, .06);
}

.refresh-btn {
  font-size: 24rpx;
}

.nav-title {
  text-align: center;
}

.eyebrow,
.section-kicker {
  display: block;
  color: #b17d42;
  font-size: 18rpx;
  font-weight: 850;
  letter-spacing: 4rpx;
}

.title {
  display: block;
  margin-top: 4rpx;
  color: #181711;
  font-size: 38rpx;
  font-weight: 880;
}

.shop-line {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 30rpx;
}

.shop-name {
  max-width: 440rpx;
  overflow: hidden;
  color: #292620;
  font-size: 28rpx;
  font-weight: 780;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.date-text {
  color: #8c8579;
  font-size: 22rpx;
}

.range-tabs {
  display: flex;
  gap: 12rpx;
  margin-top: 24rpx;
  padding: 10rpx;
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .62);
}

.range-tab {
  flex: 1;
  height: 66rpx;
  border-radius: 24rpx;
  color: #8c8579;
  font-size: 25rpx;
  font-weight: 760;
  line-height: 66rpx;
  text-align: center;
}

.range-tab.active {
  background: #181711;
  box-shadow: 0 12rpx 28rpx rgba(24, 23, 17, .14);
  color: #f1dfc3;
}

.turnover-card {
  position: relative;
  overflow: hidden;
  margin-top: 24rpx;
  padding: 34rpx;
  border-radius: 38rpx;
  background: linear-gradient(145deg, #181711 0%, #2d241c 62%, #704720 100%);
  box-shadow: 0 24rpx 58rpx rgba(60, 43, 29, .18);
  color: #fff;
}

.turnover-card::after {
  content: "";
  position: absolute;
  top: -70rpx;
  right: -50rpx;
  width: 230rpx;
  height: 230rpx;
  border: 32rpx solid rgba(231, 182, 117, .16);
  border-radius: 50%;
}

.card-label,
.turnover-value,
.metric-row {
  position: relative;
  z-index: 1;
}

.card-label {
  display: block;
  color: #e8c08a;
  font-size: 24rpx;
  font-weight: 760;
}

.turnover-value {
  display: block;
  margin-top: 18rpx;
  font-size: 62rpx;
  font-weight: 900;
  letter-spacing: -1rpx;
}

.metric-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 14rpx;
  margin-top: 28rpx;
}

.metric-item {
  padding: 18rpx 16rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .1);
}

.metric-value {
  display: block;
  color: #fff;
  font-size: 28rpx;
  font-weight: 860;
}

.metric-label {
  display: block;
  margin-top: 8rpx;
  color: rgba(255, 255, 255, .62);
  font-size: 21rpx;
}

.content-wrap {
  padding: 0 28rpx;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 28rpx;
}

.section-title {
  display: block;
  margin-top: 4rpx;
  color: #292620;
  font-size: 32rpx;
  font-weight: 860;
}

.status-grid {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 14rpx;
  margin-top: 18rpx;
}

.status-card {
  min-height: 116rpx;
  padding: 20rpx 14rpx;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 12rpx 30rpx rgba(67, 57, 42, .05);
  text-align: center;
}

.status-count {
  display: block;
  color: #292620;
  font-size: 38rpx;
  font-weight: 880;
  line-height: 1;
}

.status-label {
  display: block;
  margin-top: 14rpx;
  color: #8c8579;
  font-size: 21rpx;
}

.status-2 {
  background: #fff4df;
}

.status-3 {
  background: #edf5ee;
}

.status-4 {
  background: #f0eee8;
}

.rank-heading {
  margin-top: 34rpx;
}

.rank-tip {
  padding: 10rpx 18rpx;
  border-radius: 20rpx;
  background: rgba(255, 255, 255, .78);
  color: #9c6534;
  font-size: 22rpx;
  font-weight: 760;
}

.rank-card {
  margin-top: 18rpx;
  padding: 10rpx 0;
  border-radius: 34rpx;
  background: rgba(255, 255, 255, .78);
  box-shadow: 0 14rpx 38rpx rgba(67, 57, 42, .06);
}

.rank-item {
  display: flex;
  gap: 18rpx;
  padding: 24rpx 26rpx;
  border-bottom: 1rpx solid #f0e6d9;
}

.rank-item:last-child {
  border-bottom: none;
}

.rank-index {
  display: flex;
  width: 48rpx;
  height: 48rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  background: #181711;
  color: #f1dfc3;
  font-size: 23rpx;
  font-weight: 840;
}

.rank-main {
  min-width: 0;
  flex: 1;
}

.rank-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16rpx;
}

.rank-name {
  min-width: 0;
  overflow: hidden;
  color: #292620;
  font-size: 27rpx;
  font-weight: 780;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.rank-count {
  flex-shrink: 0;
  color: #ce692d;
  font-size: 24rpx;
  font-weight: 820;
}

.rank-bar-track {
  overflow: hidden;
  height: 12rpx;
  margin-top: 16rpx;
  border-radius: 12rpx;
  background: #f0e6d9;
}

.rank-bar {
  height: 12rpx;
  border-radius: 12rpx;
  background: linear-gradient(90deg, #e57d22, #d7ad75);
}

.rank-amount {
  display: block;
  margin-top: 12rpx;
  color: #8c8579;
  font-size: 22rpx;
}

.empty-rank {
  padding: 70rpx 30rpx;
  text-align: center;
}

.empty-title {
  display: block;
  color: #292620;
  font-size: 28rpx;
  font-weight: 780;
}

.empty-desc {
  display: block;
  margin-top: 12rpx;
  color: #8c8579;
  font-size: 23rpx;
}
</style>
