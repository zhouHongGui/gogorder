<script setup lang="ts">
import { computed, ref } from 'vue'
import { onPullDownRefresh, onShow } from '@dcloudio/uni-app'
import { logout } from '../../api/auth'
import { getPendingCount } from '../../api/order'
import { getMyShops } from '../../api/shop'
import type { ShopContext } from '../../types/auth'
import { clearSession, getCurrentShop, getShops, getUser, saveShopContext } from '../../utils/session'

const loading = ref(false)
const currentShop = ref<ShopContext | null>(getCurrentShop())
const shops = ref<ShopContext[]>(getShops())
const user = ref(getUser())
const orderCounts = ref({
  preorders: 0,
  pending: 0,
  making: 0,
  waiting: 0,
  totalActive: 0
})

const statusText = computed(() => {
  if (currentShop.value?.status === 1) return '营业中'
  if (currentShop.value?.status === 2) return '暂停即时单'
  return '休息中'
})
const statusClass = computed(() => `status-${currentShop.value?.status ?? 0}`)
const canSwitch = computed(() => shops.value.length > 1)
const displayName = computed(() => user.value?.nickname || user.value?.account || '门店伙伴')
const shopInitial = computed(() => (currentShop.value?.shopName || 'GO').slice(0, 1))
const statusHint = computed(() => {
  if (currentShop.value?.status === 1) return '可接收即时订单'
  if (currentShop.value?.status === 2) return '仅处理预约与存量订单'
  return '门店休息中'
})

onShow(() => {
  if (!user.value) {
    uni.reLaunch({ url: '/pages/login/index' })
    return
  }
  void loadContext()
})

onPullDownRefresh(async () => {
  try {
    await loadContext()
  } finally {
    uni.stopPullDownRefresh()
  }
})

async function loadContext() {
  if (loading.value) return
  loading.value = true
  try {
    const result = await getMyShops()
    shops.value = result.shops
    currentShop.value = result.currentShop
    saveShopContext(result.shops, result.currentShop)
    await loadOrderCounts()
  } catch {
    // request 已统一处理 401 跳转和错误提示。
  } finally {
    loading.value = false
  }
}

async function loadOrderCounts() {
  try {
    orderCounts.value = await getPendingCount()
  } catch {
    // 首页统计失败不阻塞门店上下文。
  }
}

function showShopSwitcher() {
  if (!canSwitch.value) {
    uni.showToast({ title: '当前仅授权一家门店', icon: 'none' })
    return
  }
  uni.navigateTo({ url: '/pages/shop/select' })
}

async function handleLogout() {
  try {
    await logout()
  } catch {
    // 本地会话仍需清理，服务端 Token 最迟按缓存有效期失效。
  } finally {
    clearSession()
    uni.reLaunch({ url: '/pages/login/index' })
  }
}

function openBoard() {
  uni.navigateTo({ url: '/pages/order/board' })
}

function openVerify() {
  uni.navigateTo({ url: '/pages/order/verify' })
}

function showPending(feature: string) {
  uni.showToast({ title: `${feature}将在后续接入`, icon: 'none' })
}
</script>

<template>
  <view class="safe-page workbench-page">
    <view class="hero-shell">
      <view class="nav-row">
        <view>
          <text class="eyebrow">GOGORDER STAFF</text>
          <text class="hello-title">你好，{{ displayName }}</text>
        </view>
        <view class="avatar-badge">GO</view>
      </view>

      <view class="shop-card">
        <view class="shop-main">
          <view class="shop-mark">{{ shopInitial }}</view>
          <view class="shop-copy">
            <view class="shop-title-row" @click="showShopSwitcher">
              <text class="shop-title">{{ currentShop?.shopName || '加载中' }}</text>
              <text v-if="canSwitch" class="switch-chip">切换</text>
            </view>
            <text class="shop-address">{{ currentShop?.address || '正在获取门店信息' }}</text>
          </view>
        </view>
        <view class="shop-footer">
          <view class="status-block">
            <text class="status-dot" :class="statusClass" />
            <text class="status-text">{{ statusText }}</text>
            <text class="status-hint">{{ statusHint }}</text>
          </view>
          <text class="auth-chip">已授权</text>
        </view>
      </view>
    </view>

    <view class="content-wrap">
      <view class="section-heading">
        <view>
          <text class="section-kicker">TODAY</text>
          <text class="section-title">今日任务</text>
        </view>
        <text class="refresh-text" @click="loadContext">{{ loading ? '刷新中' : '刷新' }}</text>
      </view>

      <view class="task-panel">
        <view class="task-main" @click="openBoard">
          <text class="task-label">待制作</text>
          <view class="task-value-row">
            <text class="task-value">{{ orderCounts.pending }}</text>
            <text class="task-unit">单</text>
          </view>
          <text class="task-tip">优先处理新订单</text>
        </view>
        <view class="task-side">
          <view class="mini-stat">
            <text class="mini-value">{{ orderCounts.making }}</text>
            <text class="mini-label">制作中</text>
          </view>
          <view class="mini-stat warm" @click="openVerify">
            <text class="mini-value">{{ orderCounts.waiting }}</text>
            <text class="mini-label">待取餐</text>
          </view>
        </view>
      </view>

      <view class="preorder-strip" @click="openBoard">
        <view>
          <text class="preorder-title">预约单</text>
          <text class="preorder-desc">到制作时间会进入待制作队列</text>
        </view>
        <text class="preorder-count">{{ orderCounts.preorders }}</text>
      </view>

      <view class="section-heading operation-heading">
        <view>
          <text class="section-kicker">ACTIONS</text>
          <text class="section-title">常用操作</text>
        </view>
      </view>

      <view class="primary-actions">
        <view class="big-action board-action" @click="openBoard">
          <view class="action-icon board-icon">
            <view class="receipt-line long" />
            <view class="receipt-line" />
            <view class="receipt-line short" />
          </view>
          <text class="big-action-title">制作看板</text>
          <text class="big-action-desc">订单流转、批量开始、取消退款</text>
          <text class="action-arrow">进入 ›</text>
        </view>
        <view class="big-action verify-action" @click="openVerify">
          <view class="action-icon verify-icon">
            <view class="scan-corner corner-a" />
            <view class="scan-corner corner-b" />
            <view class="scan-corner corner-c" />
            <view class="scan-corner corner-d" />
          </view>
          <text class="big-action-title">扫码核销</text>
          <text class="big-action-desc">扫描小票二维码，完成取餐交付</text>
          <text class="action-arrow">去核销 ›</text>
        </view>
      </view>

      <view class="secondary-grid">
        <view class="small-action" @click="showPending('订单查询')">
          <text class="small-icon order-icon">#</text>
          <view>
            <text class="small-title">订单查询</text>
            <text class="small-desc">详情与退款记录</text>
          </view>
        </view>
        <view class="small-action" @click="showPending('商品沽清')">
          <text class="small-icon soldout-icon">!</text>
          <view>
            <text class="small-title">商品沽清</text>
            <text class="small-desc">门店商品状态</text>
          </view>
        </view>
      </view>

      <view class="account-card">
        <view class="account-main">
          <text class="account-name">{{ user?.account }}</text>
          <text class="account-phone">{{ user?.phone || '未绑定手机号' }}</text>
        </view>
        <text class="logout-link" @click="handleLogout">退出登录</text>
      </view>
    </view>
  </view>
</template>

<style scoped lang="scss">
.workbench-page {
  min-height: 100vh;
  padding-bottom: calc(env(safe-area-inset-bottom) + 44rpx);
  background:
    radial-gradient(circle at 90% 4%, rgba(231, 182, 117, .34), transparent 28%),
    radial-gradient(circle at 6% 36%, rgba(206, 105, 45, .08), transparent 30%),
    linear-gradient(180deg, #fbf7ee 0%, #f5f1e9 46%, #eee7dc 100%);
}

.hero-shell {
  position: relative;
  overflow: hidden;
  padding: calc(env(safe-area-inset-top) + 28rpx) 28rpx 34rpx;
  background:
    radial-gradient(circle at 84% 12%, rgba(255, 237, 205, .72), transparent 26%),
    linear-gradient(180deg, rgba(255, 250, 242, .88), rgba(255, 250, 242, .18));
}

.hero-shell::after {
  content: "";
  position: absolute;
  top: -80rpx;
  right: -70rpx;
  width: 260rpx;
  height: 260rpx;
  border-radius: 50%;
  background: rgba(231, 182, 117, .18);
}

.nav-row {
  position: relative;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.eyebrow {
  display: block;
  color: #b17d42;
  font-size: 20rpx;
  font-weight: 850;
  letter-spacing: 5rpx;
}

.hello-title {
  display: block;
  margin-top: 8rpx;
  color: #181711;
  font-size: 40rpx;
  font-weight: 860;
  line-height: 1.18;
}

.avatar-badge {
  display: flex;
  width: 78rpx;
  height: 78rpx;
  align-items: center;
  justify-content: center;
  border: 6rpx solid #fff;
  border-radius: 28rpx;
  background: #181711;
  box-shadow: 0 16rpx 36rpx rgba(40, 33, 24, .14);
  color: #e8ba7c;
  font-size: 25rpx;
  font-weight: 860;
  transform: rotate(-5deg);
}

.shop-card {
  position: relative;
  z-index: 1;
  margin-top: 30rpx;
  padding: 28rpx;
  border: 1rpx solid rgba(255, 255, 255, .8);
  border-radius: 34rpx;
  background: rgba(255, 255, 255, .82);
  box-shadow: 0 20rpx 58rpx rgba(70, 56, 38, .1);
  backdrop-filter: blur(18rpx);
}

.shop-main {
  display: flex;
  align-items: center;
  gap: 22rpx;
}

.shop-mark {
  display: flex;
  width: 88rpx;
  height: 88rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 30rpx;
  background: linear-gradient(145deg, #ce692d, #e7b675);
  box-shadow: 0 14rpx 30rpx rgba(190, 105, 43, .18);
  color: #fff;
  font-size: 34rpx;
  font-weight: 880;
}

.shop-copy {
  min-width: 0;
  flex: 1;
}

.shop-title-row {
  display: flex;
  min-width: 0;
  align-items: center;
}

.shop-title {
  min-width: 0;
  max-width: 430rpx;
  overflow: hidden;
  color: #181711;
  font-size: 34rpx;
  font-weight: 850;
  line-height: 1.2;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.switch-chip {
  flex-shrink: 0;
  margin-left: 14rpx;
  padding: 7rpx 14rpx;
  border-radius: 18rpx;
  background: #f1e4d2;
  color: #9c6534;
  font-size: 21rpx;
  font-weight: 760;
}

.shop-address {
  display: block;
  max-width: 510rpx;
  margin-top: 10rpx;
  overflow: hidden;
  color: #92897d;
  font-size: 22rpx;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.shop-footer {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
  padding-top: 22rpx;
  border-top: 1rpx solid #f0e6d9;
}

.status-block {
  display: flex;
  min-width: 0;
  align-items: center;
}

.status-dot {
  width: 14rpx;
  height: 14rpx;
  flex-shrink: 0;
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
  margin-left: 14rpx;
  color: #292620;
  font-size: 25rpx;
  font-weight: 780;
}

.status-hint {
  margin-left: 14rpx;
  padding-left: 14rpx;
  border-left: 1rpx solid #e7dccd;
  color: #958b7d;
  font-size: 21rpx;
}

.auth-chip {
  flex-shrink: 0;
  padding: 10rpx 16rpx;
  border-radius: 22rpx;
  background: #181711;
  color: #f1dfc3;
  font-size: 22rpx;
  font-weight: 740;
}

.content-wrap {
  padding: 6rpx 28rpx 0;
}

.section-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 24rpx;
}

.section-kicker {
  display: block;
  color: #b17d42;
  font-size: 18rpx;
  font-weight: 850;
  letter-spacing: 4rpx;
}

.section-title {
  display: block;
  margin-top: 4rpx;
  color: #292620;
  font-size: 33rpx;
  font-weight: 860;
}

.refresh-text {
  padding: 12rpx 20rpx;
  border-radius: 24rpx;
  background: rgba(255, 255, 255, .82);
  color: #8f6233;
  font-size: 23rpx;
  font-weight: 720;
  box-shadow: 0 10rpx 28rpx rgba(77, 63, 42, .06);
}

.task-panel {
  display: flex;
  gap: 18rpx;
  margin-top: 20rpx;
}

.task-main {
  position: relative;
  overflow: hidden;
  display: flex;
  min-height: 188rpx;
  flex: 1.25;
  flex-direction: column;
  justify-content: space-between;
  padding: 30rpx;
  border-radius: 34rpx;
  background: linear-gradient(145deg, #181711 0%, #32281f 62%, #684221 100%);
  box-shadow: 0 22rpx 54rpx rgba(60, 43, 29, .18);
  color: #fff;
}

.task-main::after {
  content: "";
  position: absolute;
  right: -36rpx;
  bottom: -50rpx;
  width: 190rpx;
  height: 190rpx;
  border: 24rpx solid rgba(231, 182, 117, .16);
  border-radius: 50%;
}

.task-label,
.task-tip,
.task-value-row {
  position: relative;
  z-index: 1;
}

.task-label {
  color: #e8c08a;
  font-size: 24rpx;
  font-weight: 760;
}

.task-value-row {
  display: flex;
  align-items: baseline;
}

.task-value {
  font-size: 72rpx;
  font-weight: 900;
  line-height: 1;
}

.task-unit {
  margin-left: 8rpx;
  color: rgba(255, 255, 255, .66);
  font-size: 25rpx;
  font-weight: 720;
}

.task-tip {
  color: rgba(255, 255, 255, .68);
  font-size: 22rpx;
}

.task-side {
  display: flex;
  flex: .95;
  flex-direction: column;
  gap: 18rpx;
}

.mini-stat {
  min-height: 85rpx;
  padding: 22rpx 24rpx;
  border: 1rpx solid #fff;
  border-radius: 28rpx;
  background: rgba(255, 255, 255, .88);
  box-shadow: 0 14rpx 36rpx rgba(67, 57, 42, .06);
}

.mini-stat.warm {
  background: #fff4df;
}

.mini-value {
  display: block;
  color: #292620;
  font-size: 38rpx;
  font-weight: 860;
  line-height: 1;
}

.mini-label {
  display: block;
  margin-top: 12rpx;
  color: #8d8173;
  font-size: 22rpx;
}

.preorder-strip {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 18rpx;
  padding: 24rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, .86);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .72);
  box-shadow: 0 12rpx 32rpx rgba(67, 57, 42, .05);
}

.preorder-title {
  display: block;
  color: #292620;
  font-size: 27rpx;
  font-weight: 800;
}

.preorder-desc {
  display: block;
  margin-top: 8rpx;
  color: #968b7d;
  font-size: 22rpx;
}

.preorder-count {
  display: flex;
  width: 66rpx;
  height: 66rpx;
  align-items: center;
  justify-content: center;
  border-radius: 24rpx;
  background: #f1e4d2;
  color: #9c6534;
  font-size: 31rpx;
  font-weight: 860;
}

.operation-heading {
  margin-top: 34rpx;
}

.primary-actions {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 20rpx;
}

.big-action {
  position: relative;
  overflow: hidden;
  min-height: 258rpx;
  padding: 26rpx;
  border-radius: 34rpx;
  box-shadow: 0 18rpx 44rpx rgba(67, 57, 42, .08);
}

.board-action {
  background: #fff;
}

.verify-action {
  background: linear-gradient(145deg, #f7c16e 0%, #df873d 100%);
}

.big-action::after {
  content: "";
  position: absolute;
  right: -52rpx;
  bottom: -56rpx;
  width: 160rpx;
  height: 160rpx;
  border-radius: 50%;
  background: rgba(255, 255, 255, .3);
}

.action-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 74rpx;
  height: 74rpx;
  border-radius: 26rpx;
}

.board-icon {
  flex-direction: column;
  gap: 8rpx;
  background: #181711;
}

.receipt-line {
  width: 34rpx;
  height: 5rpx;
  border-radius: 5rpx;
  background: #e8ba7c;
}

.receipt-line.long {
  width: 42rpx;
}

.receipt-line.short {
  width: 24rpx;
}

.verify-icon {
  background: rgba(255, 255, 255, .92);
}

.scan-corner {
  position: absolute;
  width: 18rpx;
  height: 18rpx;
  border-color: #9c4f24;
  border-style: solid;
}

.corner-a {
  top: 18rpx;
  left: 18rpx;
  border-width: 4rpx 0 0 4rpx;
}

.corner-b {
  top: 18rpx;
  right: 18rpx;
  border-width: 4rpx 4rpx 0 0;
}

.corner-c {
  right: 18rpx;
  bottom: 18rpx;
  border-width: 0 4rpx 4rpx 0;
}

.corner-d {
  bottom: 18rpx;
  left: 18rpx;
  border-width: 0 0 4rpx 4rpx;
}

.big-action-title {
  position: relative;
  z-index: 1;
  display: block;
  margin-top: 28rpx;
  color: #1d1b17;
  font-size: 31rpx;
  font-weight: 860;
}

.verify-action .big-action-title {
  color: #fff;
}

.big-action-desc {
  position: relative;
  z-index: 1;
  display: block;
  min-height: 64rpx;
  margin-top: 12rpx;
  color: #91877a;
  font-size: 22rpx;
  line-height: 1.45;
}

.verify-action .big-action-desc {
  color: rgba(255, 255, 255, .82);
}

.action-arrow {
  position: relative;
  z-index: 1;
  display: inline-block;
  margin-top: 18rpx;
  padding: 10rpx 18rpx;
  border-radius: 20rpx;
  background: #f1e4d2;
  color: #9c6534;
  font-size: 22rpx;
  font-weight: 780;
}

.verify-action .action-arrow {
  background: rgba(24, 23, 17, .92);
  color: #f1dfc3;
}

.secondary-grid {
  display: grid;
  grid-template-columns: repeat(2, 1fr);
  gap: 18rpx;
  margin-top: 18rpx;
}

.small-action {
  display: flex;
  align-items: center;
  gap: 18rpx;
  min-height: 106rpx;
  padding: 20rpx;
  border: 1rpx solid rgba(255, 255, 255, .88);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .76);
  box-shadow: 0 12rpx 30rpx rgba(67, 57, 42, .05);
}

.small-icon {
  display: flex;
  width: 54rpx;
  height: 54rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  border-radius: 18rpx;
  font-size: 25rpx;
  font-weight: 860;
}

.order-icon {
  background: #e9f2ec;
  color: #39745a;
}

.soldout-icon {
  background: #f9e3d8;
  color: #b65629;
}

.small-title,
.small-desc {
  display: block;
}

.small-title {
  color: #292620;
  font-size: 25rpx;
  font-weight: 780;
}

.small-desc {
  margin-top: 6rpx;
  color: #9a9388;
  font-size: 20rpx;
}

.account-card {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 30rpx;
  padding: 26rpx 28rpx;
  border-radius: 30rpx;
  background: rgba(45, 40, 33, .06);
}

.account-name {
  display: block;
  color: #292620;
  font-size: 27rpx;
  font-weight: 720;
}

.account-phone {
  display: block;
  margin-top: 8rpx;
  color: #9a9388;
  font-size: 22rpx;
}

.logout-link {
  padding: 12rpx 20rpx;
  border-radius: 24rpx;
  background: #fff;
  color: #9b5650;
  font-size: 24rpx;
  font-weight: 720;
}
</style>
