<template>
  <view class="safe-page order-page">
    <view class="order-hero">
      <text class="eyebrow">GOGORDER ORDERS</text>
      <text class="title">我的订单</text>
      <text class="description">下单功能开放后，你可以在这里查看订单状态和取餐信息。</text>
    </view>

    <view class="empty-card">
      <view class="empty-mark">GO</view>
      <text class="empty-title">还没有订单</text>
      <text class="empty-description">去首页选择门店，开启第一杯吧。</text>
      <button class="primary-button" @click="goHome">去首页</button>
    </view>
  </view>
</template>

<script setup lang="ts">
function goHome() {
  uni.switchTab({ url: '/pages/index/index' })
}
</script>

<style lang="scss" scoped>
.order-page {
  padding: 40rpx 32rpx 72rpx;
}

.order-hero {
  padding: 30rpx 8rpx 38rpx;
}

.eyebrow {
  display: block;
  color: #b17d42;
  font-size: 20rpx;
  font-weight: 700;
  letter-spacing: 4rpx;
}

.title {
  display: block;
  margin-top: 18rpx;
  color: #181711;
  font-size: 48rpx;
  font-weight: 800;
}

.description {
  display: block;
  margin-top: 16rpx;
  color: #8c8579;
  font-size: 25rpx;
  line-height: 1.7;
}

.empty-card {
  margin-top: 24rpx;
  padding: 78rpx 40rpx 52rpx;
  border-radius: 36rpx;
  background: #fff;
  text-align: center;
}

.empty-mark {
  width: 112rpx;
  height: 112rpx;
  margin: 0 auto;
  border-radius: 38rpx;
  background: #181711;
  color: #e8ba7c;
  font-size: 30rpx;
  font-weight: 800;
  line-height: 112rpx;
}

.empty-title {
  display: block;
  margin-top: 34rpx;
  color: #181711;
  font-size: 31rpx;
  font-weight: 700;
}

.empty-description {
  display: block;
  margin-top: 14rpx;
  color: #8c8579;
  font-size: 24rpx;
}

.primary-button {
  margin-top: 42rpx;
}
</style>
