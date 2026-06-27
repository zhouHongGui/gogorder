<script setup lang="ts">
import { computed, onBeforeUnmount, ref } from 'vue'
import { onLoad } from '@dcloudio/uni-app'
import { loginByPassword, loginBySms, sendSms } from '../../api/auth'
import { getToken, saveSession } from '../../utils/session'

const mode = ref<'sms' | 'password'>('sms')
const phone = ref('')
const code = ref('')
const account = ref('')
const password = ref('')
const sending = ref(false)
const submitting = ref(false)
const countdown = ref(0)
let countdownTimer: ReturnType<typeof setInterval> | null = null

const phoneValid = computed(() => /^1[3-9]\d{9}$/.test(phone.value))
const canSubmit = computed(() => mode.value === 'sms'
  ? phoneValid.value && /^\d{6}$/.test(code.value)
  : account.value.trim().length >= 2 && password.value.length >= 5)

onLoad(() => {
  if (getToken()) uni.reLaunch({ url: '/pages/workbench/index' })
})

onBeforeUnmount(() => {
  if (countdownTimer) clearInterval(countdownTimer)
})

function changeMode(nextMode: 'sms' | 'password') {
  mode.value = nextMode
}

async function handleSendSms() {
  if (!phoneValid.value || sending.value || countdown.value > 0) return
  sending.value = true
  try {
    const result = await sendSms(phone.value)
    countdown.value = result.retryAfter || 60
    if (result.mockCode) {
      code.value = result.mockCode
      uni.showToast({ title: `开发验证码 ${result.mockCode}`, icon: 'none', duration: 2500 })
    } else {
      uni.showToast({ title: '验证码已发送', icon: 'success' })
    }
    countdownTimer = setInterval(() => {
      countdown.value -= 1
      if (countdown.value <= 0 && countdownTimer) {
        clearInterval(countdownTimer)
        countdownTimer = null
      }
    }, 1000)
  } catch {
    // request 已统一展示错误提示。
  } finally {
    sending.value = false
  }
}

async function handleLogin() {
  if (!canSubmit.value || submitting.value) return
  submitting.value = true
  try {
    const result = mode.value === 'sms'
      ? await loginBySms(phone.value, code.value)
      : await loginByPassword(account.value.trim(), password.value)
    saveSession(result)
    uni.reLaunch({ url: '/pages/workbench/index' })
  } catch {
    // request 已统一展示错误提示。
  } finally {
    submitting.value = false
  }
}
</script>

<template>
  <view class="safe-page login-page">
    <view class="bg-orb orb-top" />
    <view class="bg-orb orb-bottom" />

    <view class="brand-block">
      <view class="brand-row">
        <view class="brand-mark">GO</view>
        <view>
          <text class="brand-title">门店助手</text>
          <text class="brand-subtitle">订单、制作与门店运营</text>
        </view>
      </view>
      <view class="brand-slogan">
        <text>快速处理门店订单</text>
        <text>让每一杯都有节奏</text>
      </view>
    </view>

    <view class="login-card">
      <view class="card-heading">
        <text class="card-title">员工登录</text>
        <text class="card-desc">请选择登录方式进入门店工作台</text>
      </view>

      <view class="mode-tabs">
        <view class="mode-tab" :class="{ active: mode === 'sms' }" @click="changeMode('sms')">短信登录</view>
        <view class="mode-tab" :class="{ active: mode === 'password' }" @click="changeMode('password')">账号登录</view>
      </view>

      <view v-if="mode === 'sms'" class="form-body">
        <view class="field">
          <text class="field-label">手机号</text>
          <view class="input-shell">
            <text class="field-icon">☎</text>
            <input v-model="phone" class="field-input" type="number" maxlength="11" placeholder="请输入员工登录手机号" />
          </view>
        </view>
        <view class="field">
          <text class="field-label">验证码</text>
          <view class="input-shell code-row">
            <text class="field-icon">#</text>
            <input v-model="code" class="field-input code-input" type="number" maxlength="6" placeholder="6位验证码" />
            <button class="code-button" :disabled="!phoneValid || sending || countdown > 0" @click="handleSendSms">
              {{ countdown > 0 ? `${countdown}s` : '获取验证码' }}
            </button>
          </view>
        </view>
      </view>

      <view v-else class="form-body">
        <view class="field">
          <text class="field-label">员工账号</text>
          <view class="input-shell">
            <text class="field-icon">@</text>
            <input v-model="account" class="field-input" maxlength="30" placeholder="请输入门店员工账号" />
          </view>
        </view>
        <view class="field">
          <text class="field-label">密码</text>
          <view class="input-shell">
            <text class="field-icon">●</text>
            <input v-model="password" class="field-input" password maxlength="20" placeholder="请输入密码" />
          </view>
        </view>
      </view>

      <button class="primary-button login-button" :disabled="!canSubmit || submitting" @click="handleLogin">
        {{ submitting ? '登录中...' : '登录' }}
      </button>
      <text class="login-tip">账号由管理员创建，不支持员工自行注册</text>
    </view>
  </view>
</template>

<style scoped lang="scss">
.login-page {
  position: relative;
  overflow: hidden;
  padding: calc(env(safe-area-inset-top) + 52rpx) 32rpx 48rpx;
  background:
    radial-gradient(circle at 84% 6%, rgba(231, 182, 117, .48), transparent 28%),
    radial-gradient(circle at 7% 36%, rgba(206, 105, 45, .12), transparent 28%),
    linear-gradient(180deg, #fffaf2 0%, #f6efe4 48%, #eee6da 100%);
}

.bg-orb {
  position: absolute;
  border-radius: 50%;
  pointer-events: none;
}

.orb-top {
  top: -120rpx;
  right: -96rpx;
  width: 330rpx;
  height: 330rpx;
  background: rgba(231, 182, 117, .24);
}

.orb-bottom {
  bottom: 160rpx;
  left: -130rpx;
  width: 300rpx;
  height: 300rpx;
  background: rgba(206, 105, 45, .09);
}

.brand-block {
  position: relative;
  z-index: 1;
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  margin-bottom: 34rpx;
}

.brand-row {
  display: flex;
  align-items: center;
}

.brand-mark {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 96rpx;
  height: 96rpx;
  margin-right: 22rpx;
  border: 6rpx solid rgba(255, 255, 255, .88);
  border-radius: 34rpx;
  background: linear-gradient(145deg, #181711, #3d2b1c);
  box-shadow: 0 20rpx 44rpx rgba(42, 34, 24, .18);
  color: #e8ba7c;
  font-size: 34rpx;
  font-weight: 860;
  transform: rotate(-5deg);
}

.brand-title,
.brand-subtitle,
.brand-slogan text {
  display: block;
}

.brand-title {
  color: #181711;
  font-size: 46rpx;
  font-weight: 860;
  letter-spacing: 1rpx;
  line-height: 1.12;
}

.brand-subtitle {
  margin-top: 10rpx;
  color: #817b70;
  font-size: 24rpx;
}

.brand-slogan {
  margin-top: 34rpx;
  padding: 26rpx 28rpx;
  border: 1rpx solid rgba(255, 255, 255, .72);
  border-radius: 30rpx;
  background: rgba(255, 255, 255, .56);
  box-shadow: 0 16rpx 44rpx rgba(84, 61, 31, .08);
  color: #8b643d;
  font-size: 24rpx;
  font-weight: 750;
  line-height: 1.7;
  backdrop-filter: blur(16rpx);
}

.login-card {
  position: relative;
  z-index: 1;
  padding: 34rpx 30rpx 34rpx;
  border: 1rpx solid rgba(255, 255, 255, .78);
  border-radius: 40rpx;
  background: rgba(255, 255, 255, .9);
  box-shadow: 0 28rpx 76rpx rgba(72, 55, 34, .13);
  backdrop-filter: blur(22rpx);
}

.card-heading {
  margin-bottom: 26rpx;
}

.card-title,
.card-desc {
  display: block;
}

.card-title {
  color: #181711;
  font-size: 34rpx;
  font-weight: 860;
}

.card-desc {
  margin-top: 8rpx;
  color: #9b9184;
  font-size: 22rpx;
}

.mode-tabs {
  display: flex;
  padding: 8rpx;
  border-radius: 28rpx;
  background: #f0e7da;
  box-shadow: inset 0 0 0 1rpx rgba(218, 205, 188, .78);
}

.mode-tab {
  flex: 1;
  height: 72rpx;
  border-radius: 22rpx;
  color: #8f887c;
  font-size: 26rpx;
  font-weight: 760;
  line-height: 72rpx;
  text-align: center;
  transition: all .18s ease;
}

.mode-tab.active {
  background: linear-gradient(135deg, #181711, #44311d);
  box-shadow: 0 12rpx 26rpx rgba(42, 34, 24, .16);
  color: #fff6e8;
}

.form-body {
  margin-top: 34rpx;
}

.field {
  margin-bottom: 24rpx;
}

.field-label {
  display: block;
  margin-bottom: 12rpx;
  color: #7d7469;
  font-size: 23rpx;
  font-weight: 760;
}

.input-shell {
  display: flex;
  box-sizing: border-box;
  width: 100%;
  height: 96rpx;
  align-items: center;
  border: 2rpx solid #efe4d5;
  border-radius: 28rpx;
  background: #fffaf3;
  box-shadow: inset 0 0 0 1rpx rgba(255, 255, 255, .68);
}

.field-icon {
  display: flex;
  width: 72rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  color: #c88a45;
  font-size: 26rpx;
  font-weight: 850;
}

.field-input {
  box-sizing: border-box;
  width: 100%;
  height: 92rpx;
  min-width: 0;
  padding: 0 24rpx 0 0;
  border: 0;
  background: transparent;
  color: #292620;
  font-size: 29rpx;
}

.code-row {
  display: flex;
  padding-right: 10rpx;
  gap: 12rpx;
}

.code-input {
  flex: 1;
  width: auto;
  padding-right: 0;
}

.code-button {
  display: flex;
  width: 188rpx;
  height: 70rpx;
  flex-shrink: 0;
  align-items: center;
  justify-content: center;
  margin: 0;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: #f1dfc7;
  box-shadow: inset 0 0 0 1rpx rgba(211, 151, 84, .16);
  color: #9d622f;
  font-size: 23rpx;
  font-weight: 800;
  line-height: 70rpx;
}

.code-button[disabled] {
  background: #eee8de;
  box-shadow: none;
  color: #aea59a;
  opacity: 1;
}

.login-button {
  display: flex;
  height: 94rpx;
  align-items: center;
  justify-content: center;
  margin-top: 18rpx;
  padding: 0;
  border: 0;
  border-radius: 999rpx;
  background: linear-gradient(135deg, #181711, #49341f);
  box-shadow: 0 18rpx 42rpx rgba(49, 34, 20, .22);
  color: #fff7ea;
  font-size: 30rpx;
  font-weight: 850;
  letter-spacing: 2rpx;
  line-height: 94rpx;
}

.login-button[disabled] {
  background: linear-gradient(135deg, #d1c9bd, #bdb4a7);
  box-shadow: none;
  color: #fffaf3;
  opacity: 1;
}

.login-tip {
  display: block;
  margin-top: 24rpx;
  color: #a8a196;
  font-size: 22rpx;
  line-height: 1.6;
  text-align: center;
}
</style>
