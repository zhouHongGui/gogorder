<template>
  <view class="safe-page login-page">
    <view class="brand">
      <view class="brand-mark">GO</view>
      <text class="eyebrow">GOGORDER</text>
      <text class="title">今天，喝点喜欢的</text>
      <text class="subtitle">登录后选择附近门店，开始点单</text>
    </view>

    <!-- #ifdef H5 -->
    <view class="login-card">
      <text class="card-title">手机号登录</text>
      <view class="field">
        <text class="field-label">手机号</text>
        <input v-model="phone" type="number" maxlength="11" placeholder="请输入手机号" />
      </view>
      <view class="field">
        <text class="field-label">验证码</text>
        <view class="code-row">
          <input v-model="code" type="number" maxlength="6" placeholder="6位验证码" />
          <button class="code-button" :disabled="countdown > 0 || sending" @click="handleSendSms">
            {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
          </button>
        </view>
      </view>
      <view v-if="mockCode" class="mock-tip">开发验证码：{{ mockCode }}</view>
      <button class="primary-button" :disabled="loggingIn" @click="handleSmsLogin">
        {{ loggingIn ? '登录中...' : '登录并开始点单' }}
      </button>
    </view>
    <!-- #endif -->

    <!-- #ifdef MP-WEIXIN -->
    <view class="login-card wechat-card">
      <text class="card-title">微信小程序登录</text>
      <text class="wechat-copy">微信登录后授权手机号，即可同步你的点单账号。</text>
      <button v-if="!bindTicket" class="primary-button" :disabled="loggingIn" @click="handleWechatLogin">
        微信登录
      </button>
      <button v-else class="primary-button" open-type="getPhoneNumber" @getphonenumber="handlePhoneNumber">
        授权手机号并登录
      </button>
    </view>
    <!-- #endif -->

    <text class="agreement">登录即代表同意 gogorder 用户服务约定</text>
  </view>
</template>

<script setup lang="ts">
import { onUnmounted, ref } from 'vue'
import { bindWechatPhone, loginBySms, loginByWechat, sendSms } from '../../api/auth'
import { getToken, saveSession } from '../../utils/auth'
import type { LoginResult } from '../../types/auth'

const phone = ref('')
const code = ref('')
const mockCode = ref('')
const countdown = ref(0)
const sending = ref(false)
const loggingIn = ref(false)
const bindTicket = ref('')
let timer: ReturnType<typeof setInterval> | undefined

if (getToken()) {
  uni.reLaunch({ url: '/pages/index/index' })
}

async function handleSendSms() {
  if (!/^1\d{10}$/.test(phone.value)) {
    uni.showToast({ title: '请输入正确手机号', icon: 'none' })
    return
  }
  sending.value = true
  try {
    const result = await sendSms(phone.value)
    mockCode.value = result.mockCode || ''
    countdown.value = result.retryAfter || 60
    timer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0) clearInterval(timer)
    }, 1000)
    uni.showToast({ title: '验证码已发送', icon: 'success' })
  } finally {
    sending.value = false
  }
}

async function handleSmsLogin() {
  if (!/^1\d{10}$/.test(phone.value) || !/^\d{6}$/.test(code.value)) {
    uni.showToast({ title: '请填写手机号和验证码', icon: 'none' })
    return
  }
  loggingIn.value = true
  try {
    finishLogin(await loginBySms(phone.value, code.value))
  } finally {
    loggingIn.value = false
  }
}

function handleWechatLogin() {
  loggingIn.value = true
  uni.login({
    provider: 'weixin',
    success: async result => {
      try {
        const data = await loginByWechat(result.code)
          if (data.bound && data.token && data.userInfo) {
            finishLogin({ token: data.token, userInfo: data.userInfo })
          } else {
            bindTicket.value = data.bindTicket || ''
          }
      } finally {
        loggingIn.value = false
      }
    },
    fail: () => {
      loggingIn.value = false
      uni.showToast({ title: '微信登录失败', icon: 'none' })
    }
  })
}

async function handlePhoneNumber(event: { detail: { code?: string } }) {
  const phoneCode = event.detail.code
  if (!phoneCode) {
    uni.showToast({ title: '需要授权手机号才能登录', icon: 'none' })
    return
  }
  finishLogin(await bindWechatPhone(bindTicket.value, phoneCode))
}

function finishLogin(data: LoginResult) {
  saveSession(data)
  uni.reLaunch({ url: '/pages/index/index' })
}

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style lang="scss" scoped>
.login-page {
  padding: calc(var(--status-bar-height) + 72rpx) 36rpx 48rpx;
  background:
    radial-gradient(circle at 88% 8%, rgba(230, 178, 105, 0.38), transparent 28%),
    linear-gradient(180deg, #fbf7ee 0%, #f3eee4 100%);
}

.brand { display: flex; flex-direction: column; margin-bottom: 66rpx; }
.brand-mark {
  width: 108rpx; height: 108rpx; border-radius: 34rpx; background: #181711; color: #fff;
  display: flex; align-items: center; justify-content: center; font-size: 38rpx; font-weight: 800;
  transform: rotate(-5deg); margin-bottom: 38rpx;
}
.eyebrow { font-size: 22rpx; letter-spacing: 8rpx; color: #9c7040; font-weight: 700; }
.title { margin-top: 16rpx; font-size: 56rpx; line-height: 1.18; font-weight: 800; }
.subtitle { margin-top: 18rpx; font-size: 26rpx; color: #817b70; }
.login-card {
  padding: 42rpx 34rpx; border-radius: 36rpx; background: rgba(255,255,255,.82);
  box-shadow: 0 24rpx 70rpx rgba(77, 63, 42, .10); backdrop-filter: blur(20rpx);
}
.card-title { display: block; font-size: 34rpx; font-weight: 700; margin-bottom: 30rpx; }
.field { padding: 20rpx 0; border-bottom: 1rpx solid #e8e1d6; }
.field-label { display: block; color: #8f887c; font-size: 22rpx; margin-bottom: 12rpx; }
.field input { height: 56rpx; font-size: 30rpx; }
.code-row { display: flex; align-items: center; }
.code-row input { flex: 1; }
.code-button { margin: 0; padding: 0 18rpx; background: transparent; color: #9c7040; font-size: 24rpx; }
.mock-tip { margin: 20rpx 0 0; padding: 16rpx 20rpx; border-radius: 16rpx; background: #fff4df; color: #9c6530; font-size: 24rpx; }
.primary-button { margin-top: 38rpx; }
.wechat-copy { display: block; color: #817b70; font-size: 26rpx; line-height: 1.7; }
.agreement { display: block; text-align: center; color: #a8a196; font-size: 22rpx; margin-top: 36rpx; }
</style>
